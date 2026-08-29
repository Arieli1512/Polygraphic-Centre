package com.drobnyd.drobnyd.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class Client {

    public static Client provisioned(String firebaseUid, String email, String firstName, String lastName) {
        Client client = new Client();
        client.setFirebaseUid(firebaseUid);
        client.setEmail(email);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setStatus(ClientStatus.ACTIVE);
        return client;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id", nullable = false, updatable = false)
    @ToString.Include
    private Integer clientId;

    @Column(nullable = false, unique = true, length = 255)
    @ToString.Include
    private String email;

    @Column(name = "firebase_uid", nullable = false, unique = true, length = 255)
    @ToString.Include
    private String firebaseUid;

    @Column(name = "first_name", nullable = false, length = 100)
    @ToString.Include
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    @ToString.Include
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @ToString.Include
    private ClientStatus status = ClientStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}