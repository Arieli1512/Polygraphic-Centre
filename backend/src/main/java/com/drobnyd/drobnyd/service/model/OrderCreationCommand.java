package com.drobnyd.drobnyd.service.model;

import java.time.OffsetDateTime;

import com.drobnyd.drobnyd.entity.PrintColorMode;
import com.drobnyd.drobnyd.entity.PrintDuplex;
import com.drobnyd.drobnyd.entity.PrintFinishing;
import com.drobnyd.drobnyd.entity.PrintOrientation;

public record OrderCreationCommand(
        Integer clientId,
        Integer printingPointId,
        String filePath,
        Integer pageCount,
        OffsetDateTime pickupAt,
        String format,
        String paperType,
        PrintColorMode colorMode,
        PrintDuplex duplex,
        PrintOrientation orientation,
        PrintFinishing finishing,
        Integer copies) {
}