package com.dp1code.routing.Controller;

import com.dp1code.routing.dto.BloqueoDTO;
import com.dp1code.routing.Service.BloqueoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bloqueos")
public class BloqueoController {

    @Autowired
    private BloqueoService bloqueoService;

    @PostMapping("/registrar")
    public String registrar(@RequestBody BloqueoDTO bloqueo) {
        bloqueoService.registrarBloqueo(bloqueo);
        return "Bloqueo registrado correctamente.";
    }
}
