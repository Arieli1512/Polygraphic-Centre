package com.drobnyd.drobnyd.controller;

import com.drobnyd.drobnyd.dto.ApiMeta;
import com.drobnyd.drobnyd.dto.ApiResponse;
import com.drobnyd.drobnyd.dto.PrintingPointResponse;
import com.drobnyd.drobnyd.service.PrintingPointService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/printing-points")
public class PrintingPointController {
    private final PrintingPointService printingPointService;

    public PrintingPointController(PrintingPointService printingPointService) {
        this.printingPointService = printingPointService;
    }

    @GetMapping
    public ApiResponse<List<PrintingPointResponse>> getPrintingPoints(
            HttpServletRequest request
    ) {
        return new ApiResponse<>(
                printingPointService.findAll(),
                new ApiMeta(
                        getRequestId(request),
                        Instant.now()
                )
        );
    }


    @GetMapping("/{printingPointId}")
    public ApiResponse<PrintingPointResponse> getPrintingPoint(
            @PathVariable String printingPointId,
            HttpServletRequest request
    ) {
        return new ApiResponse<>(
                printingPointService.findById(printingPointId),
                new ApiMeta(
                        getRequestId(request),
                        Instant.now()
                )
        );
    }

    private String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute("requestId");
    }
}