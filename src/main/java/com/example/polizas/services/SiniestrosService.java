package com.example.polizas.services;

import com.example.polizas.model.Siniestro;
import com.example.polizas.model.SiniestroExternal;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SiniestrosService {

    private final RestClient restClient;

    public SiniestrosService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Siniestro getSiniestroById(String siniestroId) {
        SiniestroExternal ext = restClient.get()
                .uri("/siniestros/{siniestroId}", siniestroId)
                .retrieve()
                .body(SiniestroExternal.class);

        return toSiniestro(ext);
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
