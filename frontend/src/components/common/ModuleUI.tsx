/* eslint-disable react-refresh/only-export-components -- shared view and formatting helpers live together. */
import { RefreshCw, Search, AlertCircle, LoaderCircle, X } from "lucide-react";
import type { ReactNode } from "react";
import "./ModuleUI.css";

export function money(value?: number | null) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return "Unavailable";
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(Number(value));
}
export function dateLabel(value?: string) {
  if (!value) return "Unavailable";
  const date = new Date(value);
  return Number.isNaN(date.valueOf()) ? value : date.toLocaleDateString("en-IN", { dateStyle: "medium" });
}
export function errorMessage(error: unknown) {
  const candidate = error as { response?: { data?: { message?: string; error?: string } }; message?: string };
  return candidate?.response?.data?.message || candidate?.response?.data?.error || candidate?.message || "The request could not be completed.";
}
export function StatusBadge({ value }: { value?: string }) {
  return <span className={`status-badge status-${(value || "unknown").toLowerCase().replaceAll("_", "-")}`}>{value?.replaceAll("_", " ") || "Unknown"}</span>;
}
export function ModuleHeading({ eyebrow, title, description, onRefresh, action }: { eyebrow?: string; title: string; description?: string; onRefresh?: () => void; action?: ReactNode }) {
  return <div className="module-heading"><div><span className="module-eyebrow">{eyebrow || "OPERATIONS"}</span><h2>{title}</h2>{description && <p>{description}</p>}</div><div className="module-actions">{action}{onRefresh && <button className="button button-secondary" onClick={onRefresh} type="button"><RefreshCw size={16} /> Refresh</button>}</div></div>;
}
export function SearchBox({ value, onChange, placeholder = "Search records" }: { value: string; onChange: (value: string) => void; placeholder?: string }) {
  return <label className="search-box"><Search size={17} /><input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} /></label>;
}
export function LoadingBlock({ label = "Loading records…" }: { label?: string }) { return <div className="module-state"><LoaderCircle className="spin" size={22} />{label}</div>; }
export function ErrorBlock({ message, retry }: { message: string; retry?: () => void }) { return <div className="module-error"><AlertCircle size={19} /><span>{message}</span>{retry && <button className="button button-secondary" onClick={retry}>Retry</button>}</div>; }
export function EmptyBlock({ title = "No records found", text = "Records will appear here when they are available." }: { title?: string; text?: string }) { return <div className="module-empty"><div className="empty-mark"><Search size={20} /></div><strong>{title}</strong><span>{text}</span></div>; }
export function DataTable({ columns, rows, onRowClick, emptyTitle }: { columns: Array<{ label: string; render: (row: Record<string, unknown>) => ReactNode; align?: "right" }>; rows: Array<Record<string, unknown>>; onRowClick?: (row: Record<string, unknown>) => void; emptyTitle?: string }) {
  if (!rows.length) return <EmptyBlock title={emptyTitle} />;
  return <div className="table-scroll"><table className="data-table"><thead><tr>{columns.map((column) => <th key={column.label} className={column.align === "right" ? "align-right" : ""}>{column.label}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={String(row.id ?? row.key ?? index)} onClick={() => onRowClick?.(row)} className={onRowClick ? "clickable-row" : ""}>{columns.map((column) => <td key={column.label} className={column.align === "right" ? "align-right" : ""}>{column.render(row)}</td>)}</tr>)}</tbody></table></div>;
}
export function Modal({ title, onClose, children, wide }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><section className={`modal-card ${wide ? "modal-wide" : ""}`} role="dialog" aria-modal="true" aria-label={title}><div className="modal-heading"><h3>{title}</h3><button className="icon-button" aria-label="Close dialog" onClick={onClose}><X size={19} /></button></div>{children}</section></div>;
}
export function Notice({ kind = "success", children, onDismiss }: { kind?: "success" | "error"; children: ReactNode; onDismiss?: () => void }) { return <div className={`notice notice-${kind}`} role="status">{children}{onDismiss && <button className="notice-close" onClick={onDismiss} aria-label="Dismiss"><X size={15} /></button>}</div>; }
