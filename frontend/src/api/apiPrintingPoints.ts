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
        queryKey: ['GET_ALL_PRINTING_POINTS'],
        queryFn: async (): Promise<PrintingPoint[]> => {
            const response =  await apiFetch<ApiPageResponse<PrintingPoint>>(`/printing-points`);
            return response.data;
        },
    });
}

export function createGetPrintingPointQuery(id: number) {
    return queryOptions({
        queryKey: [`GET_PRINTING_POINT_${id}`],
        queryFn: async (): Promise<PrintingPoint> => {
            const response =  await apiFetch<ApiPageResponse<PrintingPoint>>(`/printing-points/${id}`);
            return response.data[0];
        },
    });
}
