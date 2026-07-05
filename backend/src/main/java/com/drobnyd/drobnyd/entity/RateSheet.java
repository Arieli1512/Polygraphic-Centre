package com.drobnyd.drobnyd.entity;

import com.drobnyd.drobnyd.entity.id.RateSheetId;

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
@Table(name = "rate_sheets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class RateSheet {

    public static RateSheet of(PrintingPoint printingPoint, String paperType, String format, Long pagePrice) {
        RateSheet rateSheet = new RateSheet();
        rateSheet.setPrintingPoint(printingPoint);
        rateSheet.setId(new com.drobnyd.drobnyd.entity.id.RateSheetId(
                printingPoint.getPrintingPointId(),
                paperType,
                format));
        rateSheet.setPagePrice(pagePrice);
        return rateSheet;
    }

    @EmbeddedId
    @ToString.Include
    private RateSheetId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("printingPointId")
    @JoinColumn(name = "printing_point_id", nullable = false)
    private PrintingPoint printingPoint;

    @Column(name = "page_price", nullable = false)
    @ToString.Include
    private Long pagePrice;
}