package com.drobnyd.drobnyd.orders.api;

import com.drobnyd.drobnyd.orders.OrderService;
import com.drobnyd.drobnyd.api.ApiResponse;
import com.drobnyd.drobnyd.api.ApiMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestBody UpdateStatusRequest request) {

        orderService.changeOrderStatus(orderId, request.status());

        Map<String, String> data = Map.of("orderId", String.valueOf(orderId), "status", request.status());
        ApiMeta meta = new ApiMeta(UUID.randomUUID().toString(), Instant.now());
        ApiResponse<Map<String, String>> response = new ApiResponse<>(data, meta);

        return ResponseEntity.ok(response);
    }
}

record UpdateStatusRequest(String status) {}