package com.example.insurance.controller;

import com.example.insurance.model.Claim;
import com.example.insurance.services.ClaimService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/{claimId}")
    public Claim getClaimById(@PathVariable String claimId) {
        return claimService.getClaimById(claimId);
    }
}
