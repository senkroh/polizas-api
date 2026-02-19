package com.example.insurance.client;

import com.example.insurance.exceptions.ResourceNotFoundException;
import com.example.insurance.exceptions.UpstreamServiceException;
import com.example.insurance.external.ExternalClaim;
import com.example.insurance.external.ExternalPolicy;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class PolicyClient {

    private final RestClient restClient;

    public PolicyClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "wiremock", fallbackMethod = "fetchByNationalIdFallback")
    public List<ExternalPolicy> fetchByNationalId(String nationalId) {
        try {
            return restClient.get()
                    .uri("/polizas?dni={nationalId}", nationalId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ExternalPolicy>>() {});
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error fetching policies", e);
        }
    }

    public List<ExternalPolicy> fetchByNationalIdFallback(String nationalId, Throwable t) {
        throw new UpstreamServiceException("Service unavailable", t);
    }

    @CircuitBreaker(name = "wiremock", fallbackMethod = "fetchByIdFallback")
    public ExternalPolicy fetchById(String policyId) {
        try {
            return restClient.get()
                    .uri("/polizas/{policyId}", policyId)
                    .retrieve()
                    .body(ExternalPolicy.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Policy not found: " + policyId);
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error fetching policy", e);
        }
    }

    public ExternalPolicy fetchByIdFallback(String policyId, Throwable t) {
        throw new UpstreamServiceException("Service unavailable", t);
    }

    @CircuitBreaker(name = "wiremock", fallbackMethod = "fetchConditionsFallback")
    public List<String> fetchConditions(String policyId) {
        try {
            return restClient.get()
                    .uri("/polizas/{policyId}/condiciones", policyId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {});
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error fetching conditions", e);
        }
    }

    public List<String> fetchConditionsFallback(String policyId, Throwable t) {
        throw new UpstreamServiceException("Service unavailable", t);
    }

    @CircuitBreaker(name = "wiremock", fallbackMethod = "fetchClaimsFallback")
    public List<ExternalClaim> fetchClaims(String policyId) {
        try {
            return restClient.get()
                    .uri("/polizas/{policyId}/siniestros", policyId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ExternalClaim>>() {});
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error fetching claims", e);
        }
    }

    public List<ExternalClaim> fetchClaimsFallback(String policyId, Throwable t) {
        throw new UpstreamServiceException("Service unavailable", t);
    }
}
