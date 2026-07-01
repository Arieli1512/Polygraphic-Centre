package com.drobnyd.drobnyd.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.Operator;

public interface OperatorRepository extends JpaRepository<Operator, Integer> {

	Optional<Operator> findByFirebaseUid(@org.jspecify.annotations.NonNull String firebaseUid);
}