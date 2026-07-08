import styles from './ManagementTables.module.css';
import {forwardRef, useImperativeHandle, useState} from "react";
import {createEditOpeningHoursMutation, type OpeningHours} from "../../api/apiOpeningHours.ts";
import {useMutation} from "@tanstack/react-query";

export const OpeningHoursTable = forwardRef<{submitOpeningHours: () => Promise<OpeningHours[]>}, {data: OpeningHours[] | undefined}>(({ data }, ref) => {

    const [openingHours, setOpeningHours] = useState<OpeningHours[]>(data ?? []);
    const weekdays = ['pon.','wt.','śr.','czw.','pi.','sob.','nd'];

    const mutationEditOpeningHours = useMutation(createEditOpeningHoursMutation());

    useImperativeHandle(ref, () => ({
        submitOpeningHours: async () => {
            return await mutationEditOpeningHours.mutateAsync(openingHours);
        }
    }));




    return (
        <table className={styles.managementTable}>
            <thead>
                <tr>
                    <th scope="col"></th>
                    <th scope="col">Od</th>
                    <th scope="col">Do</th>
                </tr>
            </thead>
            <tbody>
            {openingHours.map(openingHoursSet => (
                <tr key={openingHoursSet.day_of_week}>
                    <th scope="row">
                        {weekdays[openingHoursSet.day_of_week-1]}
                    </th>

                    <td>
                        <input type="time" defaultValue={openingHoursSet.start_time} onChange={(e) => {
                            setOpeningHours(prev =>
                                prev.map(item =>
                                    item.day_of_week === openingHoursSet.day_of_week
                                        ? { ...item, start_time: e.target.value }
                                        : item
                                )
                            );
                        }}/>
                    </td>
                    <td>
                        <input type="time" defaultValue={openingHoursSet.end_time} onChange={(e) => {
                            setOpeningHours(prev =>
                                prev.map(item =>
                                    item.day_of_week === openingHoursSet.day_of_week
                                        ? { ...item, end_time: e.target.value }
                                        : item
                                )
                            );
                        }}/>
                    </td>

                </tr>
            ))}
            </tbody>
        </table>
    );
});


