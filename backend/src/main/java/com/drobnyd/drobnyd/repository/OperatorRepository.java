package com.drobnyd.drobnyd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.drobnyd.drobnyd.entity.Operator;

public interface OperatorRepository extends JpaRepository<Operator, Integer> {

	Optional<Operator> findByFirebaseUid(@org.jspecify.annotations.NonNull String firebaseUid);

	Optional<Operator> findByEmail(@org.jspecify.annotations.NonNull String email);

	List<Operator> findAllByOrderByCreatedAtDesc();

	List<Operator> findByPrintingPoint_PrintingPointIdOrderByCreatedAtDesc(Integer printingPointId);

	Optional<Operator> findByOperatorIdAndPrintingPoint_PrintingPointId(Integer operatorId, Integer printingPointId);
}