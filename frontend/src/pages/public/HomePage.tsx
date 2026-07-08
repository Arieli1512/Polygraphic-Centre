import React from 'react'
import {useNavigate} from 'react-router-dom'
import styles from './HomePage.module.css' // Import your scoped styling definitions
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {PrintingPointsTable} from "../../components/tables/PrintingPointsTable.tsx";

export const HomePage: React.FC = () => {
    const navigate = useNavigate()
    const { user,  } = useAuth();
    const role = user.role;

    return (
        <div>

            <main className={styles.homeMain}>
                <h1 className={styles.homeTitle}>
                    Szybki druk internetowy bez zbędnych formalności
                </h1>
                <p className={styles.homeSubtitle}>
                    Wyślij plik, oblicz koszty w kilka sekund i odbierz gotowe zlecenie w wybranym punkcie poligraficznym.
                </p>

                <section className={styles.navigationSection}>
                    <div className={styles.navigationGrid}>

                        {(role === 'EMPLOYEE' || role === 'ADMIN') && (
                            <button
                                onClick={() => navigate('/operator/queue')}
                                className={styles.navigationCard}>
                                Zamówienia
                            </button>
                        )}

                        {role === 'ADMIN' && (
                            <>
                                <button
                                    onClick={() => navigate('/admin/management')}
                                    className={styles.navigationCard}>
                                    Zarządzanie Punktem Druku
                                </button>
                                <button
                                    onClick={() => navigate('/admin/operators')}
                                    className={styles.navigationCard}>
                                    Zarządzanie Pracownikami
                                </button>
                            </>
                        )}

                        {role === 'GOSC' && (
                            <button
                                onClick={() => navigate('/calculator')}
                                className={styles.navigationCard}>
                                Szacowanie Kosztu
                            </button>
                        )}
                        {role != 'GOSC' && (
                            <button
                                onClick={() => navigate('/order')}
                                className={styles.navigationCard}>
                                Złóż Zamówienie
                            </button>
                        )}

                        {role === 'KLIENT' && (
                            <>
                                <button
                                    onClick={() => navigate('/client/orders')}
                                    className={styles.navigationCard}>
                                    Historia Zamówień
                                </button>
                                <button
                                    onClick={() => navigate('/client/wallet')}
                                    className={styles.navigationCard}>
                                    Portfel
                                </button>
                            </>
                        )}

                        <button
                            onClick={() => navigate('/printingpoints')}
                            className={styles.navigationCard}>
                            Punkty Druku
                        </button>

                    </div>
                </section>

                <section>
                    <PrintingPointsTable/>
                </section>


            </main>

        </div>
    )
}