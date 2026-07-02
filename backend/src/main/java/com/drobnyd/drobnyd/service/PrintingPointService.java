package com.drobnyd.drobnyd.service;

import com.drobnyd.drobnyd.dto.PageResult;
import com.drobnyd.drobnyd.dto.PrintingPointResponse;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PrintingPointService {
    private final List<PrintingPointResponse> printingPoints = new CopyOnWriteArrayList<>(List.of(
        new PrintingPointResponse(
                1,
                "Main Printing Point",
                "Example Street 1",
                "Warsaw",
                "00-001",
                "Poland",
                20
        ),
        new PrintingPointResponse(
                2,
                "Campus Printing Point",
                "University Avenue 10",
                "Warsaw",
                "00-002",
                "Poland",
                15
        )
    ));

    public PageResult<PrintingPointResponse> findAll(int page, int size) {
        int totalItems = printingPoints.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        int fromIndex = Math.min(page * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<PrintingPointResponse> items = List.copyOf(
            printingPoints.subList(fromIndex, toIndex)
        );

        return new PageResult<>(
            items,
            page,
            size,
            totalItems,
            totalPages
        );
    }

    public PrintingPointResponse findById(String printingPointId) {
        return printingPoints.stream()
            .filter(point -> hasId(point, printingPointId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Printing point not found: " + printingPointId
            ));
    }

    public Integer deleteById(String printingPointId) {
        PrintingPointResponse printingPoint = findById(printingPointId);
        printingPoints.remove(printingPoint);

        return printingPoint.printingPointId();
    }

    private boolean hasId(PrintingPointResponse point, String printingPointId) {
        return point.printingPointId().toString().equals(printingPointId);
    }
}