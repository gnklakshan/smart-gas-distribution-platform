import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent } from "react";
import {
  Package, Send, History, User as UserIcon, CheckCircle2, LayoutDashboard,
  Users, Clock, TrendingUp, Scale, PhoneCall, Timer, BadgeCheck,
} from "lucide-react";
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis, Tooltip, CartesianGrid } from "recharts";
import { toast } from "sonner";
import { z } from "zod";
import { RoleGuard } from "@/components/RoleGuard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { StatusBadge } from "@/components/StatusBadge";
import { PaymentDialog } from "@/components/PaymentDialog";
import {
  api, type Allocation, type AllocationAnalytics, type CylinderType,
  type InventoryRecord, type Payment, type QueueAnalytics, type QueueEntry, type StockHistoryEntry,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/dealer")({
  validateSearch: z.object({
    tab: z.enum(["overview", "queue", "stock", "allocations", "profile"]).optional(),
  }),
  component: () => (<RoleGuard allow={["DEALER", "ADMIN"]}><DealerDashboard /></RoleGuard>),
});

function DealerDashboard() {
  const { user } = useAuth();
  const navigate = Route.useNavigate();
  const search = Route.useSearch();
  const tab = search.tab ?? "overview";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{user?.businessName || user?.name}</h1>
        <p className="text-muted-foreground">Demand, queue, and stock at a glance.</p>
      </div>
      <Tabs
        value={tab}
        onValueChange={(v) => navigate({ search: (prev) => ({ ...prev, tab: v as typeof tab }) })}
      >
        <TabsList>
          <TabsTrigger value="overview"><LayoutDashboard className="mr-2 h-4 w-4" />Overview</TabsTrigger>
          <TabsTrigger value="queue"><Users className="mr-2 h-4 w-4" />Queue</TabsTrigger>
          <TabsTrigger value="stock"><Package className="mr-2 h-4 w-4" />Stock</TabsTrigger>
          <TabsTrigger value="allocations"><History className="mr-2 h-4 w-4" />Allocations</TabsTrigger>
          <TabsTrigger value="profile"><UserIcon className="mr-2 h-4 w-4" />Profile</TabsTrigger>
        </TabsList>
        <TabsContent value="overview" className="mt-4"><OverviewTab /></TabsContent>
        <TabsContent value="queue" className="mt-4"><QueueTab /></TabsContent>
        <TabsContent value="stock" className="mt-4"><StockTab /></TabsContent>
        <TabsContent value="allocations" className="mt-4"><AllocationsTab /></TabsContent>
        <TabsContent value="profile" className="mt-4"><DealerProfile /></TabsContent>
      </Tabs>
    </div>
  );
}

// ── Overview ──────────────────────────────────────────────────────────────

