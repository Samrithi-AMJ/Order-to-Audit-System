import api from "./axios";
import type { Customer, Product } from "./masterData";
export type { Customer, Product } from "./masterData";

export interface SalesOrderItem {
  id?: number;
  productId: number;
  productName?: string;
  productCode?: string;
  hsnCode?: string;
  quantity: number;
  unitPrice?: number;
  taxableAmount?: number;
  gstRate?: number;
  gstAmount?: number;
  totalAmount?: number;
}

export interface SalesOrder {
  id: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  customerCode: string;
  customerState: string;
  customerGstin?: string;
  orderDate: string;
  salesmanName: string;
  status: string;
  totalTaxableAmount: number;
  totalGstAmount: number;
  grandTotal: number;
  rejectionReason?: string;
  approvedByName?: string;
  approvedAt?: string;
  dispatchedAt?: string;
  notes?: string;
  items: SalesOrderItem[];
}

export interface CreateSalesOrder {
  customerId: number;
  orderDate?: string;
  notes?: string;
  items: Array<{ productId: number; quantity: number }>;
}

export const getCustomers = async () =>
  (await api.get<Customer[]>("/customers")).data;

export const getProducts = async () =>
  (await api.get<Product[]>("/products")).data;

export const getSalesOrders = async () =>
  (await api.get<SalesOrder[]>("/orders")).data;

export const createSalesOrder = async (order: CreateSalesOrder) =>
  (await api.post<SalesOrder>("/orders", order)).data;

export const getSalesOrder = async (id: number) =>
  (await api.get<SalesOrder>(`/orders/${id}`)).data;

export const getOrdersByCustomer = async (customerId: number) =>
  (await api.get<SalesOrder[]>(`/orders/customer/${customerId}`)).data;

export const approveSalesOrder = async (id: number, approved: boolean, rejectionReason?: string) =>
  (await api.post<SalesOrder>(`/orders/${id}/approval`, { approved, rejectionReason })).data;

export const dispatchSalesOrder = async (id: number, body: { dispatchNotes?: string; vehicleNumber?: string }) =>
  (await api.post<SalesOrder>(`/dispatch/${id}`, body)).data;
