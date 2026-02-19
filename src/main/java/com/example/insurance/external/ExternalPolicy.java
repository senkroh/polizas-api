package com.example.insurance.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class ExternalPolicy {

    @JsonProperty("polizaId")
    private String policyId;
    @JsonProperty("descripcion")
    private String description;
    @JsonProperty("coberturas")
    private List<String> coverages;

}
