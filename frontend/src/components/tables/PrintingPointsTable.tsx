import React from 'react'
import styles from './PrintingPointsTable.module.css'
import {createGetAllPrintingPointsQuery} from '../../api/apiPrintingPoints.ts'
import {useQuery} from "@tanstack/react-query";
import { use } from "react";
import {LimitShownPrintingPointsContext} from "../../tools/contexts/LimitShownPrintingPointsContext.ts"
import {useNavigate} from "react-router-dom";

export const PrintingPointsTable: React.FC = () => {
    const navigate = useNavigate();
    const {id, hideDetails} = use(LimitShownPrintingPointsContext);
    const {data} = useQuery(createGetAllPrintingPointsQuery());

    return (
        <main>
            <section className={styles.printingPointsSection}>
                {!id && <h3>Nasze punkty druku</h3>}
                <div>
                    {data?.map((pp) => {
                        if(!id || id == pp.printingPointId) {
                            return (
                                <div key={pp.printingPointId} data-id={pp.printingPointId} className={`${styles.printingPointDiv} printing-point-div-marker`} onClick={() => {
                                    navigate(`/printingpoints/${pp.printingPointId}`);
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