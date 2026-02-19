package com.example.insurance.services;

import com.example.insurance.client.ClaimClient;
import com.example.insurance.mappers.ClaimMapper;
import com.example.insurance.model.Claim;
import org.springframework.stereotype.Service;

@Service
public class ClaimService {

    private final ClaimClient claimClient;
    private final ClaimMapper claimCMapper;

    public ClaimService(ClaimClient claimClient, ClaimMapper claimCMapper) {
        this.claimClient = claimClient;
        this.claimCMapper = claimCMapper;
    }

    public Claim getClaimById(String claimId) {
        return claimCMapper.toClaim(claimClient.fetchById(claimId));
    }

}
