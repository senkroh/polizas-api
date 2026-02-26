package com.example.insurance.controller;

import com.example.insurance.model.Claim;
import com.example.insurance.services.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/claims")
@Tag(name = "Claims", description = "Manage insurance claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @Operation(summary = "Get claim by ID", description = "Returns the full detail of a claim by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "Claim not found"),
            @ApiResponse(responseCode = "502", description = "Upstream service unavailable")
    })
    @GetMapping("/{claimId}")
    public Claim getClaimById(
            @Parameter(description = "The claim ID", example = "A1234512345678A") @PathVariable String claimId) {
        return claimService.getClaimById(claimId);
    }
}
