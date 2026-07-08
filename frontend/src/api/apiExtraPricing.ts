//import {queryOptions} from "@tanstack/react-query";
//import {apiFetch} from "./apiFetch.ts";

import {mutationOptions, queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface ExtraPricingSetting {
    printing_point_id?: number;
    binding_price?: number;
    stapling_price?: number;
    cover_price?: number;
}

export function createEditExtraPricingMutation() {
    return mutationOptions({
        mutationFn: (o: ExtraPricingSetting): Promise<ExtraPricingSetting>  => {
            return apiFetch(`/printing-points/${o.printing_point_id}/extra_pricing`, {bodyData: o});
        },
        retry: false,
    });
}


export function createGetPrintingPointExtraPricingQuery(id: number | undefined) {
        return queryOptions({
            queryKey: [`printingPoint${id}`, 'extraPricing'],
            queryFn: async (): Promise<ExtraPricingSetting> => {
                if (!id) id = 1;
                try {
                    return await apiFetch(`/printing-points/${id}/extra-pricing`);
                } catch (e) {
                    console.error(`[API Error] Failed to fetch extra pricing information for printing point ${id}, defaulting to mock data.`, e);
                    return {printing_point_id: id, binding_price: 10, stapling_price: 5.5, cover_price: 200};
                }
            },
        });
}