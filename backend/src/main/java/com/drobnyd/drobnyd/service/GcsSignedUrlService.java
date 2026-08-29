package com.drobnyd.drobnyd.service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.drobnyd.drobnyd.config.properties.GcsProperties;
import com.google.auth.ServiceAccountSigner;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.SignBlobRequest;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;

@Service
public class GcsSignedUrlService {

    private static final Logger log = LoggerFactory.getLogger(GcsSignedUrlService.class);
    private static final String METADATA_EMAIL_ENDPOINT = "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/email";

    private final GcsProperties gcsProperties;
    private final Storage storage;
    private final ServiceAccountSigner signer;

    public GcsSignedUrlService(GcsProperties gcsProperties) throws IOException {
        this.gcsProperties = gcsProperties;

        GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault();
        StorageOptions.Builder storageOptionsBuilder = StorageOptions.newBuilder().setCredentials(googleCredentials);
        if (!gcsProperties.projectId().isBlank()) {
            storageOptionsBuilder.setProjectId(gcsProperties.projectId());
        }
        this.storage = storageOptionsBuilder.build().getService();

        if (googleCredentials instanceof ServiceAccountSigner serviceAccountSigner) {
            this.signer = serviceAccountSigner;
            log.info("Using ServiceAccountSigner from application default credentials for GCS signed URLs");
        } else {
            String signerEmail = resolveSignerServiceAccountEmail();
            this.signer = new IamServiceAccountSigner(signerEmail);
            log.info("Using IAM SignBlob fallback signer for GCS signed URLs with serviceAccount={}", signerEmail);
        }
    }

    public SignedUploadUrl createUploadSignedUrl(String objectPath, String contentType,
            Map<String, String> metadataHeaders) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName(), objectPath))
                .setContentType(contentType)
                .build();

        Map<String, String> extHeaders = new HashMap<>();
        // Do not sign Content-Type as an ext header for browser uploads.
        // Some browser/proxy combinations can drop or normalize it in a way that
        // makes GCS reject the request with MalformedSecurityHeader.
        // Keep only explicit metadata headers in the signature.
        metadataHeaders.forEach((key, value) -> extHeaders.put(key.toLowerCase(), value));

        URL signedUrl = storage.signUrl(
                blobInfo,
                gcsProperties.signedUrlTtlMinutes(),
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(extHeaders),
                Storage.SignUrlOption.signWith(signer));

        return new SignedUploadUrl(
                signedUrl.toString(),
                Instant.now().plusSeconds(gcsProperties.signedUrlTtlMinutes() * 60),
                extHeaders);
    }

    public SignedDownloadUrl createDownloadSignedUrl(String objectPath) {
        BlobId blobId = BlobId.of(bucketName(), objectPath);
        Blob blob = storage.get(blobId);
        if (blob == null || !blob.exists()) {
            throw new IllegalStateException("Object does not exist in bucket for path=" + objectPath);
        }

        URL signedUrl = storage.signUrl(
                blob.asBlobInfo(),
                gcsProperties.signedUrlTtlMinutes(),
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.signWith(signer));

        return new SignedDownloadUrl(
                signedUrl.toString(),
                Instant.now().plusSeconds(gcsProperties.signedUrlTtlMinutes() * 60));
    }

    private String bucketName() {
        if (gcsProperties.bucketName().isBlank()) {
            throw new IllegalStateException("gcs.bucket-name must be configured for signed URL generation");
        }

        return gcsProperties.bucketName();
    }

    private String resolveSignerServiceAccountEmail() {
        if (!gcsProperties.signingServiceAccountEmail().isBlank()) {
            return gcsProperties.signingServiceAccountEmail();
        }

        String envValue = System.getenv("GCS_SIGNING_SERVICE_ACCOUNT_EMAIL");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        @Nullable
        String metadataEmail = resolveServiceAccountEmailFromMetadata();
        if (metadataEmail != null && !metadataEmail.isBlank()) {
            return metadataEmail;
        }

        throw new IllegalStateException(
                "Unable to resolve signing service account email. Set gcs.signing-service-account-email or GCS_SIGNING_SERVICE_ACCOUNT_EMAIL.");
    }

    private @Nullable String resolveServiceAccountEmailFromMetadata() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METADATA_EMAIL_ENDPOINT))
                    .header("Metadata-Flavor", "Google")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body().trim();
            }

            return null;
        } catch (Exception ex) {
            log.info("Could not resolve service account email from metadata endpoint", ex);
            return null;
        }
    }

    private static final class IamServiceAccountSigner implements ServiceAccountSigner {

        private final String serviceAccountEmail;

        private IamServiceAccountSigner(String serviceAccountEmail) {
            this.serviceAccountEmail = serviceAccountEmail;
        }

        @Override
        public String getAccount() {
            return serviceAccountEmail;
        }

        @Override
        public byte[] sign(byte[] toSign) {
            try (IamCredentialsClient client = IamCredentialsClient.create()) {
                SignBlobRequest request = SignBlobRequest.newBuilder()
                        .setName("projects/-/serviceAccounts/" + serviceAccountEmail)
                        .setPayload(ByteString.copyFrom(toSign))
                        .build();
                return client.signBlob(request).getSignedBlob().toByteArray();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to initialize IAM Credentials client for signing", ex);
            }
        }
    }

    public record SignedUploadUrl(
            String url,
            Instant expiresAt,
            Map<String, String> requiredHeaders) {
    }

    public record SignedDownloadUrl(
            String url,
            Instant expiresAt) {
    }
}
