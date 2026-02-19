package com.example.insurance.controller;

import com.example.insurance.model.Policy;
import com.example.insurance.model.Claim;
import com.example.insurance.services.PolicyService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public List<Policy> getPoliciesByNationalId(Principal principal) {
        String nationalId = principal.getName();
        return policyService.getPoliciesByNationalId(nationalId);
    }

    @GetMapping("/{policyId}")
    public Policy getPolicyById(@PathVariable String policyId, Principal principal) {
        String nationalId = principal.getName();
        return policyService.getPolicyById(policyId, nationalId);
    }

    @GetMapping("/{policyId}/conditions")
    public List<String> getConditions(@PathVariable String policyId, Principal principal) {
        String nationalId = principal.getName();
        return policyService.getConditions(policyId, nationalId);
    }

    @GetMapping("/{policyId}/claims")
    public List<Claim> getClaims(@PathVariable String policyId, Principal principal) {
        String nationalId = principal.getName();
        return policyService.getClaims(policyId, nationalId);
    }
}
