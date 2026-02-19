package com.example.insurance.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class Policy {

    private String policyId;
    private String description;
    private List<String> coverages;

}
