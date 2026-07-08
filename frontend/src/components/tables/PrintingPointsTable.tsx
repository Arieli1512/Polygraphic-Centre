import React from 'react'
import styles from './PrintingPointsTable.module.css'
import {createGetAllPrintingPointsQuery} from '../../api/apiPrintingPoints.ts'
import {useQuery} from "@tanstack/react-query";
import { use } from "react";
import {LimitShownPrintingPointsContext} from "../../tools/contexts/LimitShownPrintingPointsContext.ts"
import {useNavigate} from "react-router-dom";
import {ImSpinner2} from "react-icons/im";

export const PrintingPointsTable: React.FC = () => {
    const navigate = useNavigate();
    const {id, hideDetails, disabled} = use(LimitShownPrintingPointsContext);
    const {data, isPending} = useQuery(createGetAllPrintingPointsQuery());

    return (
        <main>
            <section className={styles.printingPointsSection}>
                {!id && <h3>Nasze punkty druku</h3>}
                {isPending && <ImSpinner2 className="icon-spin"/>}
                <div>
                    {data?.map((pp) => {
                        if(!id || id == pp.printingPointId) {
                            return (
                                <div key={pp.printingPointId} data-id={pp.printingPointId} className={`${styles.printingPointDiv} printing-point-div-marker`} onClick={() => {
                                    if(!disabled) navigate(`/printingpoints/${pp.printingPointId}`);
                                }}>
                                    <h4 className={styles.printingPointTitle}>{pp.name}</h4>
                                    {(!hideDetails &&
                                    <p>
                                        {pp.city} - {pp.streetAddress}
                                    </p>
                                    )}
                                </div>
                            )
                        }
                    }

                    )}
                </div>
            </section>
        </main>
    )
}