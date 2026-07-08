import {queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";
import type {ApiPageResponse} from "../tools/types/api.ts";

export interface Operator {
    operator_id: number;
    printing_point_id: number;
    firebase_uid?: string;
    email: string;
    employee_number: string;
    role: 'ADMIN' | 'EMPLOYEE';
    status: 'ACTIVE' | 'BLOCKED';
    created_at?: string;
    updated_at?: string;
}

export function createOperatorPrintingPointQuery(operator_id: number) {
    return queryOptions({
        queryKey: ['operators', `operator${operator_id}`],
        queryFn: async (): Promise<number> => {
            try {
                const response = await apiFetch<ApiPageResponse<number>>(`/operators/${operator_id}/printing_point_id`);
                return response.data[0];
            } catch (e) {
                console.error(`[API Error] Failed to fetch operator printing point, defaulting to mock data.`, e);
                return 1;
            }
        },
    });
}

export function createGetPrintingPointOperatorsQuery(printing_point_id: number | undefined) {
    return queryOptions({
        queryKey: ['operators', `printingPoint${printing_point_id}`],
        queryFn: async (): Promise<Operator[]> => {
            if (!printing_point_id) return [];
            try {
                const response = await apiFetch<ApiPageResponse<Operator>>(`/printing-points`);
                return response.data;
            } catch (e) {
                console.error(`[API Error] Failed to fetch printing point operators, defaulting to mock data.`, e);
                return [
                    {operator_id: 1, printing_point_id: printing_point_id, email: "testowy@test.com", employee_number: "TS001", role: "ADMIN", status: "ACTIVE"},
                    {operator_id: 2, printing_point_id: printing_point_id, email: "test@testmail.com", employee_number: "TS003", role: "EMPLOYEE", status: "ACTIVE"}
                ];
            }
        },
    });
}