package com.drobnyd.drobnyd.service.model;

import java.time.LocalTime;

public record OpeningHoursWindow(
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {
}