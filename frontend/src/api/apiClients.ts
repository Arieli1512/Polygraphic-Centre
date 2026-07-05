import {queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface Client {
    client_id: number;
    email: string;
    firebase_uid: string;
    first_name?: string;
    last_name?: string;
    status: 'ACTIVE' | 'BLOCKED';
    created_at?: string;
    updated_at?: string;
}

export function createGetClientQuery(id: number) {
    return queryOptions({
        queryKey: [`GET_CLIENT_${id}`],
        queryFn: (): Promise<Client>  => {
            return apiFetch(`/clients/${id}`);
        },
    });
}
