package com.example.polizas.controller;

import com.example.polizas.model.Siniestro;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/siniestros")
public class SiniestrosController {

    @GetMapping("/{siniestroId}")
    public Siniestro getSiniestroById(@PathVariable String siniestroId) {
        Siniestro siniestro = new Siniestro();
        siniestro.setSiniestroId(siniestroId);
        siniestro.setDescripcion("Detalle del siniestro con id_siniestro: " + siniestroId);
        siniestro.setEstado("En proceso");
        siniestro.setFecha("2024-08-02");
        return siniestro;
    }
}
