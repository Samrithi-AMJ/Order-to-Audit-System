import { useCallback, useEffect, useMemo, useState } from "react";
import { Plus, Eye, Check, X as XIcon, FilePlus2 } from "lucide-react";
import DashboardLayout from "../../components/layout/DashboardLayout";
import { DataTable, ErrorBlock, LoadingBlock, Modal, ModuleHeading, Notice, SearchBox, StatusBadge, dateLabel, errorMessage, money } from "../../components/common/ModuleUI";
import { getSalesOrders, getSalesOrder, createSalesOrder, approveSalesOrder, type SalesOrder } from "../../api/sales";
import { masterDataApi, type Customer, type Product } from "../../api/masterData";
import { useAuth } from "../../context/AuthContext";

type DraftLine = { productId: number; quantity: number };
export default function SalesOrdersPage() {
  const { hasRole } = useAuth();
  const canApprove = hasRole("ADMIN", "OWNER");
  const [rows, setRows] = useState<SalesOrder[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [customerFilter, setCustomerFilter] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [selected, setSelected] = useState<SalesOrder | null>(null);
  const [creating, setCreating] = useState(false);
  const [busy, setBusy] = useState(false);
  const [customerId, setCustomerId] = useState("");
  const [orderDate, setOrderDate] = useState(new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState("");
  const [lines, setLines] = useState<DraftLine[]>([{ productId: 0, quantity: 1 }]);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [orders, clientList, productList] = await Promise.all([getSalesOrders(), masterDataApi.customers(), masterDataApi.products()]);
      setRows(orders); setCustomers(clientList); setProducts(productList.filter((product) => product.active));
    } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  }, []);
  useEffect(() => { let cancelled = false; Promise.all([getSalesOrders(), masterDataApi.customers(), masterDataApi.products()]).then(([orders, clientList, productList]) => { if (!cancelled) { setRows(orders); setCustomers(clientList); setProducts(productList.filter(product => product.active)); } }).catch(error => { if (!cancelled) setError(errorMessage(error)); }).finally(() => { if (!cancelled) setLoading(false); }); return () => { cancelled = true; }; }, []);

  const filtered = useMemo(() => rows.filter((order) => {
    const needle = query.toLowerCase();
    return (status === "ALL" || order.status === status) && (!customerFilter || order.customerId === Number(customerFilter)) && (!startDate || order.orderDate >= startDate) && (!endDate || order.orderDate <= endDate) && `${order.orderNumber} ${order.customerName} ${order.customerCode} ${order.salesmanName}`.toLowerCase().includes(needle);
  }), [rows, query, status, customerFilter, startDate, endDate]);
  const preview = useMemo(() => lines.reduce((sum, line) => {
    const product = products.find((item) => item.id === line.productId);
    const base = product ? product.sellingPrice * Number(line.quantity || 0) : 0;
    return sum + base + base * ((product?.gstRate || 0) / 100);
  }, 0), [lines, products]);

  const showDetails = async (order: SalesOrder) => {
    try { setSelected(await getSalesOrder(order.id)); } catch (e) { setError(errorMessage(e)); }
  };
  const submitOrder = async (event: React.FormEvent) => {
    event.preventDefault(); setBusy(true); setError("");
    try {
      const created = await createSalesOrder({ customerId: Number(customerId), orderDate, notes: notes || undefined, items: lines.map((line) => ({ productId: line.productId, quantity: Number(line.quantity) })) });
      setNotice(`Order ${created.orderNumber} submitted. Backend total: ${money(created.grandTotal)}.`); setCreating(false); setLines([{ productId: 0, quantity: 1 }]); setNotes(""); await load(); setSelected(created);
    } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };
  const review = async (approved: boolean) => {
    if (!selected) return;
    const enteredReason = approved ? undefined : window.prompt("Reason for rejection");
    if (!approved && enteredReason === null) return;
    const rejectionReason = enteredReason?.trim() || (approved ? undefined : "Rejected by administrator");
    setBusy(true); setError("");
    try { const updated = await approveSalesOrder(selected.id, approved, rejectionReason); setSelected(updated); setNotice(`${updated.orderNumber} ${approved ? "approved" : "rejected"}.`); await load(); }
    catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  };

  return <DashboardLayout title="Sales orders" subtitle="Create customer orders and track approval through fulfillment">
    <ModuleHeading eyebrow="SALES WORKSPACE" title="Order management" description="Order pricing and tax values shown after submission come from the backend." onRefresh={() => void load()} action={<button className="button button-primary" onClick={() => setCreating(true)}><Plus size={17} /> New order</button>} />
    {notice && <Notice onDismiss={() => setNotice("")}>{notice}</Notice>}{error && <ErrorBlock message={error} retry={() => void load()} />}
    <div className="metric-strip"><div><span>All orders</span><strong>{rows.length}</strong></div><div><span>Pending approval</span><strong>{rows.filter((order) => order.status === "PENDING_APPROVAL").length}</strong></div><div><span>Approved</span><strong>{rows.filter((order) => order.status === "APPROVED").length}</strong></div><div><span>Rejected</span><strong>{rows.filter((order) => order.status === "REJECTED").length}</strong></div></div>
    <section className="module-panel"><div className="table-toolbar"><SearchBox value={query} onChange={setQuery} placeholder="Search order, customer, or salesperson" /><select className="filter-select" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All statuses</option>{["PENDING_APPROVAL", "APPROVED", "REJECTED", "DISPATCHED"].map((value) => <option key={value}>{value}</option>)}</select><select className="filter-select" value={customerFilter} onChange={event=>setCustomerFilter(event.target.value)}><option value="">All customers</option>{customers.map(customer=><option key={customer.id} value={customer.id}>{customer.name}</option>)}</select><label className="compact-field">From<input type="date" value={startDate} onChange={event=>setStartDate(event.target.value)}/></label><label className="compact-field">To<input type="date" value={endDate} onChange={event=>setEndDate(event.target.value)}/></label><span className="record-count">{filtered.length} orders</span></div>
      {loading ? <LoadingBlock /> : error && !rows.length ? null : <DataTable rows={filtered as unknown as Array<Record<string, unknown>>} columns={[
        { label: "Order", render: (r) => <div className="primary-cell"><strong>{String(r.orderNumber)}</strong><small>{dateLabel(String(r.orderDate))}</small></div> },
        { label: "Customer", render: (r) => <div className="primary-cell"><strong>{String(r.customerName)}</strong><small>{String(r.customerCode || "")}</small></div> },
        { label: "Salesperson", render: (r) => String(r.salesmanName || "â€”") },
        { label: "Status", render: (r) => <StatusBadge value={String(r.status)} /> },
        { label: "Total", align: "right", render: (r) => money(Number(r.grandTotal)) },
        { label: "", align: "right", render: () => <span className="table-action"><Eye size={16} /> View</span> },
      ]} onRowClick={(r) => { const order = rows.find((item) => item.id === r.id); if (order) void showDetails(order); }} emptyTitle="No orders match these filters" />}
    </section>
    {creating && <Modal title="Create sales order" onClose={() => setCreating(false)} wide><form className="module-form" onSubmit={submitOrder}>{error&&<ErrorBlock message={error}/>}
      <div className="form-grid"><label>Customer<select required value={customerId} onChange={(e) => setCustomerId(e.target.value)}><option value="">Select customer</option>{customers.filter((customer) => customer.active).map((customer) => <option key={customer.id} value={customer.id}>{customer.name} Â· {customer.customerCode}</option>)}</select></label><label>Order date<input type="date" value={orderDate} onChange={(e) => setOrderDate(e.target.value)} /></label></div>
      <div className="form-section-title"><div><strong>Order items</strong><small>Backend uses each productâ€™s current selling price and GST rate.</small></div><button className="button button-secondary" type="button" onClick={() => setLines([...lines, { productId: 0, quantity: 1 }])}><Plus size={15} /> Add line</button></div>
      {lines.map((line, index) => { const product = products.find((item) => item.id === line.productId); return <div className="order-draft-line" key={index}><label>Product<select required value={line.productId || ""} onChange={(e) => setLines(lines.map((item, i) => i === index ? { ...item, productId: Number(e.target.value) } : item))}><option value="">Choose product</option>{products.map((item) => <option key={item.id} value={item.id}>{item.name} Â· {money(item.sellingPrice)} Â· GST {item.gstRate}%</option>)}</select></label><label>Quantity<input type="number" min="0.001" step="0.001" required value={line.quantity} onChange={(e) => setLines(lines.map((item, i) => i === index ? { ...item, quantity: Number(e.target.value) } : item))} /></label><div className="draft-total"><small>Estimated line total</small><strong>{product ? money(product.sellingPrice * line.quantity * (1 + product.gstRate / 100)) : money(0)}</strong></div>{lines.length > 1 && <button type="button" className="icon-button danger-icon" onClick={() => setLines(lines.filter((_, i) => i !== index))} aria-label="Remove line"><XIcon size={17} /></button>}</div>; })}
      <label>Notes<textarea rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Optional order notes" /></label><div className="preview-total"><span>Estimated total for preview</span><strong>{money(preview)}</strong></div><p className="form-hint">Preview is indicative. Submitted order totals are calculated by the server.</p><div className="form-footer"><button type="button" className="button button-secondary" onClick={() => setCreating(false)}>Cancel</button><button disabled={busy || !customerId || lines.some((line) => !line.productId)} className="button button-primary">{busy ? "Submittingâ€¦" : <><FilePlus2 size={16} /> Submit order</>}</button></div>
    </form></Modal>}
    {selected && <Modal title={`Order ${selected.orderNumber}`} onClose={() => setSelected(null)} wide><div className="detail-summary"><div><span>Customer</span><strong>{selected.customerName} Â· {selected.customerCode}</strong></div><div><span>Order date</span><strong>{dateLabel(selected.orderDate)}</strong></div><div><span>Status</span><StatusBadge value={selected.status} /></div><div><span>Grand total</span><strong>{money(selected.grandTotal)}</strong></div></div><div className="table-scroll"><table className="data-table"><thead><tr><th>Product</th><th>Qty</th><th>Unit price</th><th>GST</th><th className="align-right">Line total</th></tr></thead><tbody>{selected.items?.map((item) => <tr key={item.id || item.productId}><td>{item.productName}</td><td>{item.quantity}</td><td>{money(item.unitPrice)}</td><td>{item.gstRate}% Â· {money(item.gstAmount)}</td><td className="align-right">{money(item.totalAmount)}</td></tr>)}</tbody></table></div><div className="totals-row"><span>Taxable {money(selected.totalTaxableAmount)}</span><span>GST {money(selected.totalGstAmount)}</span><strong>Total {money(selected.grandTotal)}</strong></div>{selected.rejectionReason && <Notice kind="error">Rejection reason: {selected.rejectionReason}</Notice>}{canApprove && selected.status === "PENDING_APPROVAL" && <div className="form-footer"><button className="button button-danger" disabled={busy} onClick={() => void review(false)}><XIcon size={16} /> Reject</button><button className="button button-primary" disabled={busy} onClick={() => void review(true)}><Check size={16} /> Approve order</button></div>}<p className="form-hint">Approval is available to ADMIN and OWNER roles. The backend validates all actions.</p></Modal>}
  </DashboardLayout>;
}

