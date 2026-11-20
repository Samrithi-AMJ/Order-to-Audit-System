package com.nittrichy.ordertoaudit.controller;

import com.nittrichy.ordertoaudit.dto.BenchmarkResponseDto;
import com.nittrichy.ordertoaudit.service.BenchmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @Value("${app.benchmark.enabled:false}")
    private boolean benchmarkEnabled;

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<BenchmarkResponseDto> runBenchmark() {
        if (!benchmarkEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(benchmarkService.runBenchmark());
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> getBenchmarkStatus() {
        // Benchmark results are not persisted, so status must not rerun destructive setup.
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}

