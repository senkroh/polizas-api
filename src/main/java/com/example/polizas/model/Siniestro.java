package com.example.polizas.model;

public class Siniestro {

    private String siniestroId;
    private String descripcion;
    private String estado;
    private String fecha;

    public String getSiniestroId() {
        return siniestroId;
    }

    public void setSiniestroId(String siniestroId) {
        this.siniestroId = siniestroId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    @Override
    public String toString() {
        return "Siniestro{" +
                "siniestroId='" + siniestroId + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", estado='" + estado + '\'' +
                ", fecha='" + fecha + '\'' +
                '}';
    }
}
