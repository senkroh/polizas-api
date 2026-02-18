package com.example.polizas.services;

import com.example.polizas.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolizasService {

    //TODO: gestión de errores

    private final RestClient restClient;

    public PolizasService(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Poliza> getPolizasByDni(String dni) {
        try{
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

    public List<Siniestro> getSiniestros(String polizaId, String dni) {
        checkOwnership(polizaId, dni);
        List<SiniestroExternal> externals = restClient.get()
                .uri("/polizas/{polizaId}/siniestros", polizaId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<SiniestroExternal>>() {});

        return externals.stream()
                .map(this::toSiniestro)
                .collect(Collectors.toList());
    }

    private void checkOwnership(String polizaId, String dni) {
        List<Poliza> userPolizas = getPolizasByDni(dni);
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
        siniestro.setEstado(ext.getEstado());
        return siniestro;
    }
}
