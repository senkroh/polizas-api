package com.example.polizas.controller;

import com.example.polizas.model.Poliza;
import com.example.polizas.model.Siniestro;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/polizas")
public class PolizasController {

    @GetMapping
    public List<Poliza> getPolizasByDni(@RequestParam String dni) {
        Poliza poliza = new Poliza();
        poliza.setPolizaId("12345" + dni);
        poliza.setDescripcion("Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
        return List.of(poliza);
    }

    @GetMapping("/{polizaId}")
    public Poliza getPolizaById(@PathVariable String polizaId) {
        Poliza poliza = new Poliza();
        poliza.setPolizaId(polizaId);
        poliza.setDescripcion("Detalle de la póliza con id_poliza: " + polizaId);
        poliza.setCoberturas(Arrays.asList("Cobertura A", "Cobertura B"));
        return poliza;
    }

    @GetMapping("/{polizaId}/condiciones")
    public List<String> getCondiciones(@PathVariable String polizaId) {
        return Arrays.asList("Condición A", "Condición B", "Condición C");
    }

    @GetMapping("/{polizaId}/siniestros")
    public List<Siniestro> getSiniestros(@PathVariable String polizaId) {
        Siniestro s1 = new Siniestro();
        s1.setSiniestroId("A" + polizaId);
        s1.setEstado("En proceso");

        Siniestro s2 = new Siniestro();
        s2.setSiniestroId("B" + polizaId);
        s2.setEstado("En proceso");

        return Arrays.asList(s1, s2);
    }
}
