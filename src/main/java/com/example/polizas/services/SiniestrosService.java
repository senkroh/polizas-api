package com.example.polizas.services;

import com.example.polizas.model.ResourceNotFoundException;
import com.example.polizas.model.Siniestro;
import com.example.polizas.model.SiniestroExternal;
import com.example.polizas.model.UpstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SiniestrosService {

    private final RestClient restClient;

    public SiniestrosService(RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "wiremock", fallbackMethod = "getSiniestrosByIdFallback")
    public Siniestro getSiniestroById(String siniestroId) {
        try{
            SiniestroExternal ext = restClient.get()
                    .uri("/siniestros/{siniestroId}", siniestroId)
                    .retrieve()
                    .body(SiniestroExternal.class);

            return toSiniestro(ext);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Siniestro no encontrado: " + siniestroId);
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Error al obtener siniestro", e);
        }

    }
    public Siniestro getSiniestrosByIdFallback(String siniestroId, Throwable t) {
        throw new UpstreamServiceException("Servicio no disponible", t);
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
