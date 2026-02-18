package com.example.polizas.model;

//Clase para cuando el upstream de la api de Wiremock falle
public class UpstreamServiceException extends RuntimeException{
    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