function OverviewTab() {
  const { user } = useAuth();
  const [queueStats, setQueueStats] = useState<QueueAnalytics | null>(null);
  const [allocStats, setAllocStats] = useState<AllocationAnalytics | null>(null);
  const [inventory, setInventory] = useState<InventoryRecord[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    (async () => {
      setLoading(true);
      try {
        const [q, a, inv] = await Promise.all([
          api<QueueAnalytics>(`/api/v1/queue/dealer/${user.id}/analytics`),
          api<AllocationAnalytics>(`/api/v1/allocations/dealer/${user.id}/analytics`),
          api<InventoryRecord[]>(`/api/v1/inventory/dealer/${user.id}`),
        ]);
        setQueueStats(q);
        setAllocStats(a);
        setInventory(inv);
      } catch (e) {
        toast.error(e instanceof Error ? e.message : "Failed to load overview");
      } finally { setLoading(false); }
    })();
  }, [user?.id]);

  if (loading) return <p className="text-sm text-muted-foreground">Loading overview…</p>;

  const totalStock = inventory.reduce((sum, i) => sum + i.availableStock, 0);
  const fairnessDelta = allocStats ? allocStats.last30DaysApprovedQty - allocStats.platformAvg30DaysApprovedQty : 0;

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={Package} label="Total stock" value={totalStock} />
        <StatCard icon={Users} label="Waiting in queue" value={queueStats?.waitingCount ?? 0} />
        <StatCard icon={CheckCircle2} label="Completed today" value={queueStats?.completedToday ?? 0} />
        <StatCard
          icon={TrendingUp}
          label="Fulfillment rate"
          value={`${allocStats?.fulfillmentRatePct ?? 0}%`}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><TrendingUp className="h-4 w-4" />Allocation demand</CardTitle>
            <CardDescription>Your requests to the platform, by outcome.</CardDescription>
          </CardHeader>
          <CardContent>
            {allocStats && (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart
                  data={[
                    { name: "Requested", value: allocStats.totalRequested },
                    { name: "Approved", value: allocStats.totalApproved },
                    { name: "Rejected", value: allocStats.totalRejected },
                    { name: "Pending", value: allocStats.pendingCount },
                  ]}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" fontSize={12} />
                  <YAxis fontSize={12} allowDecimals={false} />
                  <Tooltip />
                  <Bar dataKey="value" radius={[4, 4, 0, 0]} fill="hsl(var(--primary))" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><Scale className="h-4 w-4" />Fair distribution</CardTitle>
            <CardDescription>How your last 30 days compares to the platform average.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {allocStats && (
              <>
                <div className="flex items-baseline justify-between">
                  <span className="text-sm text-muted-foreground">You received</span>
                  <span className="text-2xl font-bold">{allocStats.last30DaysApprovedQty}</span>
                </div>
                <Bar30 value={allocStats.last30DaysApprovedQty} max={Math.max(allocStats.last30DaysApprovedQty, allocStats.platformAvg30DaysApprovedQty, 1)} color="bg-primary" />
                <div className="flex items-baseline justify-between">
                  <span className="text-sm text-muted-foreground">Platform average</span>
                  <span className="text-2xl font-bold text-muted-foreground">{allocStats.platformAvg30DaysApprovedQty}</span>
                </div>
                <Bar30 value={allocStats.platformAvg30DaysApprovedQty} max={Math.max(allocStats.last30DaysApprovedQty, allocStats.platformAvg30DaysApprovedQty, 1)} color="bg-muted-foreground/50" />
                <p className="text-xs text-muted-foreground pt-1">
                  {fairnessDelta >= 0
                    ? `You're receiving ${fairnessDelta.toFixed(1)} more units than average over the last 30 days.`
                    : `You're receiving ${Math.abs(fairnessDelta).toFixed(1)} fewer units than average over the last 30 days.`}
                </p>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Bar30({ value, max, color }: { value: number; max: number; color: string }) {
  const pct = max === 0 ? 0 : Math.min(100, (value / max) * 100);
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
      <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
    </div>
  );
}

function StatCard({ icon: Icon, label, value }: { icon: typeof Package; label: string; value: string | number }) {
  return (
    <Card>
      <CardContent className="flex items-center gap-3 pt-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
          <Icon className="h-5 w-5 text-primary" />
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
          <p className="text-2xl font-bold">{value}</p>
        </div>
      </CardContent>
    </Card>
  );
}

// ── Queue ─────────────────────────────────────────────────────────────────

function QueueTab() {
  const { user } = useAuth();
  const [stats, setStats] = useState<QueueAnalytics | null>(null);
  const [entries, setEntries] = useState<QueueEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [s, e] = await Promise.all([
        api<QueueAnalytics>(`/api/v1/queue/dealer/${user.id}/analytics`),
        api<QueueEntry[]>(`/api/v1/queue/dealer/${user.id}`),
      ]);
      setStats(s);
      setEntries(e.filter((q) => q.status === "WAITING" || q.status === "READY_FOR_PICKUP"));
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Failed to load queue");
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [user?.id]);

  const callNext = async () => {
    if (!user) return;
    setBusyId("call-next");
    try {
      await api(`/api/v1/queue/dealer/${user.id}/call-next`, { method: "PUT" });
      toast.success("Next citizen called");
      load();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "No one is waiting");
    } finally { setBusyId(null); }
  };

  const complete = async (id: string) => {
    setBusyId(id);
    try {
      await api(`/api/v1/queue/${id}/complete`, { method: "PUT" });
      toast.success("Pickup completed");
      load();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Action failed");
    } finally { setBusyId(null); }
  };

  const waiting = entries.filter((e) => e.status === "WAITING");
  const ready = entries.filter((e) => e.status === "READY_FOR_PICKUP");

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard icon={Users} label="Waiting" value={stats?.waitingCount ?? 0} />
        <StatCard icon={BadgeCheck} label="Ready for pickup" value={stats?.readyCount ?? 0} />
        <StatCard icon={Timer} label="Avg wait (30d)" value={stats?.avgWaitMinutes != null ? `${Math.round(stats.avgWaitMinutes)} min` : "—"} />
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Live queue</CardTitle>
            <CardDescription>Citizens currently waiting or ready for pickup.</CardDescription>
          </div>
          <Button onClick={callNext} disabled={busyId === "call-next" || waiting.length === 0}>
            <PhoneCall className="mr-2 h-4 w-4" />
            {busyId === "call-next" ? "Calling…" : "Call next"}
          </Button>
        </CardHeader>
        <CardContent>
          {loading ? <p className="text-sm text-muted-foreground">Loading…</p> : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Token</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Waiting since</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {entries.length === 0 && (
                    <TableRow><TableCell colSpan={4} className="text-center text-sm text-muted-foreground py-6">No one in queue right now.</TableCell></TableRow>
                  )}
                  {[...ready, ...waiting].map((q) => (
                    <TableRow key={q.id}>
                      <TableCell className="font-mono text-xs">{q.tokenNumber}</TableCell>
                      <TableCell>
                        <Badge variant="secondary" className={q.status === "READY_FOR_PICKUP" ? "bg-emerald-100 text-emerald-800" : "bg-amber-100 text-amber-800"}>
                          {q.status === "READY_FOR_PICKUP" ? "Ready" : "Waiting"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-xs">{new Date(q.requestedAt).toLocaleString()}</TableCell>
                      <TableCell className="text-right">
                        {q.status === "READY_FOR_PICKUP" && (
                          <Button size="sm" onClick={() => complete(q.id)} disabled={busyId === q.id}>
                            <CheckCircle2 className="mr-1 h-3.5 w-3.5" /> Complete
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

// ── Stock ─────────────────────────────────────────────────────────────────

function StockTab() {
  const { user } = useAuth();
  const [items, setItems] = useState<InventoryRecord[]>([]);
  const [history, setHistory] = useState<StockHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [edits, setEdits] = useState<Record<string, string>>({});
  const [savingId, setSavingId] = useState<string | null>(null);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [inv, hist] = await Promise.all([
        api<InventoryRecord[]>(`/api/v1/inventory/dealer/${user.id}`),
        api<StockHistoryEntry[]>(`/api/v1/inventory/dealer/${user.id}/stock-history`),
      ]);
      setItems(inv);
      setEdits(Object.fromEntries(inv.map((i) => [i.id, String(i.availableStock)])));
      setHistory(hist);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Failed to load inventory");
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [user?.id]);

  const save = async (id: string) => {
    setSavingId(id);
    try {
      await api(`/api/v1/inventory/${id}/stock`, {
        method: "PUT", body: JSON.stringify({ availableStock: Number(edits[id]) }),
      });
      toast.success("Stock updated");
      load();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Update failed");
    } finally { setSavingId(null); }
  };

  if (loading) return <p className="text-sm text-muted-foreground">Loading inventory…</p>;
  if (items.length === 0) return (
    <Card><CardContent className="py-8 text-center text-sm text-muted-foreground">
      No inventory records yet. Ask an admin to create one for your cylinder types.
    </CardContent></Card>
  );

  return (
    <div className="space-y-4">
      <div className="grid gap-4 md:grid-cols-2">
        {items.map((inv) => (
          <Card key={inv.id}>
            <CardHeader>
              <CardTitle>{inv.cylinderTypeName}</CardTitle>
              <CardDescription>Last updated {inv.lastUpdated ? new Date(inv.lastUpdated).toLocaleString() : "—"}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="rounded-xl bg-gradient-to-br from-primary to-primary/70 p-5 text-primary-foreground">
                <p className="text-xs uppercase tracking-wide opacity-80">Available stock</p>
                <p className="mt-1 text-4xl font-bold">{inv.availableStock}</p>
              </div>
              <div className="flex items-end gap-2">
                <div className="flex-1 space-y-2">
                  <Label htmlFor={`stock-${inv.id}`}>Update stock</Label>
                  <Input
                    id={`stock-${inv.id}`}
                    type="number"
                    min={0}
                    value={edits[inv.id] ?? ""}
                    onChange={(e) => setEdits((prev) => ({ ...prev, [inv.id]: e.target.value }))}
                  />
                </div>
                <Button onClick={() => save(inv.id)} disabled={savingId === inv.id}>
                  {savingId === inv.id ? "Saving…" : "Save"}
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Stock change history</CardTitle>
          <CardDescription>Last 30 days of stock movement, across all cylinder types.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Cylinder type</TableHead>
                  <TableHead>Change</TableHead>
                  <TableHead>New stock</TableHead>
                  <TableHead>Reason</TableHead>
                  <TableHead>When</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {history.length === 0 && (
                  <TableRow><TableCell colSpan={5} className="text-center text-sm text-muted-foreground py-6">No stock changes recorded yet.</TableCell></TableRow>
                )}
                {history.map((h) => (
                  <TableRow key={h.id}>
                    <TableCell>{h.cylinderTypeName}</TableCell>
                    <TableCell className={h.change >= 0 ? "text-emerald-600 font-medium" : "text-rose-600 font-medium"}>
                      {h.change >= 0 ? `+${h.change}` : h.change}
                    </TableCell>
                    <TableCell>{h.newStock}</TableCell>
                    <TableCell className="text-xs text-muted-foreground">{h.reason.replace(/_/g, " ")}</TableCell>
                    <TableCell className="text-xs">{new Date(h.changedAt).toLocaleString()}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ── Allocations ───────────────────────────────────────────────────────────

function AllocationsTab() {
  const { user } = useAuth();
  const [items, setItems] = useState<Allocation[]>([]);
  const [cylinderTypes, setCylinderTypes] = useState<CylinderType[]>([]);
  const [cylinderTypeId, setCylinderTypeId] = useState("");
  const [qty, setQty] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [paidAllocationIds, setPaidAllocationIds] = useState<Set<string>>(new Set());
  const [payingAllocationId, setPayingAllocationId] = useState<string | null>(null);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [allocs, types] = await Promise.all([
        api<Allocation[]>(`/api/v1/allocations/dealer/${user.id}`),
        api<CylinderType[]>(`/api/v1/cylinder-types`),
      ]);
      setItems(allocs);
      setCylinderTypes(types);

      const approved = allocs.filter((a) => a.status === "APPROVED");
      const results = await Promise.all(
        approved.map((a) =>
          api<Payment>(`/api/v1/payments/allocations/${a.id}`).catch(() => null),
        ),
      );
      setPaidAllocationIds(new Set(
        results
          .filter((p): p is Payment => p !== null && p.status === "PAID")
          .map((p) => p.allocationId),
      ));
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Failed to load allocations");
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [user?.id]);

  const request = async (e: FormEvent) => {
    e.preventDefault();
    if (!cylinderTypeId) { toast.error("Select a cylinder type"); return; }
    setSubmitting(true);
    try {
      await api(`/api/v1/allocations/request`, {
        method: "POST",
        body: JSON.stringify({ cylinderTypeId, requestedQuantity: Number(qty) }),
      });
      toast.success("Allocation request submitted");
      setQty("");
      load();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Request failed");
    } finally { setSubmitting(false); }
  };

  const confirm = async (id: string) => {
    try {
      await api(`/api/v1/allocations/${id}/confirm`, { method: "PUT" });
      toast.success("Delivery confirmed");
      load();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Confirm failed");
    }
  };

  const cylinderTypeName = (id: string) => cylinderTypes.find((c) => c.id === id)?.name ?? id.slice(0, 8) + "…";

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Request allocation</CardTitle>
          <CardDescription>Request more cylinders from the platform.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={request} className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="flex-1 space-y-2">
              <Label>Cylinder type</Label>
              <Select value={cylinderTypeId} onValueChange={setCylinderTypeId}>
                <SelectTrigger><SelectValue placeholder="Select cylinder type" /></SelectTrigger>
                <SelectContent>
                  {cylinderTypes.map((c) => (
                    <SelectItem key={c.id} value={c.id}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex-1 space-y-2">
              <Label htmlFor="qty">Quantity</Label>
              <Input id="qty" type="number" min={1} value={qty} onChange={(e) => setQty(e.target.value)} required />
            </div>
            <Button type="submit" disabled={submitting}>
              <Send className="mr-2 h-4 w-4" /> {submitting ? "Submitting…" : "Submit request"}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>My allocations</CardTitle></CardHeader>
        <CardContent>
          {loading ? <p className="text-sm text-muted-foreground">Loading…</p> : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Cylinder type</TableHead>
                    <TableHead>Requested</TableHead>
                    <TableHead>Approved</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Date</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.length === 0 && (
                    <TableRow><TableCell colSpan={6} className="text-center text-sm text-muted-foreground py-6">No allocations yet.</TableCell></TableRow>
                  )}
                  {items.map((a) => (
                    <TableRow key={a.id}>
                      <TableCell>{cylinderTypeName(a.cylinderTypeId)}</TableCell>
                      <TableCell>{a.requestedQuantity}</TableCell>
                      <TableCell>{a.approvedQuantity ?? "—"}</TableCell>
                      <TableCell><StatusBadge status={a.status} /></TableCell>
                      <TableCell className="text-xs">{new Date(a.requestedAt).toLocaleString()}</TableCell>
                      <TableCell className="text-right">
                        {a.status === "APPROVED" && (
                          paidAllocationIds.has(a.id) ? (
                            <Button size="sm" onClick={() => confirm(a.id)}>
                              <CheckCircle2 className="mr-1 h-3.5 w-3.5" /> Confirm
                            </Button>
                          ) : (
                            <Button size="sm" variant="outline" onClick={() => setPayingAllocationId(a.id)}>
                              Pay now
                            </Button>
                          )
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {payingAllocationId && (
        <PaymentDialog
          allocationId={payingAllocationId}
          open={!!payingAllocationId}
          onOpenChange={(open) => { if (!open) setPayingAllocationId(null); }}
          onPaid={() => { setPayingAllocationId(null); load(); }}
        />
      )}
    </div>
  );
}

function DealerProfile() {
  const { user } = useAuth();
  if (!user) return null;
  return (
    <Card>
      <CardHeader><CardTitle>Profile</CardTitle></CardHeader>
      <CardContent className="grid gap-3 sm:grid-cols-2">
        <Field label="Name" value={user.name} />
        <Field label="Business" value={user.businessName || "—"} />
        <Field label="NIC" value={user.nic} />
        <Field label="Email" value={user.email} />
        <Field label="Phone" value={user.phone || "—"} />
        <Field label="Address" value={user.address || "—"} />
        <Field label="Reg No" value={user.businessRegNo || "—"} />
        <Field label="User ID" value={user.id} />
      </CardContent>
    </Card>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-card p-3">
      <p className="text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-medium break-all">{value}</p>
    </div>
  );
}
