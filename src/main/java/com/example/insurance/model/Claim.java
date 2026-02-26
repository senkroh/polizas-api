package com.example.insurance.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Schema(description = "An insurance claim")
public class Claim {

    @Schema(description = "Unique claim identifier", example = "A1234512345678A")
    private String claimId;

    @Schema(description = "Claim description")
    private String description;

    @Schema(description = "Current status of the claim", example = "En proceso")
    private String status;

    @Schema(description = "Date the claim was filed", example = "2024-08-02")
    private String date;

}
