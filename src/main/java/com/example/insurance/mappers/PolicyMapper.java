package com.example.insurance.mappers;

import com.example.insurance.external.ExternalPolicy;
import com.example.insurance.model.Policy;
import org.springframework.stereotype.Component;

@Component
public class PolicyMapper {

    public Policy toPolicy(ExternalPolicy external) {
        Policy policy = new Policy();
        policy.setPolicyId(external.getPolicyId());
        policy.setDescription(external.getDescription());
        policy.setCoverages(external.getCoverages());
        return policy;
    }

}
