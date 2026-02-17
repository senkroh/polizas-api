package com.example.polizas.model;

import java.util.List;

public class Poliza {

    private String polizaId;
    private String descripcion;
    private List<String> coberturas;

    public String getPolizaId() {
        return polizaId;
    }

    public void setPolizaId(String polizaId) {
        this.polizaId = polizaId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<String> getCoberturas() {
        return coberturas;
    }

    public void setCoberturas(List<String> coberturas) {
        this.coberturas = coberturas;
    }

    @Override
    public String toString() {
        return "Poliza{" +
                "polizaId='" + polizaId + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", coberturas=" + coberturas +
                '}';
    }
}
