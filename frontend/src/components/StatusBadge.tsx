import type { AllocationStatus } from "@/lib/api";
import { Badge } from "@/components/ui/badge";

const styles: Record<AllocationStatus, string> = {
  PENDING: "bg-amber-100 text-amber-800 hover:bg-amber-100",
  APPROVED: "bg-emerald-100 text-emerald-800 hover:bg-emerald-100",
  REJECTED: "bg-rose-100 text-rose-800 hover:bg-rose-100",
  DELIVERED: "bg-sky-100 text-sky-800 hover:bg-sky-100",
};

export function StatusBadge({ status }: { status: AllocationStatus }) {
  return <Badge variant="secondary" className={styles[status]}>{status}</Badge>;
}
