package com.drobnyd.drobnyd.config;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.drobnyd.drobnyd.config.properties.FirebaseAdminProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private final FirebaseAdminProperties firebaseAdminProperties;

    public FirebaseConfig(FirebaseAdminProperties firebaseAdminProperties) {
        this.firebaseAdminProperties = firebaseAdminProperties;
    }

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(resolveCredentials())
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully using {}",
                    firebaseAdminProperties.useApplicationDefaultCredentials()
                            ? "Application Default Credentials"
                            : "classpath service account resource " + firebaseAdminProperties.serviceAccountResource());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Firebase Admin SDK", e);
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        if (firebaseAdminProperties.useApplicationDefaultCredentials()) {
            return GoogleCredentials.getApplicationDefault();
        }

        String serviceAccountResource = firebaseAdminProperties.serviceAccountResource();
        if (serviceAccountResource == null) {
            throw new IllegalStateException(
                    "Firebase Admin SDK requires firebase.admin.service-account-resource when ADC is disabled.");
        }

        ClassPathResource resource = new ClassPathResource(serviceAccountResource);
        try (InputStream inputStream = resource.getInputStream()) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }
}