package com.example.polizas.services;

import com.example.polizas.model.Poliza;
import com.example.polizas.model.PolizaExternal;
import com.example.polizas.model.Siniestro;
import com.example.polizas.model.SiniestroExternal;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolizasService {

    //TODO: gestión de autorización,gestión de errores

    private final RestClient restClient;

    public PolizasService(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Poliza> getPolizasByDni(String dni) {
        List<PolizaExternal> externals = restClient.get()
                .uri("/polizas?dni={dni}", dni)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PolizaExternal>>() {});

        return externals.stream()
                .map(this::toPoliza)
                .collect(Collectors.toList());
    }

    public Poliza getPolizaById(String polizaId) {
        PolizaExternal ext = restClient.get()
                .uri("/polizas/{polizaId}", polizaId)
                .retrieve()
                .body(PolizaExternal.class);

        return toPoliza(ext);
    }

    public List<String> getCondiciones(String polizaId) {
        return restClient.get()
                .uri("/polizas/{polizaId}/condiciones", polizaId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});
    }

    public List<Siniestro> getSiniestros(String polizaId) {
        List<SiniestroExternal> externals = restClient.get()
                .uri("/polizas/{polizaId}/siniestros", polizaId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<SiniestroExternal>>() {});

        return externals.stream()
                .map(this::toSiniestro)
                .collect(Collectors.toList());
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
