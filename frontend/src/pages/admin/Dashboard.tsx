import { useCallback, useEffect, useState } from "react";
import {
  AlertCircle,
  IndianRupee,
  ShoppingCart,
  ClipboardList,
  Factory,
  AlertTriangle,
  Package,
  RefreshCw,
  Wallet,
} from "lucide-react";
import {
  BarChart,
  Bar,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import DashboardLayout from "../../components/layout/DashboardLayout";
import {
  getDashboardSummary,
  type DashboardSummary,
} from "../../api/reports";
import "./Dashboard.css";

const currency = (value: number | null | undefined) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value ?? 0);

const number = (value: number | null | undefined) =>
  new Intl.NumberFormat("en-IN").format(value ?? 0);

export default function Dashboard() {
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = useCallback(async (showLoading = true) => {
    if (showLoading) setLoading(true);
    setError("");

    try {
      const result = await getDashboardSummary();
      setData(result);
    } catch {
      setError(
        "Unable to load dashboard data. Check that the backend is running and try again."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    getDashboardSummary()
      .then((result) => { if (!cancelled) setData(result); })
      .catch(() => { if (!cancelled) setError("Unable to load dashboard data. Check that the backend is running and try again."); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const cards = data
    ? [
        {
          title: "Total Sales",
          value: currency(data.totalSales),
          icon: IndianRupee,
          color: "orange",
          description: "Overall sales value",
        },
        {
          title: "Receivables",
          value: currency(data.totalReceivables),
          icon: Wallet,
          color: "blue",
          description: "Pending customer payments",
        },
        {
          title: "Total Purchases",
          value: currency(data.totalPurchases),
          icon: ShoppingCart,
          color: "purple",
          description: "Overall purchase value",
        },
        {
          title: "Pending Orders",
          value: number(data.pendingOrdersCount),
          icon: ClipboardList,
          color: "green",
          description: "Orders awaiting completion",
        },
        {
          title: "Active Production",
          value: number(data.activeProductionRunsCount),
          icon: Factory,
          color: "blue",
          description: "Ongoing production runs",
        },
        {
          title: "Low Stock Alerts",
          value: number(data.lowStockAlertsCount),
          icon: AlertTriangle,
          color: "red",
          description: "Items requiring attention",
        },
      ]
    : [];

  const salesChart = Object.entries(data?.categoryWiseSales ?? {}).map(
    ([category, sales]) => ({
      category,
      sales: Number(sales),
    })
  );

  return (
    <DashboardLayout
      title="Admin Dashboard"
      subtitle="Overview of your manufacturing business"
    >
      <div className="dashboard-welcome">
        <div>
          <h2>Business Overview</h2>
          <p>Monitor sales, inventory and production performance.</p>
        </div>

        <button
          className="dashboard-refresh"
          onClick={() => void loadDashboard()}
          disabled={loading}
        >
          <RefreshCw size={16} className={loading ? "refresh-spinning" : ""} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="dashboard-error">
          <AlertCircle size={20} />
          <span>{error}</span>
          <button onClick={() => void loadDashboard()}>Retry</button>
        </div>
      )}

      {loading && !data ? (
        <div className="dashboard-loading">
          <div className="loading-spinner" />
          <p>Loading your business overview...</p>
        </div>
      ) : data ? (
        <>
          <div className="dashboard-card-grid">
            {cards.map((card) => {
              const Icon = card.icon;

              return (
                <article className="dashboard-stat-card" key={card.title}>
                  <div className={`stat-icon ${card.color}`}>
                    <Icon size={21} />
                  </div>
                  <p className="stat-title">{card.title}</p>
                  <h3>{card.value}</h3>
                  <span className="stat-description">
                    {card.description}
                  </span>
                </article>
              );
            })}
          </div>

          <div className="dashboard-section-grid">
            <section className="dashboard-panel sales-panel">
              <div className="panel-heading">
                <div>
                  <h3>Category-wise Sales</h3>
                  <p>Sales distribution by product category</p>
                </div>
                <span className="panel-icon">
                  <IndianRupee size={19} />
                </span>
              </div>

              {salesChart.length > 0 ? (
                <div className="sales-chart">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={salesChart}
                      margin={{ top: 15, right: 10, left: 5, bottom: 5 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis
                        dataKey="category"
                        tick={{ fontSize: 11 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        tick={{ fontSize: 11 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip
                        formatter={(value) => currency(Number(value))}
                      />
                      <Bar
                        dataKey="sales"
                        name="Sales"
                        fill="#f28c38"
                        radius={[5, 5, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <div className="chart-empty">
                  No category-wise sales data available.
                </div>
              )}
            </section>

            <section className="dashboard-panel inventory-panel">
              <div className="panel-heading">
                <div>
                  <h3>Inventory Valuation</h3>
                  <p>Current stock value by type</p>
                </div>
                <span className="panel-icon">
                  <Package size={19} />
                </span>
              </div>

              <div className="valuation-list">
                <div className="valuation-item">
                  <span className="valuation-dot raw-dot" />
                  <div>
                    <strong>Raw Materials</strong>
                    <small>Raw material stock</small>
                  </div>
                  <b>{currency(data.rawMaterialStockValuation)}</b>
                </div>

                <div className="valuation-item">
                  <span className="valuation-dot packaging-dot" />
                  <div>
                    <strong>Packaging Materials</strong>
                    <small>Packaging stock</small>
                  </div>
                  <b>{currency(data.packagingStockValuation)}</b>
                </div>

                <div className="valuation-item">
                  <span className="valuation-dot finished-dot" />
                  <div>
                    <strong>Finished Goods</strong>
                    <small>Ready-to-sell products</small>
                  </div>
                  <b>{currency(data.finishedGoodsStockValuation)}</b>
                </div>
              </div>

              <div className="valuation-total">
                <span>Total Inventory Value</span>
                <strong>
                  {currency(
                    Number(data.rawMaterialStockValuation ?? 0) +
                    Number(data.packagingStockValuation ?? 0) +
                    Number(data.finishedGoodsStockValuation ?? 0)
                  )}
                </strong>
              </div>
            </section>
          </div>

          <div className="dashboard-footer-note">
            <span>Dashboard data loaded from your OrdertoAudit backend.</span>
            <span>Values reflect the latest API response.</span>
          </div>
        </>
      ) : (
        <div className="dashboard-empty">
          <AlertCircle size={25} />
          <h3>Dashboard data unavailable</h3>
          <p>Refresh the page after checking your backend connection.</p>
          <button onClick={() => void loadDashboard()}>Try again</button>
        </div>
      )}
    </DashboardLayout>
  );
}
