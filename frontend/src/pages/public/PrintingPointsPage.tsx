import React from 'react'
import {PrintingPointsTable} from "../../components/tables/PrintingPointsTable";
import {LimitShownPrintingPointsContext} from "../../tools/contexts/LimitShownPrintingPointsContext.ts"
import { useParams } from "react-router";

export const PrintingPointsPage: React.FC = () => {
    const params = useParams();
    let limitShownPrintingPoints: number | string | undefined | null = params.id;
    if (!limitShownPrintingPoints) limitShownPrintingPoints = null;
    else limitShownPrintingPoints = parseInt(limitShownPrintingPoints);

    return (
        <div>

            <main>
                <LimitShownPrintingPointsContext value={{id: limitShownPrintingPoints, hideDetails: false}}>
                    <PrintingPointsTable/>
                </LimitShownPrintingPointsContext>
            </main>

        </div>
    )
}