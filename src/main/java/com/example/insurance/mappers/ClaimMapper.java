package com.example.insurance.mappers;

import com.example.insurance.external.ExternalClaim;
import com.example.insurance.model.Claim;
import org.springframework.stereotype.Component;

@Component
public class ClaimMapper {

    public Claim toClaim(ExternalClaim external) {
        Claim claim = new Claim();
        claim.setClaimId(external.getClaimId());
        claim.setDescription(external.getDescription());
        claim.setStatus(external.getStatus());
        claim.setDate(external.getDate());
        return claim;
    }

}
