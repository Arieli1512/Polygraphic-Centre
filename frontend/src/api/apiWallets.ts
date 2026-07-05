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
        queryKey: [`GET_USER_WALLET_${client_id}`],
        queryFn: (): Promise<Wallet> => {
            return apiFetch(`/clients/${client_id}/wallet`);
        },
    });
}