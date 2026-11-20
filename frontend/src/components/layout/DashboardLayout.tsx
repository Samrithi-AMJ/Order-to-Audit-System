import {
  LayoutDashboard,
  ShoppingCart,
  Package,
  Factory,
  ClipboardList,
  Truck,
  Receipt,
  CreditCard,
  BarChart3,
  Users,
  Boxes,
  LogOut,
  Menu,
  X,
} from "lucide-react";
import { useState, type ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./DashboardLayout.css";

interface DashboardLayoutProps {
  children: ReactNode;
  title: string;
  subtitle?: string;
}

const navigation = [
  { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard, roles: ["ADMIN", "OWNER"] },
  { label: "Sales Orders", path: "/orders", icon: ClipboardList, roles: ["SALESMAN"] },
  { label: "Customers", path: "/customers", icon: Users, roles: ["SALESMAN"] },
  { label: "Purchases", path: "/purchases", icon: ShoppingCart, roles: ["PRODUCTION"] },
  { label: "Inventory", path: "/inventory", icon: Package, roles: ["PRODUCTION"] },
  { label: "Production", path: "/production", icon: Factory, roles: ["PRODUCTION"] },
  { label: "BOM", path: "/boms", icon: Factory, roles: ["PRODUCTION"] },
  { label: "Master Data", path: "/master-data", icon: Boxes, roles: ["PRODUCTION"] },
  { label: "Dispatch", path: "/dispatch", icon: Truck, roles: ["ADMIN", "OWNER"] },
  { label: "Invoices", path: "/invoices", icon: Receipt, roles: ["ACCOUNTANT"] },
  { label: "Payments", path: "/payments", icon: CreditCard, roles: ["ACCOUNTANT"] },
  { label: "Reports", path: "/reports", icon: BarChart3, roles: ["ACCOUNTANT"] },
];

export default function DashboardLayout({
  children,
  title,
  subtitle,
}: DashboardLayoutProps) {
  const { user, logout, hasRole } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const isManager = hasRole("ADMIN", "OWNER");
  const visibleNavigation = navigation.filter((item) => isManager || item.roles.some((role) => hasRole(role)));

  return (
    <div className="erp-layout">
      <aside className={`erp-sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <div className="erp-brand">
          <div className="erp-brand-icon">O</div>
          <div>
            <h2>OrdertoAudit</h2>
            <span>MANUFACTURING ERP</span>
          </div>
          <button
            className="sidebar-close"
            onClick={() => setMenuOpen(false)}
            aria-label="Close menu"
          >
            <X size={20} />
          </button>
        </div>

        <div className="sidebar-section-label">WORKSPACE</div>

        <nav className="erp-navigation">
          {visibleNavigation.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                end
                onClick={() => setMenuOpen(false)}
                className={({ isActive }) =>
                  `erp-nav-link ${isActive ? "active" : ""}`
                }
              >
                <Icon size={19} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="user-avatar">
            {(user?.fullName || user?.username || "U")
              .charAt(0)
              .toUpperCase()}
          </div>
          <div className="user-details">
            <strong>{user?.fullName || user?.username}</strong>
            <span>{user?.roles?.join(", ")}</span>
          </div>
          <button
            className="logout-button"
            onClick={logout}
            title="Logout"
            aria-label="Logout"
          >
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      {menuOpen && (
        <button
          className="sidebar-overlay"
          onClick={() => setMenuOpen(false)}
          aria-label="Close navigation"
        />
      )}

      <main className="erp-main">
        <header className="erp-topbar">
          <button
            className="mobile-menu-button"
            onClick={() => setMenuOpen(true)}
            aria-label="Open menu"
          >
            <Menu size={22} />
          </button>
          <div>
            <h1>{title}</h1>
            <p>{subtitle || "Manage your manufacturing operations"}</p>
          </div>
          <div className="topbar-user">
            <span>{user?.fullName || user?.username}</span>
            <div className="topbar-avatar">
              {(user?.fullName || user?.username || "U")
                .charAt(0)
                .toUpperCase()}
            </div>
          </div>
        </header>

        <section className="erp-content">{children}</section>
      </main>
    </div>
  );
}
