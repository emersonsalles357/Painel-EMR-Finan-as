package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.dto.DashboardResponse;
import br.com.emr.emrfinancas.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> consolidar() {
        return ResponseEntity.ok(dashboardService.consolidar());
    }
}
