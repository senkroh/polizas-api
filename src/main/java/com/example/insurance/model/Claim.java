package com.example.insurance.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Claim {

    private String claimId;
    private String description;
    private String status;
    private String date;

}
