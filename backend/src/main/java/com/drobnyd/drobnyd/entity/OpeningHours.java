package com.drobnyd.drobnyd.entity;

import java.time.LocalTime;

import com.drobnyd.drobnyd.entity.id.OpeningHoursId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "opening_hours")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class OpeningHours {

    @EmbeddedId
    @ToString.Include
    private OpeningHoursId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("printingPointId")
    @JoinColumn(name = "printing_point_id", nullable = false)
    private PrintingPoint printingPoint;

    @Column(name = "start_time", nullable = false)
    @ToString.Include
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @ToString.Include
    private LocalTime endTime;
}