import api from "./axios";

export interface Customer { id: number; customerCode: string; name: string; contactPerson?: string; email?: string; phone?: string; address?: string; state: string; stateCode?: string; gstin?: string; creditLimit?: number; outstandingBalance?: number; assignedSalesmanId?: number; assignedSalesmanName?: string; active: boolean; }
export interface Product { id: number; productCode: string; name: string; category: string; unit: string; packetWeightGrams?: number; hsnCode?: string; sellingPrice: number; gstRate: number; currentStock?: number; reorderLevel?: number; active: boolean; }
export interface Supplier { id: number; supplierCode: string; name: string; contactPerson?: string; email?: string; phone?: string; address?: string; state?: string; gstin?: string; active: boolean; }
export interface RawMaterial { id: number; materialCode: string; name: string; category: string; unit: string; currentStock?: number; weightedAvgCost?: number; reorderLevel?: number; active: boolean; }
export interface PackagingMaterial { id: number; materialCode: string; name: string; type: string; unit: string; currentStock?: number; unitCost?: number; reorderLevel?: number; active: boolean; }

export const masterDataApi = {
  customers: async () => (await api.get<Customer[]>("/customers")).data,
  customer: async (id: number) => (await api.get<Customer>(`/customers/${id}`)).data,
  createCustomer: async (body: Partial<Customer>) => (await api.post<Customer>("/customers", body)).data,
  updateCustomer: async (id: number, body: Partial<Customer>) => (await api.put<Customer>(`/customers/${id}`, body)).data,
  products: async () => (await api.get<Product[]>("/products")).data,
  product: async (id: number) => (await api.get<Product>(`/products/${id}`)).data,
  createProduct: async (body: Partial<Product>) => (await api.post<Product>("/products", body)).data,
  updateProduct: async (id: number, body: Partial<Product>) => (await api.put<Product>(`/products/${id}`, body)).data,
  suppliers: async () => (await api.get<Supplier[]>("/suppliers")).data,
  supplier: async (id: number) => (await api.get<Supplier>(`/suppliers/${id}`)).data,
  createSupplier: async (body: Partial<Supplier>) => (await api.post<Supplier>("/suppliers", body)).data,
  rawMaterials: async () => (await api.get<RawMaterial[]>("/raw-materials")).data,
  createRawMaterial: async (body: Partial<RawMaterial>) => (await api.post<RawMaterial>("/raw-materials", body)).data,
  packagingMaterials: async () => (await api.get<PackagingMaterial[]>("/packaging-materials")).data,
  createPackagingMaterial: async (body: Partial<PackagingMaterial>) => (await api.post<PackagingMaterial>("/packaging-materials", body)).data,
};
