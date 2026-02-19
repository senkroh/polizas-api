package com.example.insurance.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ExternalClaim {

    @JsonProperty("siniestroId")
    private String claimId;
    @JsonProperty("descripcion")
    private String description;
    @JsonProperty("estado")
    private String status;
    @JsonProperty("fecha")
    private String date;

}
