package ro.daya.dayalog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // I use this endpoint to check whether public routes are really open.
    @GetMapping("/api/health")
    public String health() {
        return "DAYA Log backend is running";
    }
}