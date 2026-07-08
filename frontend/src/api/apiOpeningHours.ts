import {mutationOptions, queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface OpeningHours {
    printing_point_id: number;
    day_of_week: number;
    start_time: string;
    end_time: string;
}

export function createEditOpeningHoursMutation() {
    return mutationOptions({
        mutationFn: (o: OpeningHours[]): Promise<OpeningHours[]>  => {
            return apiFetch(`/printing-points/${o[0].printing_point_id}/opening-hours`, {bodyData: o});
        },
        retry: false,
    });
}

export function createGetPrintingPointOpeningHoursQuery(id: number | undefined) {
    return queryOptions({
        queryKey: [`printingPoint${id}`, 'openingHours'],
        queryFn: async (): Promise<OpeningHours[]> => {
            if (!id) id = 1;
            try {
                return await apiFetch(`/printing-points/${id}/opening-hours`);
            } catch (e) {
                console.error(`[API Error] Failed to fetch opening hours for printing point ${id}, defaulting to mock data.`, e);
                return [
                    {printing_point_id: id, day_of_week: 1, start_time: "08:00", end_time: "18:00"},
                    {printing_point_id: id, day_of_week: 2, start_time: "08:00", end_time: "18:00"},
                    {printing_point_id: id, day_of_week: 3, start_time: "08:00", end_time: "18:00"},
                    {printing_point_id: id, day_of_week: 4, start_time: "08:00", end_time: "18:00"},
                    {printing_point_id: id, day_of_week: 5, start_time: "08:00", end_time: "18:00"},
                    {printing_point_id: id, day_of_week: 6, start_time: "12:00", end_time: "18:00"},
                    {printing_point_id: id, day_of_week: 7, start_time: "12:00", end_time: "18:00"}
                ];
            }
        },
    });
}