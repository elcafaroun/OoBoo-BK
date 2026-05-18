package io.c4us.masterbackend.service;

import io.c4us.masterbackend.domain.SubscriptionPlan;
import io.c4us.masterbackend.repo.SubscriptionPlanRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SubscriptionPlanService {

    @Autowired
    private SubscriptionPlanRepo repository;

    public List<SubscriptionPlan> getAllPlans() {
        return repository.findAll();
    }
}