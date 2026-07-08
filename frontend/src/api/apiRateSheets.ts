import {mutationOptions, queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface RateSheet {
    printing_point_id: number;
    paper_type: string;
    format: string;
    page_price: number;
}

export function createEditRateSheetMutation() {
    return mutationOptions({
        mutationFn: (rs: RateSheet[]): Promise<RateSheet[]>  => {
            return apiFetch(`/printing-points/${rs[0].printing_point_id}/opening-hours`, {bodyData: rs});
        },
        retry: false,
    });
}

export function createGetPrintingPointRateSheetQuery(id: number | undefined) {
    return queryOptions({
        queryKey: ['rateSheet', `printingPoint${id}`],
        queryFn: async (): Promise<RateSheet[]> => {
            if (!id) id = 1;
            try {
                return await apiFetch(`/printing-points/${id}/rate-sheets`);
            } catch (e) {
                console.error(`[API Error] Failed to fetch rate sheet for printing point ${id}, defaulting to mock data.`, e);
                return [
                    {printing_point_id: id, paper_type: "standardowy", format: "A4", page_price: 60},
                    {printing_point_id: id, paper_type: "standardowy", format: "A3", page_price: 100},
                    {printing_point_id: id, paper_type: "standardowy", format: "A5", page_price: 35},
                    {printing_point_id: id, paper_type: "kredowy", format: "A4", page_price: 70},
                    {printing_point_id: id, paper_type: "kredowy", format: "A3", page_price: 110},
                    {printing_point_id: id, paper_type: "błyszczący", format: "A4", page_price: 75},
                    {printing_point_id: id, paper_type: "błyszczący", format: "A3", page_price: 130},
                    {printing_point_id: id, paper_type: "matowy", format: "A4", page_price: 80}
                ];
            }
        },
    });
}