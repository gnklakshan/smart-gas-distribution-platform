const configuredApiBase = import.meta.env.VITE_API_BASE_URL?.trim();

export const API_BASE = configuredApiBase ?? "";

export type Role = "CITIZEN" | "DEALER" | "ADMIN";

export interface User {
  id: string;
  nic: string;
  email: string;
  name: string;
  role: Role;
  phone?: string;
  address?: string;
  businessName?: string;
  businessRegNo?: string;
  createdAt?: string;
}

export interface Inventory {
  id: string;
  dealerId: string;
  dealerName: string;
  address: string;
  latitude: number;
  longitude: number;
  availableStock: number;
  lastUpdated?: string;
  distanceKm?: number;
}

/** Per-cylinder-type inventory record, as actually returned by GET /api/v1/inventory/dealer/{id} */
export interface InventoryRecord {
  id: string;
  dealerId: string;
  cylinderTypeId: string;
  cylinderTypeName: string;
  availableStock: number;
  lastUpdated?: string;
  dealerName?: string | null;
  address?: string | null;
  latitude?: number | null;
  longitude?: number | null;
}

export interface CylinderType {
  id: string;
  name: string;
  capacityKg: number;
}

export type AllocationStatus = "PENDING" | "APPROVED" | "REJECTED" | "DELIVERED";

export interface Allocation {
  id: string;
  dealerId: string;
  cylinderTypeId: string;
  requestedQuantity: number;
  approvedQuantity: number | null;
  status: AllocationStatus;
  rejectionReason?: string;
  requestedAt: string;
  resolvedAt?: string;
  deliveredAt?: string;
}

export interface AllocationAnalytics {
  totalRequested: number;
  totalApproved: number;
  totalRejected: number;
  pendingCount: number;
  fulfillmentRatePct: number;
  last30DaysApprovedQty: number;
  platformAvg30DaysApprovedQty: number;
}

export type QueueStatus = "WAITING" | "READY_FOR_PICKUP" | "COMPLETED" | "CANCELLED";

export interface QueueEntry {
  id: string;
  userId: string;
  dealerId: string;
  cylinderTypeId: string;
  tokenNumber: string;
  status: QueueStatus;
  requestedAt: string;
  fulfilledAt?: string | null;
}

export interface QueueAnalytics {
  waitingCount: number;
  readyCount: number;
  completedToday: number;
  avgWaitMinutes: number | null;
}

export type StockChangeReason = "MANUAL_UPDATE" | "ALLOCATION_CONFIRMED" | "QUEUE_COMPLETED";

export interface StockHistoryEntry {
  id: string;
  cylinderTypeId: string;
  cylinderTypeName: string;
  previousStock: number;
  newStock: number;
  change: number;
  reason: StockChangeReason;
  changedAt: string;
}

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("token");
}

export async function api<T = unknown>(
  path: string,
  options: RequestInit & { auth?: boolean } = {},
): Promise<T> {
  const { auth = true, headers, ...rest } = options;
  const h: Record<string, string> = {
    "Content-Type": "application/json",
    ...(headers as Record<string, string> | undefined),
  };
  if (auth) {
    const token = getToken();
    if (token) h["Authorization"] = `Bearer ${token}`;
  }
  const res = await fetch(`${API_BASE}${path}`, { ...rest, headers: h });
  const text = await res.text();
  const data = text ? (() => { try { return JSON.parse(text); } catch { return text; } })() : null;
  if (!res.ok) {
    const msg = (data && typeof data === "object" && "message" in data
      ? (data as { message?: string }).message
      : null) || (typeof data === "string" ? data : null) || `Request failed (${res.status})`;
    throw new Error(msg);
  }
  return data as T;
}
