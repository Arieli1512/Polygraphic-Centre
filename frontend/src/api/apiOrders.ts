import {mutationOptions, queryOptions} from "@tanstack/react-query";
import {apiFetch} from "./apiFetch.ts";

export interface Order {
    order_id?: number;
    printing_point_id: number;
    client_id: number;
    file_path?: string;
    page_count: number;
    total_price: number;
    pickup_at: string;
    status: 'PENDING' | 'APPROVED' | 'READY' | 'DISPENSED' | 'CANCELLED';
    created_at?: string;
    updated_at?: string;

    //file: FormData;
}

export interface OrderStatusUpdate {
    order_id: number;
    status: 'PENDING' | 'APPROVED' | 'READY' | 'DISPENSED' | 'CANCELLED';
}

export function createNewOrderMutation() {
    return mutationOptions({
        mutationFn: (o: FormData): Promise<Order>  => {
            return apiFetch(`/orders`, {bodyData: o});
        },
        retry: false,
    });
}

export function createUserOrdersQuery(client_id: number) {
        return queryOptions({
            queryKey: ['orders', `client${client_id}`],
            queryFn: async (): Promise<Order[]> => {
                try {
                    return await apiFetch(`/clients/${client_id}/orders`);
                } catch (e) {
                    console.error(`[API Error] Failed to fetch orders for client ${client_id}, defaulting to mock data.`, e);
                    return [
                        {order_id: 1, printing_point_id: 1, client_id: client_id, file_path: "api/v1/orders/1/text.pdf", page_count: 1, total_price: 300, pickup_at: "2026-07-07T12:00", status: 'PENDING', created_at: "2026-07-07T12:00", updated_at: "2026-07-07T12:00"},
                        {order_id: 2, printing_point_id: 1, client_id: client_id, file_path: "api/v1/orders/2/testowy.docx", page_count: 3, total_price: 500, pickup_at: "2026-07-07T12:00", status: 'READY', created_at: "2026-07-07T12:00", updated_at: "2026-07-07T12:00"}
                    ];
                }
            },
        });
}

export function createOrderQueueQuery(printing_point_id: number | undefined) {
    return queryOptions({
        queryKey: ['orders', `printingPoint${printing_point_id}`],
        queryFn: async (): Promise<Order[]> => {
            try {
                return await apiFetch(`/printing_points/${printing_point_id}/orders`);
            } catch (e) {
                console.error(`[API Error] Failed to fetch orders for client ${printing_point_id}, defaulting to mock data.`, e);
                return [
                    {order_id: 1, printing_point_id: printing_point_id ?? 1, client_id: 2, file_path: "api/v1/orders/1/text.pdf", page_count: 1, total_price: 300, pickup_at: "2026-07-07T12:00", status: 'PENDING', created_at: "2026-07-07T12:00", updated_at: "2026-07-07T12:00"},
                    {order_id: 2, printing_point_id: printing_point_id ?? 1, client_id: 1, file_path: "api/v1/orders/2/testowy.docx", page_count: 3, total_price: 500, pickup_at: "2026-07-07T12:00", status: 'READY', created_at: "2026-07-07T12:00", updated_at: "2026-07-07T12:00"}
                ];
            }
        },
    });
}

export function createUpdateOrderStatusMutation () {
    return mutationOptions({
        mutationFn: (o: OrderStatusUpdate): Promise<Order>  => {
            return apiFetch(`/orders/${o.order_id}/status`, {bodyData: o, method: "PATCH"});
        },
        retry: false,
    });
}