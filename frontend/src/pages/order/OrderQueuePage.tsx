import React from 'react'
import {OrdersTable} from "../../components/tables/OrdersTable.tsx";
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {useQuery} from "@tanstack/react-query";
import {createOperatorPrintingPointQuery} from "../../api/apiOperators.ts";
import {createOrderQueueQuery} from "../../api/apiOrders.ts";

export const OrderQueuePage: React.FC = () => {
    const { user,  } = useAuth();
    const operator_id = user.client_id;
    const {data: printingPointId, isPending: EmployeePrintingPointQueryIsPending} = useQuery(createOperatorPrintingPointQuery(operator_id ?? -1));
    const {data, isPending: OrdersQueryIsPending} = useQuery(createOrderQueueQuery(printingPointId));

    return (
        <div>

            <main>
                <OrdersTable role="EMPLOYEE" orders={data} ordersQueryIsPending={EmployeePrintingPointQueryIsPending || OrdersQueryIsPending}/>
            </main>

        </div>
    )
}