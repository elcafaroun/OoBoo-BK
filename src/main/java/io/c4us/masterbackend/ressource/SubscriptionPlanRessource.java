package io.c4us.masterbackend.ressource;

import io.c4us.masterbackend.domain.SubscriptionPlan;
import io.c4us.masterbackend.service.SubscriptionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/souscriptions")
public class SubscriptionPlanRessource {

    @Autowired
    private SubscriptionPlanService service;

    @GetMapping
    public List<SubscriptionPlan> getPlans() {
        return service.getAllPlans();
    }
}