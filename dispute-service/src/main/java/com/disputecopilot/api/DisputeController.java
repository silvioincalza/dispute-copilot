package com.disputecopilot.api;

import com.disputecopilot.api.dto.ConfirmActionRequest;
import com.disputecopilot.api.dto.CreateDisputeRequest;
import com.disputecopilot.api.dto.DisputeResponse;
import com.disputecopilot.domain.service.DisputeService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @GetMapping
    public List<DisputeResponse> listDisputes() {
        return disputeService.findAll().stream().map(ApiMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DisputeResponse getDispute(@PathVariable UUID id) {
        return ApiMapper.toResponse(disputeService.findById(id));
    }

    @PostMapping
    public ResponseEntity<DisputeResponse> createDispute(@RequestBody CreateDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiMapper.toResponse(disputeService.create(request)));
    }

    @PutMapping("/{id}/confirm-action")
    public DisputeResponse confirmAction(@PathVariable UUID id, @RequestBody(required = false) ConfirmActionRequest request) {
        return ApiMapper.toResponse(disputeService.confirmAction(id, request));
    }
}
