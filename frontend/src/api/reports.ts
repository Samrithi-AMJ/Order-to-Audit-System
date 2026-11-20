import api from "./axios";

export interface DashboardSummary {
  totalSales: number;
  totalReceivables: number;
  totalPurchases: number;
  pendingOrdersCount: number;
  activeProductionRunsCount: number;
  lowStockAlertsCount: number;
  rawMaterialStockValuation: number;
  packagingStockValuation: number;
  finishedGoodsStockValuation: number;
  categoryWiseSales: Record<string, number>;
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const response = await api.get<DashboardSummary>("/reports/dashboard");
  return response.data;
}

export interface GstLine {
  invoiceNumber: string; invoiceDate: string; customerName: string; customerGstin?: string;
  placeOfSupply?: string; interState: boolean; taxableValue: number; cgstAmount: number;
  sgstAmount: number; igstAmount: number; invoiceTotal: number;
}
export interface GstSummary {
  financialYear: string; totalTaxableValue: number; totalCgst: number; totalSgst: number;
  totalIgst: number; totalGstCollected: number; totalGrossInvoiced: number; lineItems: GstLine[];
}
export interface ProductionVariance {
  runNumber: string; productName: string; runDate: string; expectedOutput: number;
  actualGoodOutput: number; yieldVariancePercentage: number; yieldVarianceFlagged: boolean;
  totalCost: number; costPerPacket: number;
  items: Array<{ materialName: string; materialType: string; plannedQuantity: number; actualQuantity: number; damagedQuantity: number; variancePercentage: number; flagged: boolean; totalCost: number }>;
}
export const getGstSummary = async (startDate?: string, endDate?: string) =>
  (await api.get<GstSummary>("/reports/gst-summary", { params: { startDate, endDate } })).data;
export const getProductionVariance = async () =>
  (await api.get<ProductionVariance[]>("/reports/production-variance")).data;
export const downloadReport = async (kind: "stock-ledger" | "sales-register", format: "excel" | "csv") => {
  const response = await api.get<Blob>(`/reports/export/${kind}/${format}`, { responseType: "blob" });
  return response.data;
};
