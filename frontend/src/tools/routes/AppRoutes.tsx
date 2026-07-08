import { Routes, Route, Navigate } from 'react-router-dom';
import { RoleGuard } from './RoleGuard.tsx';

// Import podstron
import { MainLayout } from '../../pages/MainLayout.tsx';
import { HomePage } from '../../pages/public/HomePage.tsx';
import { LoginPage } from '../../pages/public/LoginPage.tsx';
import { OrderPage } from '../../pages/order/OrderPage.tsx';
import { PrintingPointsPage } from '../../pages/public/PrintingPointsPage.tsx';
import { OrderHistoryPage } from '../../pages/order/OrderHistoryPage.tsx';
import { WalletPage } from '../../pages/wallet/WalletPage.tsx';
import { OrderQueuePage } from '../../pages/order/OrderQueuePage.tsx';
import { GeneralManagementPage } from '../../pages/admin/GeneralManagementPage.tsx';
import { OperatorsPage } from '../../pages/admin/OperatorsPage.tsx';

const AppRoutes = () => {
    return (
        <Routes>
            <Route element={<MainLayout />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/printingpoints/:id" element={<OrderPage />} />
                <Route path="/printingpoints" element={<PrintingPointsPage />} />
                <Route path="/calculator" element={<OrderPage />} />
                <Route path="/order" element={<OrderPage />} />

                {/* Ścieżki tylko dla KLIENTA */}
                <Route element={<RoleGuard allowedRoles={['KLIENT']} />}>
                    <Route path="/client/orders" element={<OrderHistoryPage />} />
                    <Route path="/client/wallet" element={<WalletPage />} />
                </Route>

                {/* Ścieżki dla EMPLOYEE (Pracownika) i ADMINA */}
                <Route element={<RoleGuard allowedRoles={['EMPLOYEE', 'ADMIN']} />}>
                    <Route path="/operator/queue" element={<OrderQueuePage />} />
                </Route>

                {/* Ścieżki tylko dla ADMINA */}
                <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
                    <Route path="/admin/management" element={<GeneralManagementPage />} />
                    <Route path="/admin/operators" element={<OperatorsPage />} />
                </Route>

            </Route>

            {/* Obsługa nieznanych adresów URL */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
};
export default AppRoutes