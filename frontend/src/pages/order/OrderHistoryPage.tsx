import React from 'react'
import {OrdersTable} from "../../components/tables/OrdersTable.tsx";
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {useQuery} from "@tanstack/react-query";
import {createUserOrdersQuery} from "../../api/apiOrders.ts";

export const OrderHistoryPage: React.FC = () => {
    const { user,  } = useAuth();
    const client_id = user.client_id;
    const {data, isPending: OrdersQueryIsPending} = useQuery(createUserOrdersQuery(client_id ?? -1));

    return (
        <div>

            <main>
                <OrdersTable role="KLIENT" orders={data} ordersQueryIsPending={OrdersQueryIsPending}/>
            </main>

        </div>
    )
}