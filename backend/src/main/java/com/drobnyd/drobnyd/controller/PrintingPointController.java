package com.drobnyd.drobnyd.controller;

import com.drobnyd.drobnyd.dto.ApiMeta;
import com.drobnyd.drobnyd.dto.ApiResponse;
import com.drobnyd.drobnyd.dto.ApiPageResponse;
import com.drobnyd.drobnyd.dto.ApiPageMeta;
import com.drobnyd.drobnyd.dto.PageResult;
import com.drobnyd.drobnyd.dto.PrintingPointResponse;
import com.drobnyd.drobnyd.common.ApiPaginationLinks;
import com.drobnyd.drobnyd.service.PrintingPointService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/printing-points")
public class PrintingPointController {
    private final PrintingPointService printingPointService;

    public PrintingPointController(PrintingPointService printingPointService) {
        this.printingPointService = printingPointService;
    }

    @GetMapping
    public ApiPageResponse<PrintingPointResponse> getPrintingPoints(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        HttpServletRequest request
    ) {
        PageResult<PrintingPointResponse> printingPoints = printingPointService.findAll(page, size);

        return new ApiPageResponse<>(
            printingPoints.items(),
            new ApiPageMeta(
                getRequestId(request),
                printingPoints.page(),
                printingPoints.size(),
                printingPoints.totalItems(),
                printingPoints.totalPages(),
                Instant.now()
            ),
            ApiPaginationLinks.build(
                request,
                printingPoints.page(),
                printingPoints.size(),
                printingPoints.totalPages()
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

    @DeleteMapping("/{printingPointId}")
    public ApiResponse<Integer> deletePrintingPoint(
        @PathVariable String printingPointId,
        HttpServletRequest request
    ) {
        return new ApiResponse<>(
            printingPointService.deleteById(printingPointId),
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