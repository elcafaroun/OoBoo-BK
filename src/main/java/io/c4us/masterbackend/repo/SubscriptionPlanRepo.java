package io.c4us.masterbackend.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.c4us.masterbackend.domain.SubscriptionPlan;

public interface SubscriptionPlanRepo extends JpaRepository<SubscriptionPlan, Long>{
    Optional<SubscriptionPlan> findByName(String name);
    
}
