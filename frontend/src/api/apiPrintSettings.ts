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
        queryKey: ['orderDetails', `order${order_id}`],
        queryFn: async (): Promise<PrintSettings> => {
            try {
                return await apiFetch(`/orders/${order_id}/print-settings`);
            } catch (e) {
                console.error(`[API Error] Failed to fetch order details for order ${order_id}, defaulting to mock data.`, e);
                return {order_id: 1, format: "A4", paper_type: "standardowy", color_mode: "COLOR", duplex: "DOUBLE_SIDED", orientation: "PORTRAIT", finishing: "BINDING", copies: 2};
            }
        },
    });
}