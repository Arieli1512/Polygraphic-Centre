import styles from './ManagementTables.module.css';
import {forwardRef, useImperativeHandle, useState} from "react";
import {createEditExtraPricingMutation, type ExtraPricingSetting} from "../../api/apiExtraPricing.ts";
import {useMutation} from "@tanstack/react-query";

export const ExtraPricingTable = forwardRef<{submitExtraPricing: () => Promise<ExtraPricingSetting | undefined>}, {data: ExtraPricingSetting | undefined}>(({ data }, ref) => {

    const [extraPricing, setExtraPricing ] = useState<ExtraPricingSetting | undefined>(data);

    const mutationEditExtraPricing = useMutation(createEditExtraPricingMutation());

    useImperativeHandle(ref, () => ({
        submitExtraPricing: async () => {
            if (extraPricing) return await mutationEditExtraPricing.mutateAsync(extraPricing);
        }
    }));


    return (
        <table className={styles.managementTable}>
            <tbody>
                <tr>
                    <th scope="row"> Oprawianie </th>
                    <td>
                        <input type="number" step="0.01" min="0" max="99999"  defaultValue={extraPricing ? extraPricing.binding_price : undefined} onChange={(e) => {
                            setExtraPricing(prev => ({
                                ...prev,
                                binding_price: parseFloat(e.target.value)
                            }));
                        }}/>
                    </td>
                </tr>
                <tr>
                    <th scope="row"> Zszywanie </th>
                    <td>
                        <input type="number" step="0.01" min="0" max="99999" defaultValue={extraPricing ? extraPricing.stapling_price : undefined} onChange={(e) => {
                            setExtraPricing(prev => ({
                                ...prev,
                                stapling_price: parseFloat(e.target.value)
                            }));
                        }}/>
                    </td>
                </tr>
                <tr>
                    <th scope="row"> Okładka </th>
                    <td>
                        <input type="number" step="0.01" min="0" max="99999" defaultValue={extraPricing ? extraPricing.cover_price : undefined} onChange={(e) => {
                            setExtraPricing(prev => ({
                                ...prev,
                                cover_price: parseFloat(e.target.value)
                            }));
                        }}/>
                    </td>
                </tr>
            </tbody>
        </table>
    );
});