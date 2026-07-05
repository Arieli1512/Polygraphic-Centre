import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import type { UserRole } from '../types/auth.ts';
import {useAuth} from "../auth/UseAuth.tsx";

interface RoleGuardProps {
    allowedRoles: UserRole[];
}

export const RoleGuard: React.FC<RoleGuardProps> = ({ allowedRoles }) => {
    const { user,  } = useAuth();
    const role = user.role;
    const isAuthenticated = user.isAuthenticated;

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (!allowedRoles.includes(role)) {
        return (
            <div style={{ padding: '40px', textAlign: 'center', fontFamily: 'system-ui, sans-serif' }}>
                <h2 style={{ color: '#ef4444' }}>🚫 Brak uprawnień</h2>
                <p>Twoja obecna rola (<strong>{role}</strong>) nie posiada uprawnień dostępu do tej sekcji systemu.</p>
                <p style={{ fontSize: '0.9rem', color: 'var(--text)', marginTop: '10px' }}>
                    Wskazówka: Możesz zmienić rolę w pliku <code>src/api/mockAuthStore.ts</code> aby przetestować ten widok.
                </p>
            </div>
        );
    }

    // Jeśli rola jest prawidłowa, renderujemy komponenty wewnętrzne
    return <Outlet />;
};