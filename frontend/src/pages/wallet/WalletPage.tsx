import React, {useState} from 'react'
import styles from './WalletPage.module.css'
import {createUserWalletQuery} from '../../api/apiWallets.ts'
import {createWalletTopUpMutation, type WalletTopUp} from '../../api/apiWalletTopUps.ts'
import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {ImSpinner2} from "react-icons/im";

export const WalletPage: React.FC = () => {
    const { user,  } = useAuth();
    const client_id = user.client_id;
    const {data, isPending} = useQuery(createUserWalletQuery(client_id ?? -1));
    const queryClient = useQueryClient();
    const mutationWalletTopUp = useMutation({...createWalletTopUpMutation(),
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: [`wallet`]})
        }});
    const [showTopUp, setShowTopUp] = useState(false);
    const [topUpValue, setTopUpValue] = useState(0);

    return (
        <main className={styles.walletMain}>
            {isPending ? (<ImSpinner2 className="icon-spin"/>) : ( <>
                <div className={styles.walletInfoContainer}>
                    <h2>Portfel Klienta</h2>
                    {data?.status !== 'ACTIVE' &&
                        <div className={styles.walletStatusWarning}>
                            {data?.status === 'BLOCKED' ? "Portfel zablokowany" : data?.status === 'CLOSED' ? "Portfel zamknięty" : "Błędne Dane"}
                        </div>
                    }
                    <div className={styles.walletBalance}> {data?.balance.toFixed(2)}zł </div>
                    <button onClick={() => {
                        setShowTopUp(!showTopUp);
                    }}>Doładowanie konta</button>
                </div>

                <div hidden={!showTopUp} className={styles.walletTopUpContainer}>
                    <input type="number" min="0" step=".01" value={topUpValue} onChange={(e) => {
                        const parsed = parseFloat(e.target.value);
                        setTopUpValue(isNaN(parsed) ? 0 : parsed);
                    }}/>
                    <button disabled={mutationWalletTopUp.isPending} onClick={() => {
                        const topUp : WalletTopUp = {
                            client_id: client_id ?? -1,
                            amount: topUpValue
                        }
                        mutationWalletTopUp.mutate(topUp);
                        setTopUpValue(0);
                    }}
                    >Doładuj</button>
                </div>
            </>)}

        </main>
    )
}