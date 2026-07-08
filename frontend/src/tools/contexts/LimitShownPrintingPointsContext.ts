import {createContext} from "react";

export const LimitShownPrintingPointsContext = createContext<{id: number | null, hideDetails: boolean, disabled: boolean | null}>({id: null, hideDetails: false, disabled: false});