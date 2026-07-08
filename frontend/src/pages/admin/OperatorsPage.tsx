import React from "react";
import {useQuery} from "@tanstack/react-query";
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {createGetPrintingPointOperatorsQuery, createOperatorPrintingPointQuery} from "../../api/apiOperators.ts";
import styles from "./OperatorsPage.module.css";
import {ImSpinner2} from "react-icons/im";

export const OperatorsPage: React.FC  = () => {
    const { user,  } = useAuth();
    const operator_id = user.client_id;
    const {data: printingPointId, isPending: EmployeePrintingPointQueryIsPending} = useQuery(createOperatorPrintingPointQuery(operator_id ?? -1));
    const {data: operatorsData, isPending: OperatorsQueryIsPending} = useQuery(createGetPrintingPointOperatorsQuery(printingPointId));

    /*
    const queryClient = useQueryClient();
    const mutationUpdateOperators = useMutation({...createUpdateOperatorsMutation(),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: [`operators`]});
        }});*/

    return (
        <main>
            {(EmployeePrintingPointQueryIsPending || OperatorsQueryIsPending) ? <ImSpinner2 className="icon-spin"/> :
                <table className={styles.operatorsTable}>
                    <thead>
                    <tr>
                        <th>Identyfikator</th>
                        <th>Email</th>
                        <th>Rola</th>
                        <th>Status</th>
                    </tr>
                    </thead>
                {operatorsData?.map((operator) => {
                    let borderColor = "var(--accent-bg)";
                    switch(operator.status) {
                        case 'ACTIVE':
                            borderColor = "var(--active)";
                            break;
                        case 'BLOCKED':
                            borderColor = "var(--blocked)";
                            break;
                    }
                    const borderStyle = {
                        '--borderColor': borderColor
                    } as React.CSSProperties;
                    return (<>
                        <tr style={borderStyle} className={styles.operatorRow}>
                            <td>
                                {operator.employee_number}
                            </td>
                            <td>
                                {operator.email}
                            </td>
                            <td>
                                {operator.role}
                            </td>
                            <td>
                                {operator.status}
                            </td>

                        </tr>

                        <tr className={styles.operatorsTableSpacer}></tr>
                    </>
                    )})}
                </table>}
        </main>
    )
}