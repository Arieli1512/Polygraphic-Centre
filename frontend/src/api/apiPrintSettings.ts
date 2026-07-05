import {queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface PrintSettings {
    order_id?: number;
    format: string;
    paper_type: string;
    color_mode: 'COLOR' | 'GRAYSCALE';
    duplex: 'SINGLE_SIDED' | 'DOUBLE_SIDED';
    orientation: 'PORTRAIT' | 'LANDSCAPE';
    finishing: 'NONE' | 'BINDING' | 'STAPLING' | 'COVER';
    copies: number;
}

export function createOrderDetailsQuery(order_id: number) {
    return queryOptions({
        queryKey: [`GET_ORDER_DETAILS_${order_id}`],
        queryFn: (): Promise<PrintSettings> => {
            return apiFetch(`/orders/${order_id}/print-settings`);
        },
    });
}