import React, {useState} from 'react'
import styles from './OrdersTable.module.css'
import {
    createUpdateOrderStatusMutation,
    type Order,
    type OrderStatusUpdate
} from '../../api/apiOrders.ts'
import {createOrderDetailsQuery} from '../../api/apiPrintSettings.ts'
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {LimitShownPrintingPointsContext} from "../../tools/contexts/LimitShownPrintingPointsContext.ts";
import {PrintingPointsTable} from "./PrintingPointsTable.tsx";
import {MdArrowRight, MdDeleteForever} from "react-icons/md";
import { MdOutlineExpandMore } from "react-icons/md";
import {dateTimeFormatter} from "../../tools/datetime/dateTimeFormats.ts";
import {useNavigate} from "react-router-dom";
import {ImSpinner2} from "react-icons/im";
import type {UserRole} from "../../tools/types/auth.ts";

interface OrdersTableProps {
    role: UserRole,
    orders: Order[] | undefined,
    ordersQueryIsPending : boolean
}

export const OrdersTable = ({role, orders, ordersQueryIsPending}:OrdersTableProps) => {
    const navigate = useNavigate();
    const [orderDetailsId, setOrderDetailsId] = useState<number | null>(null);
    const {data: orderDetails, isPending: OrderDetailsQueryIsPending} = useQuery(createOrderDetailsQuery(orderDetailsId ?? -1));
    const queryClient = useQueryClient();
    const mutationUpdateOrderStatus = useMutation({...createUpdateOrderStatusMutation(),
        onSuccess: () => {
        queryClient.invalidateQueries({queryKey: [`orders`]});
    }});

    let dataSorted = null;
    if (orders) {
        dataSorted = orders;
        dataSorted.sort((a, b) => {
            if (a.status === b.status) return (new Date(a.pickup_at).getTime() - new Date(b.pickup_at).getTime());
            else if (a.status === 'CANCELLED') return 1;
            else if (b.status === 'CANCELLED') return -1;
            else return (new Date(a.pickup_at).getTime() - new Date(b.pickup_at).getTime());
        })
    }

    return (
        <main>
            <h1>Historia Zamówień</h1>
            <div className={styles.orderHistoryContainer}>
                <table className={styles.orderHistoryTable}>
                    <thead>
                        <tr>
                            <th>Status</th>
                            <th>Plik</th>
                            <th>Ilość stron</th>
                            <th>Koszt</th>
                            <th>Termin Odbioru</th>
                            <th>Punkt Druku</th>
                            <th>Pokaż Detale</th>
                            {(role === "KLIENT") && <th>Anuluj Zamówienie</th> }
                            {(role === "EMPLOYEE") && <th>Aktualizuj Status</th> }
                        </tr>
                    </thead>
                    {ordersQueryIsPending && <ImSpinner2 className="icon-spin"/>}
                    <tbody>
                        {dataSorted?.map((order) => {
                            let borderColor = "var(--accent)";
                            switch(order.status) {
                                case 'PENDING':
                                    borderColor = "var(--pending)";
                                    break;
                                case 'APPROVED':
                                    borderColor = "var(--approved)";
                                    break;
                                case 'READY':
                                    borderColor = "var(--ready)";
                                    break;
                                case 'DISPENSED':
                                    borderColor = "var(--dispensed)";
                                    break;
                                case 'CANCELLED':
                                    borderColor = "var(--cancelled)";
                                    break;
                            }
                            const borderStyle = {
                                '--borderColor': borderColor
                            } as React.CSSProperties;
                            return (
                                <React.Fragment key={order.order_id}>
                                    <tr style={borderStyle} className={styles.orderRow}>
                                        <td> {order.status} </td>
                                        <td> {order.file_path?.split(/[\\/]/).pop()} </td>
                                        <td className={styles.tdCentered}> {order.page_count} </td>
                                        <td className={styles.tdCentered}> {order.total_price.toFixed(2)}zł </td>
                                        <td className={styles.tdCentered}> {dateTimeFormatter.format(new Date(order.pickup_at))} </td>
                                        <td> <LimitShownPrintingPointsContext value={{id: order.printing_point_id, hideDetails: true, disabled: false}}>
                                                    <PrintingPointsTable/>
                                            </LimitShownPrintingPointsContext></td>
                                        <td className={styles.tdCentered}> <button aria-label={"Pokaż Detale Zamówienia"} disabled={OrderDetailsQueryIsPending} className={styles.orderHistoryButton} onClick={() => {
                                            if (orderDetailsId === order.order_id) setOrderDetailsId(null);
                                            else setOrderDetailsId(order.order_id ?? null);
                                        }}><MdOutlineExpandMore className={styles.orderHistoryIcons}/></button> </td>
                                        {(role === "KLIENT") && <td className={styles.tdCentered}> <button aria-label={"Anuluj Zamówienie"} disabled={mutationUpdateOrderStatus.isPending} className={styles.orderHistoryButton} onClick={() => {
                                            if (!order.order_id) return;
                                            const o: OrderStatusUpdate = {order_id: order.order_id, status: 'CANCELLED'};
                                            mutationUpdateOrderStatus.mutate(o);
                                        }}><MdDeleteForever className={styles.orderHistoryIcons}/></button> </td>}
                                        {(role === "EMPLOYEE") && <td className={styles.tdCentered}> <button aria-label={"Aktualizuj Status Zamówienia"} disabled={mutationUpdateOrderStatus.isPending} className={styles.orderHistoryButton} onClick={() => {
                                            if (!order.order_id) return;
                                            const o: OrderStatusUpdate = {order_id: order.order_id, status: 'CANCELLED'};
                                            mutationUpdateOrderStatus.mutate(o);
                                        }}><MdArrowRight className={styles.orderHistoryIcons}/></button> </td>}
                                    </tr>


                                    {OrderDetailsQueryIsPending && <ImSpinner2 className="icon-spin"/>}
                                    {(orderDetailsId === order.order_id && orderDetails &&
                                        <tr className={styles.detailsRow}>
                                            <td colSpan={8}>
                                                <div className={styles.detailsRowDiv}>
                                                    <span className={styles.orderDetailsSpan}>Format {orderDetails.format}</span>
                                                    <span className={styles.orderDetailsSpan}>Papier {orderDetails.paper_type}</span>
                                                    <span className={styles.orderDetailsSpan}>{orderDetails.copies} {orderDetails.copies === 1 ? "kopia" : orderDetails.copies === 2 ? "kopie" : "kopii"}</span>
                                                    <span className={styles.orderDetailsSpan}>{orderDetails.color_mode === 'COLOR' ? "W kolorze" : orderDetails.color_mode === 'GRAYSCALE' ? "Czarno-białe" : "Brak danych"}</span>
                                                    <span className={styles.orderDetailsSpan}>{orderDetails.duplex === 'SINGLE_SIDED' ? "Jednostronnie" : orderDetails.duplex === 'DOUBLE_SIDED' ? "Dwustronnie" : "Brak danych"}</span>
                                                    <span className={styles.orderDetailsSpan}>{orderDetails.orientation === 'PORTRAIT' ? "Pionowo" : orderDetails.orientation === 'LANDSCAPE' ? "Poziomo" : "Brak danych"}</span>
                                                    <span className={styles.orderDetailsSpan}>{orderDetails.finishing === 'NONE' ? "" : orderDetails.finishing === 'BINDING' ? "Z oprawianiem" : orderDetails.finishing === 'STAPLING' ? "Ze zszywaniem" : orderDetails.finishing === 'COVER' ? "Z okładką" : "Brak danych"}</span>
                                                    {(role === "KLIENT") && <span className={styles.orderDetailsSpan}><button className={styles.orderEditButton} onClick={() => {
                                                        navigate(`/order`, {state: {editingOrder: order, editingOrderDetails: orderDetails}});
                                                    }}>Edytuj</button></span>}
                                                </div>
                                            </td>
                                        </tr>
                                        )}

                                    <tr className={styles.orderHistoryTableSpacer}>
                                        <td colSpan={8}/>
                                    </tr>
                                </React.Fragment>
                            )
                        }

                        )}
                    </tbody>
                </table>
            </div>
        </main>
    )
}