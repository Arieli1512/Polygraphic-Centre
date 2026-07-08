import styles from './ManagementTables.module.css';
import {forwardRef, useImperativeHandle, useState} from "react";
import {createEditRateSheetMutation, type RateSheet} from "../../api/apiRateSheets.ts";
import {useMutation} from "@tanstack/react-query";

export const RateSheetTable = forwardRef<{submitRateSheet: () => Promise<RateSheet[]>}, {data: RateSheet[] | undefined}>(({ data }, ref) => {

    const [rateSheet, setRateSheet] = useState<RateSheet[]>(data ?? []);

    const mutationEditRateSheet = useMutation(createEditRateSheetMutation());

    useImperativeHandle(ref, () => ({
        submitRateSheet: async () => {
            return await mutationEditRateSheet.mutateAsync(rateSheet);
        }
    }));


    const basePaperTypes = ['standardowy', 'błyszczący', 'matowy', 'kredowy'];
    const uniquePaperTypes = [...new Set(rateSheet?.map(sheet => sheet.paper_type))];
    const [paperTypes, ] = useState( [... new Set([...basePaperTypes, ...uniquePaperTypes])] );

    const baseFormats = ['A3', 'A4', 'A5', 'A6'];
    const uniqueFormats = [...new Set(rateSheet?.map(sheet => sheet.format))];
    const [formats, ] = useState( [... new Set([...baseFormats, ...uniqueFormats])] );

    return (
        <table className={styles.managementTable}>
            <thead>
                <tr>
                    <th></th>
                    {paperTypes.map(paperType=>
                        <th key={paperType} scope="col">{paperType}</th>
                    )}
                </tr>
            </thead>
            <tbody>
            {formats.map(format => (
                <tr key={format}>
                    <th scope="row">
                        {format}
                    </th>

                    {paperTypes.map(paperType => {
                        // Find the rate that matches BOTH the format and the paper type
                        const matchingRate = rateSheet?.find(
                            rate => rate.format === format && rate.paper_type === paperType
                        );

                        return (
                            // Use a combined string for the key since it's an intersection
                            <td key={`${format}-${paperType}`}>
                                <input type="number" step="0.01" min="0" max="99999" defaultValue={matchingRate ? matchingRate.page_price : undefined} onChange={(e) => {
                                    setRateSheet(prev =>
                                        prev.map(item =>
                                            (item.format === format && item.paper_type === paperType)
                                                ? { ...item, page_price: parseFloat(e.target.value) }
                                                : item
                                        )
                                    );
                                }}/>
                            </td>
                        );
                    })}
                </tr>
            ))}
            </tbody>
        </table>
    );
});


