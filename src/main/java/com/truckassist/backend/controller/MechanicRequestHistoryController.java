package com.truckassist.backend.controller;

import com.truckassist.backend.dto.MechanicServiceRequestResponse;
import com.truckassist.backend.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mechanics/requests")
public class MechanicRequestHistoryController {

    private final ServiceRequestService service;

    public MechanicRequestHistoryController(
            ServiceRequestService service) {
        this.service = service;
    }

    // =====================================================
    // GET REQUEST HISTORY FOR CURRENT MECHANIC
    // =====================================================

    @GetMapping("/history")
    @Operation(
            summary = "Get request history for current mechanic"
    )
    public List<MechanicServiceRequestResponse> getHistory(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(authentication.getName());

        return service.getRequestHistory(userId);
    }
}
