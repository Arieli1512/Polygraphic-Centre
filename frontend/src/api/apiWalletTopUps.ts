import {mutationOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface WalletTopUp {
    top_up_id?: number;
    client_id: number;
    amount: number;
    created_at?: string;
}

export function createWalletTopUpMutation () {
    return mutationOptions({
        mutationFn: (topUp: WalletTopUp): Promise<WalletTopUp>  => {
            return apiFetch(`/clients/${topUp.client_id}/wallet-top-ups`, {bodyData: topUp});
        },
        retry: false,
    });
}