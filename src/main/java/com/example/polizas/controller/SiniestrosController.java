package com.example.polizas.controller;

import com.example.polizas.model.Siniestro;
import com.example.polizas.services.SiniestrosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/siniestros")
public class SiniestrosController {

    private final SiniestrosService siniestrosService;

    public SiniestrosController(SiniestrosService siniestrosService) {
        this.siniestrosService = siniestrosService;
    }

    @GetMapping("/{siniestroId}")
    public Siniestro getSiniestroById(@PathVariable String siniestroId) {
        return siniestrosService.getSiniestroById(siniestroId);
    }
}
