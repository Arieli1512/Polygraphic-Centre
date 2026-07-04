package com.drobnyd.drobnyd.service.model;

import com.drobnyd.drobnyd.entity.PrintColorMode;
import com.drobnyd.drobnyd.entity.PrintDuplex;
import com.drobnyd.drobnyd.entity.PrintFinishing;
import com.drobnyd.drobnyd.entity.PrintOrientation;

public record PricingRequest(
        Integer printingPointId,
        String format,
        String paperType,
        PrintColorMode colorMode,
        PrintDuplex duplex,
        PrintOrientation orientation,
        PrintFinishing finishing,
        Integer copies,
        Integer pageCount) {
}