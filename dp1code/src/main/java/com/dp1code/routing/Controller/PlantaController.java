package com.dp1code.routing.Controller;

import com.dp1code.routing.Model.Planta;
import com.dp1code.routing.Service.PlantaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantas")
public class PlantaController {

    @Autowired
    private PlantaService plantaService;

    @GetMapping("/obtenerPlantas")
    public List<Planta> obtenerPlanta() {
        return plantaService.obtenerTodas();
    }

    

}
