package com.exploresg.authservice.repository;

import com.exploresg.authservice.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<TestEntity, Long> {
}
