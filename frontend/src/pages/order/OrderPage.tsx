import React, {useState} from 'react'
import styles from './OrderPage.module.css'
import {PrintingPointsTable} from "../../components/tables/PrintingPointsTable.tsx";
import {useMutation, useQuery} from "@tanstack/react-query";
import {createGetPrintingPointExtraPricingQuery} from "../../api/apiExtraPricing.ts";
import {createGetPrintingPointRateSheetQuery} from "../../api/apiRateSheets.ts";
import {LimitShownPrintingPointsContext} from "../../tools/contexts/LimitShownPrintingPointsContext.ts"
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {useLocation, useNavigate} from "react-router-dom";
import {useParams} from "react-router";
import {createNewOrderMutation, type Order} from "../../api/apiOrders.ts";
import type {PrintSettings} from "../../api/apiPrintSettings.ts";

const now = new Date();
if (now.getHours() > 13) now.setDate(now.getDate()+1);
now.setHours(14);
now.setMinutes(0);
const formattedDateTime = now.toISOString().slice(0, 16)


export const OrderPage: React.FC = () => {
    const { user,  } = useAuth();
    const role = user.role;

    const navigate = useNavigate();
    const location = useLocation();
    const editingOrder = location.state?.editingOrder;
    const editingOrderDetails = location.state?.editingOrderDetails;
    const editing: boolean = !editingOrder ? false : true;

    const id_param_tmp = useParams().id;
    const id_param: number | null = !id_param_tmp ? (!editingOrder ? null : editingOrder.printing_point_id) : parseInt(id_param_tmp);

    const mutationNewOrder = useMutation(createNewOrderMutation());
    const [limitShownPrintingPoints, setLimitShownPrintingPoints] = useState<number | null>(id_param);

    const [printingPointId, setPrintingPointId] = useState(id_param);
    const {data: PrintingPointRateSheetData} = useQuery(createGetPrintingPointRateSheetQuery(printingPointId ?? -1));
    const {data: PrintingPointExtraPricingData} = useQuery(createGetPrintingPointExtraPricingQuery(printingPointId ?? -1));
    const [newFile, setNewFile] = useState(!editing);
    const [filePath, setFilePath] = useState(editingOrder?.file_path ?? undefined);
    const [pageCount, setPageCount] = useState(editingOrder?.page_count ?? 1);
    const [pickupAt, setPickupAt] = useState(editingOrder?.pickup_at.slice(0,16) ?? formattedDateTime);
    const [format, setFormat] = useState(editingOrderDetails?.format ?? 'A3');
    const [paperType, setPaperType] = useState(editingOrderDetails?.paper_type ?? 'standardowy');
    const [colorMode, setColorMode] = useState(editingOrderDetails?.color_mode ?? 'COLOR');
    const [duplex, setDuplex] = useState(editingOrderDetails?.duplex ?? 'SINGLE_SIDED');
    const [orient, setOrient] = useState(editingOrderDetails?.orientation ?? 'PORTRAIT');
    const [finishing, setFinishing] = useState(editingOrderDetails?.finishing ?? 'NONE');
    const [copies, setCopies] = useState(editingOrderDetails?.copies ?? 1);


    let costValidity = true;
    let cost = 0;
    if (!printingPointId|| !PrintingPointRateSheetData) costValidity = false;
    else {
        let rate = PrintingPointRateSheetData.find((rs) => (rs.format == format && rs.paper_type == paperType) )?.page_price;
        if (!rate) rate = PrintingPointRateSheetData.find((rs) => (rs.format == format) )?.page_price;
        if (!rate) {
            costValidity = false;
        } else {
            cost = rate * pageCount * copies;
        }
    }

    if (!printingPointId && PrintingPointExtraPricingData) {
        switch (finishing) {
            case 'BINDING':
                cost += PrintingPointExtraPricingData.binding_price ?? 0;
                break;
            case 'STAPLING':
                cost += PrintingPointExtraPricingData.stapling_price ?? 0;
                break;
            case 'COVER':
                cost += PrintingPointExtraPricingData.cover_price ?? 0;
                break;
            default:
                break;
        }
    }

    const costTextStyle = {
        fontSize: costValidity ? '3rem' : '1.2rem'
    };

    return (
        <main>
            <form className={styles.priceCalculatorForm}
                  onSubmit={(e: React.SyntheticEvent) => {
                      e.preventDefault();

                      if (role === 'GOSC') {
                          navigate('/login');
                          e.preventDefault();
                          return;
                      }

                      const formData = new FormData();
                      const newOrder: Order = {
                          printing_point_id: printingPointId ?? -1,
                          client_id: user.client_id ?? -1,
                          file_path: filePath,
                          page_count: pageCount,
                          total_price: cost,
                          pickup_at: pickupAt,
                          status: 'PENDING'
                      }
                      const newOrderDetails: PrintSettings = {
                          format: format,
                          paper_type: paperType,
                          color_mode: colorMode,
                          duplex: duplex,
                          orientation: orient,
                          finishing: finishing,
                          copies: copies
                      }

                      if (editing) {
                          newOrder.order_id = editingOrder.order_id;
                          newOrderDetails.order_id = editingOrder.order_id;
                      }
                      if (newFile) {
                          const fileInput = document.getElementById("fileInput") as HTMLInputElement;
                          if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
                              alert("No file found");
                              e.preventDefault();
                              return;
                          }
                          const file = fileInput.files[0];
                          if (!file) {
                              alert("No file found");
                              e.preventDefault();
                              return;
                          }
                          formData.append("document", file);

                      }

                      formData.append("order", JSON.stringify(newOrder));
                      formData.append("print_settings", JSON.stringify(newOrderDetails));

                      mutationNewOrder.mutate(formData)
                  }}>

                <div className={styles.priceCalculatorPrintingPointChoice}>
                    <label className={styles.priceCalculatorLabel}> Punkt Druku </label>
                    <br/>
                    <LimitShownPrintingPointsContext value={{id: limitShownPrintingPoints, hideDetails: false}}>
                        <div className={styles.priceCalculatorPrintingPointChoiceContainer} onClick={(e)=> {
                            if (limitShownPrintingPoints) {
                                setPrintingPointId(-1);
                                setLimitShownPrintingPoints(null);
                            } else {
                                const path = e.nativeEvent.composedPath();
                                for (const element of path) {
                                    if (element instanceof HTMLElement && element.classList.contains('printing-point-div-marker')) {
                                        setPrintingPointId(Number(element.dataset.id));
                                        setLimitShownPrintingPoints(Number(element.dataset.id));
                                        e.stopPropagation();
                                        break;
                                    }
                                }
                            }
                        }}>
                            <PrintingPointsTable/>
                        </div>
                    </LimitShownPrintingPointsContext>
                </div>

                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Format </label>
                    <br/>
                    <select name="format" className={`${styles.priceCalculatorBasicCombobox} ${styles.priceCalculatorInputBase}`}
                            defaultValue={format} onChange={(e)=> {
                        setFormat(e.target.value);
                    }}>
                        <option value="A3">A3</option>
                        <option value="A4">A4</option>
                        <option value="A5">A5</option>
                    </select>
                </div>
                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Typ Papieru </label>
                    <br/>
                    <select name="paper-type" className={`${styles.priceCalculatorBasicCombobox} ${styles.priceCalculatorInputBase}`}
                            defaultValue={paperType} onChange={(e)=> {
                        setPaperType(e.target.value);
                    }}>
                        <option value="standardowy">standardowy</option>
                        <option value="błyszczący">błyszczący</option>
                        <option value="matowy">matowy</option>
                        <option value="kredowy">kredowy</option>
                    </select>
                </div>
                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Tryb Koloru </label>
                    <br/>
                    <select name="color-mode" className={`${styles.priceCalculatorBasicCombobox} ${styles.priceCalculatorInputBase}`}
                            defaultValue={colorMode} onChange={(e)=> {
                        setColorMode(e.target.value);
                    }}>
                        <option value="COLOR">W kolorze</option>
                        <option value="GRAYSCALE">Czarno-biały</option>
                    </select>
                </div>
                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Duplex </label>
                    <br/>
                    <select name="duplex" className={`${styles.priceCalculatorBasicCombobox} ${styles.priceCalculatorInputBase}`}
                            defaultValue={duplex} onChange={(e)=> {
                        setDuplex(e.target.value);
                    }}>
                        <option value="SINGLE_SIDED">Jednostronnie</option>
                        <option value="DOUBLE_SIDED">Dwustronnie</option>
                    </select>
                </div>
                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Orientacja </label>
                    <br/>
                    <select name="orientation" className={`${styles.priceCalculatorBasicCombobox} ${styles.priceCalculatorInputBase}`}
                            defaultValue={orient} onChange={(e)=> {
                        setOrient(e.target.value);
                    }}>
                        <option value="PROTRAIT">Pionowa</option>
                        <option value="LANDSCAPE">Pozioma</option>
                    </select>
                </div>
                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Wykończenie </label>
                    <br/>
                    <select name="finishing" className={`${styles.priceCalculatorBasicCombobox} ${styles.priceCalculatorInputBase}`}
                            defaultValue={finishing} onChange={(e)=> {
                        setFinishing(e.target.value);
                    }}>
                        <option value="NONE">Brak</option>
                        <option value="BINDING">Oprawianie</option>
                        <option value="STAPLING">Zszywanie</option>
                        <option value="COVER">Okładka</option>
                    </select>
                </div>
                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Ilość Kopii </label>
                    <br/>
                    <input name="copies" type="number" className={`${styles.priceCalculatorBasicInputField} ${styles.priceCalculatorInputBase}`}
                           defaultValue={copies} onChange={(e)=> {
                        setCopies(e.target.valueAsNumber);
                    }}/>
                </div>
                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Ilość stron </label>
                    <br/>
                    <input name="page-count" type="number" className={`${styles.priceCalculatorBasicInputField} ${styles.priceCalculatorInputBase}`}
                           defaultValue={pageCount} onChange={(e)=> {
                        setPageCount(e.target.valueAsNumber);
                    }}/>
                </div>

                {(role != 'GOSC' ) && (
                    <div className={styles.priceCalculatorBasicInput}>
                        <label className={styles.priceCalculatorLabel}> Plik </label>
                        <br/>
                        <input id="fileInput" name="file" type="file" className={`${styles.priceCalculatorFileInput} ${styles.priceCalculatorInputBase}`}
                               onChange={(e)=> {
                            setFilePath(e.target.value);
                            setNewFile(true);
                        }}/>
                    </div>
                )}

                <div className={styles.priceCalculatorBasicInput}>
                    <label className={styles.priceCalculatorLabel}> Czas odbioru </label>
                    <br/>
                    <input name="pickup-at" type="datetime-local"
                           className={`${styles.priceCalculatorDatetimeInput} ${styles.priceCalculatorInputBase}`}
                           defaultValue={pickupAt} onChange={(e)=> {
                        setPickupAt(e.target.value);
                    }}/>
                </div>

                <label className={styles.totalCostLabel} >Łączny Koszt: </label>
                <br/>
                <label className={styles.totalCostValue} style={costTextStyle} id="costText">{costValidity ? `${cost}zł` : "Proszę wybrać punkt druku."}</label>
                <br/>

                {(role != 'GOSC' ) && (
                    <input className={styles.orderButton} type="submit" value={`${editing ? "Edytuj" : "Złóż"} Zamówienie`}/>
                )}
                {(role === 'GOSC' ) && (
                    <input className={styles.orderButton} type="submit" value="Wymagane Zalogowanie"/>
                )}

            </form>
        </main>
    )
}