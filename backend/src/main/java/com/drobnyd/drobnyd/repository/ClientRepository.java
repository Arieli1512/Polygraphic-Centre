package com.drobnyd.drobnyd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Integer> {

	Optional<Client> findByFirebaseUid(@org.jspecify.annotations.NonNull String firebaseUid);
}