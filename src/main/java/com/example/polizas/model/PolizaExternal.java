package com.example.polizas.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class PolizaExternal {

    private String polizaId;
    private String descripcion;
    private List<String> coberturas;


}
