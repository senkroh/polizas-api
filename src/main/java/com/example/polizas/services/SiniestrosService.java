package com.example.polizas.services;

import com.example.polizas.model.ResourceNotFoundException;
import com.example.polizas.model.Siniestro;
import com.example.polizas.model.SiniestroExternal;
import com.example.polizas.model.UpstreamServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class SiniestrosService {

    private final RestClient restClient;

    public SiniestrosService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Siniestro getSiniestroById(String siniestroId) {
        try{
            SiniestroExternal ext = restClient.get()
                    .uri("/siniestros/{siniestroId}", siniestroId)
                    .retrieve()
                    .body(SiniestroExternal.class);

            return toSiniestro(ext);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Siniestro no encontrado: " + siniestroId);
        } catch (RestClientResponseException e) {
            throw new UpstreamServiceException("Error al obtener siniestro", e);
        }

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
