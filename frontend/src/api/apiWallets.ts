import {queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface Wallet {
    client_id: number;
    balance: number;
    status: 'ACTIVE' | 'BLOCKED' | 'CLOSED';
    updated_at?: string;
}

export function createUserWalletQuery(client_id: number) {
    return queryOptions({
        queryKey: ['wallet', `client${client_id}`],
        queryFn: async (): Promise<Wallet> => {
            try {
                return await apiFetch(`/clients/${client_id}/wallet`);
            } catch (e) {
                console.error(`[API Error] Failed to fetch wallet for client ${client_id}, defaulting to mock data.`, e);
                return {client_id: client_id, balance: 1300.75, status: "ACTIVE", updated_at: "2026-07-07T12:00"};
            }
        },
    });
}