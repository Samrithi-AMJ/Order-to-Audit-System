import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { roleHomePath } from "./utils/roleHome";

const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const Dashboard = lazy(() => import("./pages/admin/Dashboard"));
const SalesOrdersPage = lazy(() => import("./pages/orders/SalesOrdersPage"));
const CustomersPage = lazy(() => import("./pages/customers/CustomersPage"));
const PurchasesPage = lazy(() => import("./pages/purchases/PurchasesPage"));
const InventoryPage = lazy(() => import("./pages/inventory/InventoryPage"));
const ProductionPage = lazy(() => import("./pages/production/ProductionPage"));
const BomPage = lazy(() => import("./pages/production/BomPage"));
const MasterDataPage = lazy(() => import("./pages/master-data/MasterDataPage"));
const DispatchPage = lazy(() => import("./pages/dispatch/DispatchPage"));
const InvoicesPage = lazy(() => import("./pages/invoices/InvoicesPage"));
const PaymentsPage = lazy(() => import("./pages/payments/PaymentsPage"));
const ReportsPage = lazy(() => import("./pages/reports/ReportsPage"));

function ProtectedRoute() {
  const { user, loading } = useAuth();
  if (loading) return <div className="loading-screen">Loading OrdertoAudit...</div>;
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}

function RoleRoute({ roles, children }: { roles: string[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.roles.some((role) => ["ADMIN", "OWNER"].includes(role) || roles.includes(role))) return <>{children}</>;
  return <Navigate to={roleHomePath(user.roles)} replace />;
}

function AppRoutes() {
  const { user, loading } = useAuth();
  if (loading) return <div className="loading-screen">Loading OrdertoAudit...</div>;
  const defaultPath = user ? roleHomePath(user.roles) : "/login";

  return (
    <Suspense fallback={<div className="loading-screen" role="status">Loading workspace…</div>}>
      <Routes>
        <Route path="/login" element={user ? <Navigate to={defaultPath} replace /> : <Login />} />
        <Route path="/register" element={user ? <Navigate to={defaultPath} replace /> : <Register />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<RoleRoute roles={[]}><Dashboard /></RoleRoute>} />
          <Route path="/salesman" element={<Navigate to="/orders" replace />} />
          <Route path="/orders" element={<RoleRoute roles={["SALESMAN"]}><SalesOrdersPage /></RoleRoute>} />
          <Route path="/customers" element={<RoleRoute roles={["SALESMAN"]}><CustomersPage /></RoleRoute>} />
          <Route path="/purchases" element={<RoleRoute roles={["PRODUCTION"]}><PurchasesPage /></RoleRoute>} />
          <Route path="/inventory" element={<RoleRoute roles={["PRODUCTION"]}><InventoryPage /></RoleRoute>} />
          <Route path="/production" element={<RoleRoute roles={["PRODUCTION"]}><ProductionPage /></RoleRoute>} />
          <Route path="/boms" element={<RoleRoute roles={["PRODUCTION"]}><BomPage /></RoleRoute>} />
          <Route path="/master-data" element={<RoleRoute roles={["PRODUCTION"]}><MasterDataPage /></RoleRoute>} />
          <Route path="/dispatch" element={<RoleRoute roles={[]}><DispatchPage /></RoleRoute>} />
          <Route path="/invoices" element={<RoleRoute roles={["ACCOUNTANT"]}><InvoicesPage /></RoleRoute>} />
          <Route path="/payments" element={<RoleRoute roles={["ACCOUNTANT"]}><PaymentsPage /></RoleRoute>} />
          <Route path="/reports" element={<RoleRoute roles={["ACCOUNTANT"]}><ReportsPage /></RoleRoute>} />
        </Route>
        <Route path="*" element={<Navigate to={defaultPath} replace />} />
      </Routes>
    </Suspense>
  );
}

export default function App() {
  return <AuthProvider><AppRoutes /></AuthProvider>;
}
