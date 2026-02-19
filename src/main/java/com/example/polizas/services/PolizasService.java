package com.example.polizas.services;

import com.example.polizas.model.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolizasService {

    private final RestClient restClient;
    private final PolizasCacheService polizasCacheService;

    public PolizasService(RestClient restClient, PolizasCacheService polizasCacheService) {
        this.restClient = restClient;
        this.polizasCacheService = polizasCacheService;
    }

    public List<Poliza> getPolizasByDni(String dni) {
        return polizasCacheService.getPolizasByDni(dni);
    }

    @Cacheable("poliza")
    @CircuitBreaker(name = "wiremock", fallbackMethod = "getPolizasByIdFallback")
    public Poliza getPolizaById(String polizaId, String dni) {
        checkOwnership(polizaId, dni);
        try {
            PolizaExternal ext = restClient.get()
                    .uri("/polizas/{polizaId}", polizaId)
                    .retrieve()
                    .body(PolizaExternal.class);
            return toPoliza(ext);

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Poliza no encontrada: " + polizaId);
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error al obtener poliza", e);
        }
    }
    public Poliza getPolizasByIdFallback(String polizaId, String dni, Throwable t) {
        throw new UpstreamServiceException("Servicio no disponible", t);
    }

    @Cacheable("condiciones")
    @CircuitBreaker(name = "wiremock", fallbackMethod = "getCondicionesFallback")
    public List<String> getCondiciones(String polizaId, String dni) {
        try{
            checkOwnership(polizaId, dni);
            return restClient.get()
                    .uri("/polizas/{polizaId}/condiciones", polizaId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {});
        } catch (RestClientException e){
            throw new UpstreamServiceException("Error al obtener poliza", e);
        }
    }
    public List<String> getCondicionesFallback(String polizaId, String dni, Throwable t) {
        throw new UpstreamServiceException("Servicio no disponible", t);
    }

    @CircuitBreaker(name = "wiremock", fallbackMethod = "getSiniestrosFallback")
    public List<Siniestro> getSiniestros(String polizaId, String dni) {
        checkOwnership(polizaId, dni);
        try{
            List<SiniestroExternal> externals = restClient.get()
                    .uri("/polizas/{polizaId}/siniestros", polizaId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<SiniestroExternal>>() {});

            return externals.stream()
                    .map(this::toSiniestro)
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error al obtener siniestros",e);
        }

    }

    public List<Siniestro> getSiniestrosFallback(String polizaId,String dni, Throwable t) {
        throw new UpstreamServiceException("Servicio no disponible", t);
    }

    private void checkOwnership(String polizaId, String dni) {
        List<Poliza> userPolizas = polizasCacheService.getPolizasByDni(dni);
        boolean owns = userPolizas.stream().anyMatch(p -> p.getPolizaId().equals(polizaId));
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to poliza " + polizaId);
        }
    }

    private Poliza toPoliza(PolizaExternal ext) {
        Poliza poliza = new Poliza();
        poliza.setPolizaId(ext.getPolizaId());
        poliza.setDescripcion(ext.getDescripcion());
        poliza.setCoberturas(ext.getCoberturas());
        return poliza;
    }

    private Siniestro toSiniestro(SiniestroExternal ext) {
        Siniestro siniestro = new Siniestro();
        siniestro.setSiniestroId(ext.getSiniestroId());
        siniestro.setDescripcion(ext.getDescripcion());
        siniestro.setEstado(ext.getEstado());
        siniestro.setFecha(ext.getFecha());
        return siniestro;
    }
}
