package com.example.insurance.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
@Schema(description = "An insurance policy")
public class Policy {

    @Schema(description = "Unique policy identifier", example = "1234512345678A")
    private String policyId;

    @Schema(description = "Policy description")
    private String description;

    @Schema(description = "List of coverages included in the policy")
    private List<String> coverages;

}
