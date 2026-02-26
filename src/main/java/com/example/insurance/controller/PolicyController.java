package com.example.insurance.controller;

import com.example.insurance.model.Policy;
import com.example.insurance.model.Claim;
import com.example.insurance.services.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/policies")
@Tag(name = "Policies", description = "Manage insurance policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @Operation(summary = "Recupera todas las polizas", description = "Devuelve las polizas para el usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Polizas recuperadas correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "502", description = "Servicio externo no disponible")
    })
    @GetMapping
    public List<Policy> getPoliciesByNationalId(Principal principal) {
        String nationalId = principal.getName();
        return policyService.getPoliciesByNationalId(nationalId);
    }

    @Operation(summary = "Recupera polizas por Id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Poliza recuperada correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "La poliza no pertenece al usuario autenticado"),
            @ApiResponse(responseCode = "502", description = "Servicio externo no disponible")
    })
    @GetMapping("/{policyId}")
    public Policy getPolicyById(
            @Parameter(description = "ID de la poliza", example = "1234512345678A") @PathVariable String policyId,
            Principal principal) {
        String nationalId = principal.getName();
        return policyService.getPolicyById(policyId, nationalId);
    }

    @Operation(summary = "Recupera condiciones de una poliza", description = "Devuelve las condiciones de la poliza. Retorna 403 si la poliza no pertenece al usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Condiciones recuperadas correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "La poliza no pertenece al usuario autenticado"),
            @ApiResponse(responseCode = "502", description = "Servicio externo no disponible")
    })
    @GetMapping("/{policyId}/conditions")
    public List<String> getConditions(
            @Parameter(description = "ID de la poliza", example = "1234512345678A") @PathVariable String policyId,
            Principal principal) {
        String nationalId = principal.getName();
        return policyService.getConditions(policyId, nationalId);
    }

    @Operation(summary = "Recupera siniestros de una poliza", description = "Devuelve los siniestros vinculados a una poliza. Retorna 403 si la poliza no pertenece al usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Siniestros recuperados correctamente"),
            @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
            @ApiResponse(responseCode = "403", description = "La poliza no pertenece al usuario autenticado"),
            @ApiResponse(responseCode = "502", description = "Servicio externo no disponible")
    })
    @GetMapping("/{policyId}/claims")
    public List<Claim> getClaims(
            @Parameter(description = "ID de la poliza", example = "1234512345678A") @PathVariable String policyId,
            Principal principal) {
        String nationalId = principal.getName();
        return policyService.getClaims(policyId, nationalId);
    }
}
