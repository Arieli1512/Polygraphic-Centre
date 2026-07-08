import React, {useRef, useState} from "react";
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {useQuery} from "@tanstack/react-query";
import {createOperatorPrintingPointQuery} from "../../api/apiOperators.ts";
import {createGetPrintingPointRateSheetQuery, type RateSheet} from "../../api/apiRateSheets.ts";
import {createGetPrintingPointExtraPricingQuery, type ExtraPricingSetting} from "../../api/apiExtraPricing.ts";
import {createGetPrintingPointOpeningHoursQuery, type OpeningHours} from "../../api/apiOpeningHours.ts";
import {ImSpinner2} from "react-icons/im";
import {RateSheetTable} from "../../components/tables/RateSheetTable.tsx";
import {ExtraPricingTable} from "../../components/tables/ExtraPricingTable.tsx";
import {OpeningHoursTable} from "../../components/tables/OpeningHoursTable.tsx";
import styles from "./GeneralManagementPage.module.css"
import {PrintingPointsTable} from "../../components/tables/PrintingPointsTable.tsx";
import {LimitShownPrintingPointsContext} from "../../tools/contexts/LimitShownPrintingPointsContext.ts";

export const GeneralManagementPage: React.FC = () => {
    const rateSheetRef = useRef<{submitRateSheet: () => Promise<RateSheet[]>; }>(null);
    const extraPricingRef = useRef<{submitExtraPricing: () => Promise<ExtraPricingSetting | undefined>; }>(null);
    const openingHoursRef = useRef<{submitOpeningHours: () => Promise<OpeningHours[]>; }>(null);
    const { user,  } = useAuth();
    const operator_id = user.client_id;
    const {data: printingPointId, isPending: EmployeePrintingPointQueryIsPending} = useQuery(createOperatorPrintingPointQuery(operator_id ?? -1));
    const {data: rateSheetData, isPending: RateSheetQueryIsPending} = useQuery(createGetPrintingPointRateSheetQuery(printingPointId));
    const {data: extraPricingData, isPending: ExtraPricingQueryIsPending} = useQuery(createGetPrintingPointExtraPricingQuery(printingPointId));
    const {data: openingHoursData, isPending: OpeningHoursQueryIsPending} = useQuery(createGetPrintingPointOpeningHoursQuery(printingPointId));
    const [isLoading, setIsLoading] = useState<boolean>(false);

    return (
            <main className={styles.managementMain}>
                {EmployeePrintingPointQueryIsPending ? <ImSpinner2 className="icon-spin"/> :
                <form onSubmit={async (e) => {
                    setIsLoading(true);
                    e.preventDefault();
                    try {
                        await Promise.all([
                            rateSheetRef.current?.submitRateSheet(),
                            extraPricingRef.current?.submitExtraPricing(),
                            openingHoursRef.current?.submitOpeningHours()
                        ]);
                    } catch (e) {
                        console.error("One of the saves failed:", e);
                    } finally { setIsLoading(false);}
                }}>

                    {printingPointId &&
                        <LimitShownPrintingPointsContext value={{id: printingPointId, hideDetails: false, disabled: true}}>
                            <PrintingPointsTable/>
                        </LimitShownPrintingPointsContext>}

                    <div className={styles.managementTablesContainer}>
                        <div className={styles.managementTableDiv}>
                            <h2>Cennik</h2>
                            {RateSheetQueryIsPending ? <ImSpinner2 className="icon-spin"/> :
                                <RateSheetTable ref={rateSheetRef} data={rateSheetData}/>}
                        </div>

                        <div className={styles.managementTableDiv}>
                            <h2>Dopłaty</h2>
                            {ExtraPricingQueryIsPending ? <ImSpinner2 className="icon-spin"/> :
                                <ExtraPricingTable ref={extraPricingRef} data={extraPricingData}/>}
                        </div>

                        <div className={styles.managementTableDiv}>
                            <h2>Godziny Otwarcia</h2>
                            {OpeningHoursQueryIsPending ? <ImSpinner2 className="icon-spin"/> :
                                <OpeningHoursTable ref={openingHoursRef} data={openingHoursData}/>}
                        </div>
                    </div>

                    <button type="submit" disabled={isLoading || RateSheetQueryIsPending || ExtraPricingQueryIsPending || OpeningHoursQueryIsPending}>Zapisz Zmiany</button>
                </form>}
            </main>
    )
}