package com.example.insurance.services;

import com.example.insurance.client.PolicyClient;
import com.example.insurance.external.ExternalClaim;
import com.example.insurance.external.ExternalPolicy;
import com.example.insurance.mappers.ClaimMapper;
import com.example.insurance.mappers.PolicyMapper;
import com.example.insurance.model.Claim;
import com.example.insurance.model.Policy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PolicyService {

    private final PolicyClient policyClient;
    private final PolicyCacheService policyCacheService;
    private final PolicyMapper policyMapper;
    private final ClaimMapper claimMapper;

    public PolicyService(PolicyClient policyClient, PolicyCacheService policyCacheService, PolicyMapper policyMapper, ClaimMapper claimMapper) {
        this.policyClient = policyClient;
        this.policyCacheService = policyCacheService;
        this.policyMapper = policyMapper;
        this.claimMapper = claimMapper;
    }

    public List<Policy> getPoliciesByNationalId(String nationalId) {
        return policyCacheService.getPoliciesByNationalId(nationalId);
    }

    @Cacheable("policy")
    public Policy getPolicyById(String policyId, String nationalId) {
        checkOwnership(policyId, nationalId);
        return policyMapper.toPolicy(policyClient.fetchById(policyId));
    }

    @Cacheable("conditions")
    public List<String> getConditions(String policyId, String nationalId) {
        checkOwnership(policyId, nationalId);
        return policyClient.fetchConditions(policyId);
    }

    public List<Claim> getClaims(String policyId, String nationalId) {
        checkOwnership(policyId, nationalId);
        return policyClient.fetchClaims(policyId)
                .stream()
                .map(claimMapper::toClaim)
                .toList();
    }

    private void checkOwnership(String policyId, String nationalId) {
        List<Policy> userPolicies = policyCacheService.getPoliciesByNationalId(nationalId);
        boolean owns = userPolicies.stream().anyMatch(p -> p.getPolicyId().equals(policyId));
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to policy " + policyId);
        }
    }

}
