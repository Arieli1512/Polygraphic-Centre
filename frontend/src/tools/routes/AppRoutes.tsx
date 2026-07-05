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
/*
import { OperatorQueuePage } from '../../pages/queue/OperatorQueuePage.tsx';
import { PricingConfigPage } from '../../pages/admin/PricingConfigPage.tsx';
*/

export const AppRoutes = () => {
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
                    {/*<Route path="/operator/queue" element={<OrderQueuePage />} />*/}
                </Route>

                {/* Ścieżki tylko dla ADMINA */}
                <Route element={<RoleGuard allowedRoles={['ADMIN']} />}>
                    {/* <Route path="/admin/ratesheet" element={<RateSheetPage />} />
                    <Route path="/admin/operators" element={<OperatorsPage />} />
                    <Route path="/admin/hours" element={<OpeningHoursPage />} />*/}
                </Route>

            </Route>

            {/* Obsługa nieznanych adresów URL */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
};