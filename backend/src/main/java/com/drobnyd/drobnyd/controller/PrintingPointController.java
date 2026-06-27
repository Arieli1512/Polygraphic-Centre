package com.drobnyd.drobnyd.controller;

import com.drobnyd.drobnyd.dto.ApiMeta;
import com.drobnyd.drobnyd.dto.ApiResponse;
import com.drobnyd.drobnyd.dto.ApiPageResponse;
import com.drobnyd.drobnyd.dto.ApiPageMeta;
import com.drobnyd.drobnyd.dto.PrintingPointResponse;
import com.drobnyd.drobnyd.common.ApiPaginationLinks;
import com.drobnyd.drobnyd.validation.PaginationValidator;

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
    public ApiPageResponse<PrintingPointResponse> getPrintingPoints(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        HttpServletRequest request
    ) {
        PaginationValidator.validate(page, size);

        List<PrintingPointResponse> printingPoints = printingPointService.findAll();

        int totalItems = printingPoints.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        int fromIndex = Math.min(page * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<PrintingPointResponse> pageData = printingPoints.subList(fromIndex, toIndex);

        return new ApiPageResponse<>(
            pageData,
            new ApiPageMeta(
                getRequestId(request),
                page,
                size,
                totalItems,
                totalPages,
                Instant.now()
            ),
            ApiPaginationLinks.build(request, page, size, totalPages)
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