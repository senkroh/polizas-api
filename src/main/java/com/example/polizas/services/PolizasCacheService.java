package com.example.polizas.services;

import com.example.polizas.model.Poliza;
import com.example.polizas.model.PolizaExternal;
import com.example.polizas.model.UpstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolizasCacheService {

    private static final Logger log = LoggerFactory.getLogger(PolizasCacheService.class);

    private final RestClient restClient;

    public PolizasCacheService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Cacheable("polizas")
    @CircuitBreaker(name = "wiremock", fallbackMethod = "getPolizasByDniFallback")
    public List<Poliza> getPolizasByDni(String dni) {
        log.info("Llamando a WireMock para traer polizas por DNI: {}", dni);
        try {
            List<PolizaExternal> externals = restClient.get()
                    .uri("/polizas?dni={dni}", dni)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PolizaExternal>>() {});

            return externals.stream()
                    .map(this::toPoliza)
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error al obtener polizas", e);
        }
    }
    public List<Poliza> getPolizasByDniFallback(String dni, Throwable t) {
        throw new UpstreamServiceException("Servicio no disponible", t);
    }

    private Poliza toPoliza(PolizaExternal ext) {
        Poliza poliza = new Poliza();
        poliza.setPolizaId(ext.getPolizaId());
        poliza.setDescripcion(ext.getDescripcion());
        poliza.setCoberturas(ext.getCoberturas());
        return poliza;
    }
}
