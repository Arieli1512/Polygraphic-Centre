import {queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";
import type {ApiPageResponse} from "../tools/types/api.ts";

export interface PrintingPoint {
    printingPointId: number;
    name: string;
    streetAddress: string;
    city: string;
    postalCode: string;
    country: string;
    hourlyOrderLimit?: number;
}

export function createGetAllPrintingPointsQuery() {
    return queryOptions({
        queryKey: ['printingPoints'],
        queryFn: async (): Promise<PrintingPoint[]> => {
            try {
                const response = await apiFetch<ApiPageResponse<PrintingPoint>>(`/printing-points`);
                return response.data;
            } catch (e) {
                console.error(`[API Error] Failed to fetch printing points, defaulting to mock data.`, e);
                return [
                    {printingPointId: 1, name: "Centrum", streetAddress: "Testowa 12", city: "Warszawa", postalCode: "02-321", country: "Polska", hourlyOrderLimit: 5},
                    {printingPointId: 2, name: "Drobniejczyk", streetAddress: "Drobna 1", city: "Łódź", postalCode: "01-001", country: "Polska", hourlyOrderLimit: undefined}
                ];
            }
        },
    });
}

/*
export function createGetPrintingPointQuery(id: number) {
    return queryOptions({
        queryKey: [`GET_PRINTING_POINT_${id}`],
        queryFn: async (): Promise<PrintingPoint> => {
            const response =  await apiFetch<ApiPageResponse<PrintingPoint>>(`/printing-points/${id}`);
            return response.data[0];
        },
    });
}
 */
