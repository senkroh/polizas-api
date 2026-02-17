package com.example.polizas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Siniestro {

    private String siniestroId;
    private String descripcion;
    private String estado;
    private String fecha;


}
