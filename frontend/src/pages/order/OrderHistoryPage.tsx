import React, {useState} from 'react'
import styles from './OrderHistoryPage.module.css'
import {createUpdateOrderStatusMutation, createUserOrdersQuery, type OrderStatusUpdate} from '../../api/apiOrders.ts'
import {createOrderDetailsQuery} from '../../api/apiPrintSettings.ts'
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {LimitShownPrintingPointsContext} from "../../tools/contexts/LimitShownPrintingPointsContext.ts";
import {PrintingPointsTable} from "../../components/tables/PrintingPointsTable.tsx";
import { MdDeleteForever } from "react-icons/md";
import { MdOutlineExpandMore } from "react-icons/md";
import {dateTimeFormatter} from "../../tools/datetime/dateTimeFormats.ts";
import {useNavigate} from "react-router-dom";

export const OrderHistoryPage: React.FC = () => {
    const navigate = useNavigate();
    const { user,  } = useAuth();
    const client_id = user.client_id;
    const {data} = useQuery(createUserOrdersQuery(client_id ?? -1));
    const [orderDetailsId, setOrderDetailsId] = useState<number | null>(null);
    const {data: orderDetails} = useQuery(createOrderDetailsQuery(orderDetailsId ?? -1));
    const queryClient = useQueryClient();
    const mutationUpdateOrderStatus = useMutation({...createUpdateOrderStatusMutation(),
        onSuccess: () => {
        queryClient.invalidateQueries({queryKey: [`GET_USER_ORDERS_${client_id}`]})
    }});

    let dataSorted = null;
    if (data) {
        dataSorted = data;
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
                            <th>Anuluj Zamówienie</th>
                        </tr>
                    </thead>
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
                                        <td className={styles.tdCentered}> {order.total_price}zł </td>
                                        <td className={styles.tdCentered}> {dateTimeFormatter.format(new Date(order.pickup_at))} </td>
                                        <td> <LimitShownPrintingPointsContext value={{id: order.printing_point_id, hideDetails: true}}>
                                                    <PrintingPointsTable/>
                                            </LimitShownPrintingPointsContext></td>
                                        <td className={styles.tdCentered}> <button aria-label={"Pokaż Detale Zamówienia"}  className={styles.orderHistoryButton} onClick={() => {
                                            if (orderDetailsId === order.order_id) setOrderDetailsId(null);
                                            else setOrderDetailsId(order.order_id ?? null);
                                        }}><MdOutlineExpandMore className={styles.orderHistoryIcons}/></button> </td>
                                        <td className={styles.tdCentered}> <button aria-label={"Anuluj Zamówienie"}  className={styles.orderHistoryButton} onClick={() => {
                                            if (!order.order_id) return;
                                            const o: OrderStatusUpdate = {order_id: order.order_id, status: 'CANCELLED'};
                                            mutationUpdateOrderStatus.mutate(o);
                                        }}><MdDeleteForever className={styles.orderHistoryIcons}/></button> </td>
                                    </tr>

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
                                                    <span className={styles.orderDetailsSpan}><button className={styles.orderEditButton} onClick={() => {
                                                        navigate(`/order`, {state: {editingOrder: order, editingOrderDetails: orderDetails}});
                                                    }}>Edytuj</button></span>
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