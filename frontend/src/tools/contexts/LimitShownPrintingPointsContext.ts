import {createContext} from "react";

export const LimitShownPrintingPointsContext = createContext<{id: number | null, hideDetails: boolean}>({id: null, hideDetails: false});