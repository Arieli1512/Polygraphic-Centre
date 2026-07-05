//import {queryOptions} from "@tanstack/react-query";
//import {apiFetch} from "./apiFetch.ts";

import {queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface RateSheet {
    printing_point_id: number;
    paper_type: string;
    format: string;
    page_price: number;
}

export function createGetPrintingPointRateSheetQuery(id: number) {
    return queryOptions({
        queryKey: [`GET_PRINTING_POINT_RATE_SHEET_${id}`],
        queryFn: (): Promise<RateSheet[]> => {
            return apiFetch(`/printing-points/${id}/rate-sheets`);
        },
    });
}