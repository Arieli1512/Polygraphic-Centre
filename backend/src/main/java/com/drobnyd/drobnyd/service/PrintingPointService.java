package com.drobnyd.drobnyd.service;

import com.drobnyd.drobnyd.dto.PrintingPointResponse;
import com.drobnyd.drobnyd.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrintingPointService {
        private final List<PrintingPointResponse> printingPoints = List.of(
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
        );

      public List<PrintingPointResponse> findAll() {
        return printingPoints;
    }

     public PrintingPointResponse findById(String printingPointId) {
        return printingPoints.stream()
            .filter(point -> point.printingPointId().equals(printingPointId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Printing point not found: " + printingPointId
            ));
    }
}