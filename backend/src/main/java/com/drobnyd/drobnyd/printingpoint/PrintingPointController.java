package com.drobnyd.drobnyd.printingpoint;

import com.drobnyd.drobnyd.api.ApiMeta;
import com.drobnyd.drobnyd.api.ApiResponse;
import com.drobnyd.drobnyd.api.ApiPageResponse;
import com.drobnyd.drobnyd.api.ApiPageMeta;
import com.drobnyd.drobnyd.pagination.PageResult;
import com.drobnyd.drobnyd.printingpoint.dto.PrintingPointRequest;
import com.drobnyd.drobnyd.printingpoint.dto.PrintingPointResponse;
import com.drobnyd.drobnyd.pagination.ApiPaginationLinks;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Printing points", description = "Printing point CRUD operations.")
@RestController
@RequestMapping(value = "/api/v1/printing-points", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @Operation(summary = "List printing points")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Printing points page"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
    })
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

    @Operation(summary = "Get printing point by id")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Printing point"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
    })
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

    @Operation(summary = "Create printing point")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Printing point created"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", ref = "#/components/responses/ValidationError"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @Operation(summary = "Update printing point")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Printing point updated"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", ref = "#/components/responses/ValidationError"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
    })
    @PutMapping(value = "/{printingPointId}", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @Operation(summary = "Delete printing point")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Deleted printing point id"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
    })
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