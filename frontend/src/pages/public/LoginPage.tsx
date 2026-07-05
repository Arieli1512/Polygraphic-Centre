import React from 'react'
import styles from './LoginPage.module.css' // Import your scoped styling definitions
import type { UserRole } from '../../tools/types/auth';
import {useAuth} from "../../tools/auth/UseAuth.tsx";
import {MockUser} from "../../tools/auth/AuthContext.tsx";
import {useNavigate} from 'react-router-dom'

export const LoginPage: React.FC = () => {
    const { user, setUser } = useAuth();
    const navigate = useNavigate()

    return (
        <div>
            <div className={styles.testOptions}>
                <h2>Testing Tools</h2>


                <p>
                    Current User: {user.email || "Logged Out"}
                </p>

                <button
                    onClick={() => setUser(new MockUser('GOSC' as UserRole))}
                    className={styles.testButton}
                >
                    GOSC
                </button>
                <button
                    onClick={() => setUser(new MockUser('KLIENT' as UserRole))}
                    className={styles.testButton}
                >
                    KLIENT
                </button>
                <button
                    onClick={() => setUser(new MockUser('EMPLOYEE' as UserRole))}
                    className={styles.testButton}
                >
                    EMPLOYEE
                </button>
                <button
                    onClick={() => setUser(new MockUser('ADMIN' as UserRole))}
                    className={styles.testButton}
                >
                    ADMIN
                </button>
            </div>

            <main className={styles.loginMain}>
                <form className={styles.loginForm}
                      onSubmit = {(e: React.SyntheticEvent) => {
                    /*
                    const target = e.target as typeof e.target & {
                        email: { value: string };
                        password: { value: string };
                    };
                    const email = target.email.value; // typechecks!
                    const password = target.password.value;
                    */
                    setUser(new MockUser('KLIENT'));
                    navigate('/');
                    e.preventDefault();
                }}>
                    <label>Adres email:</label><br/>
                    <input className={styles.loginInput} type="email"  name="email"/><br/>
                    <label>Hasło:</label><br/>
                    <input className={styles.loginInput} type="password" name="password"/><br/><br/>
                    <input className={styles.loginSubmitButton} type="submit" value="Zaloguj Się"/>
                </form>
            </main>

        </div>
    )
}