import api from "./axios";
import type { Product, Customer, Supplier, RawMaterial, PackagingMaterial } from "./masterData";
import type { SalesOrder } from "./sales";

export interface PurchaseItem { id?: number; itemType: "RAW_MATERIAL" | "PACKAGING_MATERIAL"; rawMaterialId?: number; rawMaterialName?: string; packagingMaterialId?: number; packagingMaterialName?: string; quantity: number; unitPrice: number; totalAmount?: number; }
export interface Purchase { id: number; purchaseNumber: string; supplierId: number; supplierName: string; supplierCode: string; purchaseDate: string; status: string; supplierInvoiceNo?: string; totalAmount: number; notes?: string; items: PurchaseItem[]; }
export interface BomItem { id?: number; itemType: "RAW_MATERIAL" | "PACKAGING_MATERIAL"; rawMaterialId?: number; rawMaterialName?: string; rawMaterialCode?: string; packagingMaterialId?: number; packagingMaterialName?: string; packagingMaterialCode?: string; standardQuantity: number; unit?: string; notes?: string; }
export interface Bom { id: number; bomCode: string; productId: number; productName: string; productCode?: string; batchSize: number; description?: string; version?: number; active: boolean; items: BomItem[]; }
export interface Consumption { itemType: "RAW_MATERIAL" | "PACKAGING_MATERIAL"; rawMaterialId?: number; packagingMaterialId?: number; actualQuantity: number; damagedQuantity?: number; }
export interface ProductionRun { id: number; runNumber: string; productId: number; productName: string; bomId: number; bomCode: string; runDate: string; batchMultiplier: number; expectedOutput: number; actualGoodOutput: number; yieldVariancePercentage: number; varianceFlag: boolean; labourCost: number; overheadCost: number; rawMaterialCost: number; packagingCost: number; totalProductionCost: number; costPerPacket: number; status: string; notes?: string; consumptions: Array<Consumption & { rawMaterialName?: string; packagingMaterialName?: string; plannedQuantity?: number; variancePercentage?: number; totalCost?: number }>; }
export interface StockRow { id: number; itemType: string; itemCode: string; itemName: string; categoryOrType?: string; unit: string; currentStock: number; valuationRate?: number; totalValuation?: number; reorderLevel?: number; lowStock: boolean; }
export interface StockMovement { id: number; movementCode: string; movementDate: string; movementType: string; itemType: string; itemId: number; itemCode: string; itemName: string; unit: string; quantity: number; unitPrice?: number; openingStock: number; closingStock: number; referenceType?: string; referenceNumber?: string; remarks?: string; createdByName?: string; }
export interface InvoiceItem { id: number; productId: number; productName: string; productCode: string; hsnCode?: string; quantity: number; unitPrice: number; taxableAmount: number; gstRate: number; cgstAmount: number; sgstAmount: number; igstAmount: number; totalAmount: number; }
export interface Invoice { id: number; invoiceNumber: string; salesOrderId: number; salesOrderNumber: string; customerId: number; customerName: string; customerCode: string; customerGstin?: string; placeOfSupply?: string; isInterState: boolean; invoiceDate: string; dueDate: string; taxableValue: number; cgstRate: number; cgstAmount: number; sgstRate: number; sgstAmount: number; igstRate: number; igstAmount: number; grandTotal: number; paidAmount: number; outstandingAmount: number; status: string; items: InvoiceItem[]; }
export interface Payment { id: number; paymentNumber: string; customerId: number; customerName: string; invoiceId?: number; invoiceNumber?: string; paymentDate: string; amount: number; paymentMode: string; referenceNumber?: string; notes?: string; remainingCustomerBalance?: number; remainingInvoiceBalance?: number; }

export const operationsApi = {
  purchases: async () => (await api.get<Purchase[]>("/purchases")).data,
  purchase: async (id: number) => (await api.get<Purchase>(`/purchases/${id}`)).data,
  createPurchase: async (body: { supplierId: number; purchaseDate?: string; supplierInvoiceNo?: string; notes?: string; items: PurchaseItem[] }) => (await api.post<Purchase>("/purchases", body)).data,
  boms: async () => (await api.get<Bom[]>("/boms")).data,
  bom: async (id: number) => (await api.get<Bom>(`/boms/${id}`)).data,
  activeBom: async (productId: number) => (await api.get<Bom>(`/boms/product/${productId}`)).data,
  createBom: async (body: Omit<Bom, "id" | "productName" | "productCode">) => (await api.post<Bom>("/boms", body)).data,
  updateBom: async (id: number, body: Omit<Bom, "id" | "productName" | "productCode">) => (await api.put<Bom>(`/boms/${id}`, body)).data,
  productionRuns: async () => (await api.get<ProductionRun[]>("/production")).data,
  createProductionRun: async (body: { productId: number; bomId?: number; batchMultiplier: number; actualGoodOutput: number; labourCost?: number; overheadCost?: number; runDate?: string; notes?: string; consumptions?: Consumption[] }) => (await api.post<ProductionRun>("/production", body)).data,
  stockSummary: async () => (await api.get<StockRow[]>("/inventory/summary")).data,
  stockLedger: async (params?: { start?: string; end?: string; itemType?: string }) => (await api.get<StockMovement[]>("/inventory/ledger", { params })).data,
  invoices: async () => (await api.get<Invoice[]>("/invoices")).data,
  invoice: async (id: number) => (await api.get<Invoice>(`/invoices/${id}`)).data,
  invoiceForOrder: async (id: number) => (await api.get<Invoice>(`/invoices/order/${id}`)).data,
  invoicePdf: async (id: number) => (await api.get<Blob>(`/invoices/${id}/pdf`, { responseType: "blob" })).data,
  payments: async () => (await api.get<Payment[]>("/payments")).data,
  paymentsByCustomer: async (id: number) => (await api.get<Payment[]>(`/payments/customer/${id}`)).data,
  createPayment: async (body: { customerId: number; invoiceId?: number; paymentDate?: string; amount: number; paymentMode: string; referenceNumber?: string; notes?: string }) => (await api.post<Payment>("/payments", body)).data,
  products: async () => (await api.get<Product[]>("/products")).data,
  customers: async () => (await api.get<Customer[]>("/customers")).data,
  suppliers: async () => (await api.get<Supplier[]>("/suppliers")).data,
  rawMaterials: async () => (await api.get<RawMaterial[]>("/raw-materials")).data,
  packagingMaterials: async () => (await api.get<PackagingMaterial[]>("/packaging-materials")).data,
  orders: async () => (await api.get<SalesOrder[]>("/orders")).data,
};
