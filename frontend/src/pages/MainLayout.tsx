import { Outlet, useNavigate } from 'react-router-dom'
//import type { UserRole } from '../types/auth'
import styles from './MainLayout.module.css' // 1. Fixed: Imported as a 'styles' object
import {useAuth} from "../tools/auth/UseAuth.tsx";
import {MockUser} from "../tools/auth/AuthContext.tsx";


export const MainLayout = () => {

    const { user, setUser } = useAuth();
    const useAuthMock = () => ({
        isAuthenticated: user.isAuthenticated,
        role: user.role,
        email: user.email,
        logout: () => {
            console.log('Wylogowywanie...');
            setUser(new MockUser('GOSC'));
        },
    })

    const navigate = useNavigate()
    const { isAuthenticated, email, logout } = useAuthMock()

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    return (
        // 2. Fixed: Wired up the layoutContainer flex grid layout
        <div className={styles.contentFrame}>

            {/* GLOBAL HEADER */}
            <header className={styles.navBar}>

                {/* User Meta & Action */}

                <button onClick={() => navigate('/')}
                        className={styles.logo}>
                    Drobny Druczek
                </button>

                {isAuthenticated ?  (
                    <div className={styles.userMeta}>
                        <span className={styles.userEmail}>{email}</span>
                        <button onClick={handleLogout} className={styles.logoutButton}>
                            Wyloguj
                        </button>
                    </div>
                    ) : (<div className={styles.userMeta}>

                        <button
                            onClick={() => navigate('/login')}
                            className={styles.loginButton}
                        >
                            Zaloguj się
                        </button>
                    </div>
                    )
                }

            </header>

            {/* Dynamic Nested Page Content Rendering Area */}
            <main className={styles.pageContent}>
                <Outlet />
            </main>


            {/* Footer */}
            <footer className={styles.pageFooter}>
                Polygraphic Centre • wydanie robocze v0 • React + Spring Boot
            </footer>
        </div>
    )
}