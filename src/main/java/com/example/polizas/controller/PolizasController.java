package com.example.polizas.controller;

import com.example.polizas.model.Poliza;
import com.example.polizas.model.Siniestro;
import com.example.polizas.services.PolizasService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/polizas")
public class PolizasController {

    private final PolizasService polizasService;

    public PolizasController(PolizasService polizasService) {
        this.polizasService = polizasService;
    }

    @GetMapping
    public List<Poliza> getPolizasByDni(Principal principal) {
        String dni = principal.getName();
        return polizasService.getPolizasByDni(dni);
    }

    @GetMapping("/{polizaId}")
    public Poliza getPolizaById(@PathVariable String polizaId, Principal principal) {
        String dni = principal.getName();
        return polizasService.getPolizaById(polizaId, dni);
    }

    @GetMapping("/{polizaId}/condiciones")
    public List<String> getCondiciones(@PathVariable String polizaId, Principal principal) {
        String dni = principal.getName();
        return polizasService.getCondiciones(polizaId, dni);
    }

    @GetMapping("/{polizaId}/siniestros")
    public List<Siniestro> getSiniestros(@PathVariable String polizaId, Principal principal) {
        String dni = principal.getName();
        return polizasService.getSiniestros(polizaId, dni);
    }
}
