package com.drobnyd.drobnyd.printingpoint;

import com.drobnyd.drobnyd.api.ApiMeta;
import com.drobnyd.drobnyd.api.ApiResponse;
import com.drobnyd.drobnyd.api.ApiPageResponse;
import com.drobnyd.drobnyd.api.ApiPageMeta;
import com.drobnyd.drobnyd.pagination.PageResult;
import com.drobnyd.drobnyd.printingpoint.dto.PrintingPointRequest;
import com.drobnyd.drobnyd.printingpoint.dto.PrintingPointResponse;
import com.drobnyd.drobnyd.pagination.ApiPaginationLinks;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/printing-points")
public class PrintingPointController {
    private final PrintingPointService printingPointService;
    private final PrintingPointValidator printingPointValidator;

    public PrintingPointController(
        PrintingPointService printingPointService,
        PrintingPointValidator printingPointValidator
    ) {
        this.printingPointService = printingPointService;
        this.printingPointValidator = printingPointValidator;
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

    @PostMapping
    public ApiResponse<PrintingPointResponse> createPrintingPoint(
        @Valid @RequestBody PrintingPointRequest printingPointRequest,
        BindingResult bindingResult,
        HttpServletRequest request
    ) {
        printingPointValidator.validate(printingPointRequest, bindingResult);

        return new ApiResponse<>(
            printingPointService.create(printingPointRequest),
            new ApiMeta(
                getRequestId(request),
                Instant.now()
            )
        );
    }

    @PutMapping("/{printingPointId}")
    public ApiResponse<PrintingPointResponse> updatePrintingPoint(
        @PathVariable String printingPointId,
        @Valid @RequestBody PrintingPointRequest printingPointRequest,
        BindingResult bindingResult,
        HttpServletRequest request
    ) {
        printingPointValidator.validate(printingPointRequest, bindingResult);

        return new ApiResponse<>(
            printingPointService.update(printingPointId, printingPointRequest),
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