package com.dp1code.routing.Controller;

import com.dp1code.routing.dto.TimeRangeDTO;
import com.dp1code.routing.Service.TimeRangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timeranges")
public class TimeRangeController {

    @Autowired
    private TimeRangeService timeRangeService;

    @PostMapping("/registrar")
    public String registrar(@RequestBody TimeRangeDTO dto) {
        timeRangeService.registrarTimeRange(dto);
        return "TimeRange registrado correctamente.";
    }
}
