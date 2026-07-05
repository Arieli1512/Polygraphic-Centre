//import {queryOptions} from "@tanstack/react-query";
//import {apiFetch} from "./apiFetch.ts";

export interface OpeningHours {
    printing_point_id: number;
    day_of_week: number;
    start_time: string;
    end_time: string;
}