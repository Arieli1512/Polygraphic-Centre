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
        queryKey: [`GET_USER_ORDERS_${client_id}`],
        queryFn: (): Promise<Order[]> => {
            return apiFetch(`/clients/${client_id}/orders`);
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