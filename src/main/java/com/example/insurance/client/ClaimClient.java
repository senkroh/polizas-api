package com.example.insurance.client;

import com.example.insurance.exceptions.ResourceNotFoundException;
import com.example.insurance.exceptions.UpstreamServiceException;
import com.example.insurance.external.ExternalClaim;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClaimClient {

    private final RestClient restClient;

    public ClaimClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "wiremock", fallbackMethod = "fetchByIdFallback")
    public ExternalClaim fetchById(String claimId) {
        try {
            return restClient.get()
                    .uri("/siniestros/{claimId}", claimId)
                    .retrieve()
                    .body(ExternalClaim.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Claim not found: " + claimId);
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error fetching claim", e);
        }
    }

    public ExternalClaim fetchByIdFallback(String claimId, Throwable t) {
        throw new UpstreamServiceException("Service unavailable", t);
    }
}
