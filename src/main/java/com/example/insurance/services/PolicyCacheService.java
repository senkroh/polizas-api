package com.example.insurance.services;

import com.example.insurance.client.PolicyClient;
import com.example.insurance.external.ExternalPolicy;
import com.example.insurance.mappers.PolicyMapper;
import com.example.insurance.model.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyCacheService {

    private static final Logger log = LoggerFactory.getLogger(PolicyCacheService.class);

    private final PolicyClient policyClient;
    private final PolicyMapper policyMapper;

    public PolicyCacheService(PolicyClient policyClient, PolicyMapper policyMapper) {
        this.policyClient = policyClient;
        this.policyMapper = policyMapper;
    }

    @Cacheable("policies")
    public List<Policy> getPoliciesByNationalId(String nationalId) {
        log.info("Calling WireMock to fetch policies by national ID: {}", nationalId);
        return policyClient.fetchByNationalId(nationalId)
                .stream()
                .map(policyMapper::toPolicy)
                .toList();
    }

}
