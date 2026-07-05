//import {queryOptions} from "@tanstack/react-query";
//import {apiFetch} from "./apiFetch.ts";

import {queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface ExtraPricingSetting {
    pricing_point_id: number;
    binding_price?: number;
    stapling_price?: number;
    cover_price?: number;
}

export function createGetPrintingPointExtraPricingQuery(id: number) {
    return queryOptions({
        queryKey: [`GET_PRINTING_POINT_EXTRA_PRICING_${id}`],
        queryFn: (): Promise<ExtraPricingSetting> => {
            return apiFetch(`/printing-points/${id}/extra-pricing`);
        },
    });
}