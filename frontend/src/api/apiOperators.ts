//import {queryOptions} from "@tanstack/react-query";
//import {apiFetch} from "./apiFetch.ts";

export interface Operator {
    operator_id: number;
    printing_point_id: number;
    firebase_uid: string;
    email: string;
    employee_number: string;
    role: 'ADMIN' | 'EMPLOYEE';
    status: 'ACTIVE' | 'BLOCKED';
    created_at?: string;
    updated_at?: string;
}