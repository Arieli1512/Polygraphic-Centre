package com.drobnyd.drobnyd.entity.id;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OpeningHoursId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer printingPointId;
    private Integer dayOfWeek;
}