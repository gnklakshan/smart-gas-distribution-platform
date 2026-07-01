import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent } from "react";
import { Users, Boxes, ClipboardList, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { z } from "zod";
import { RoleGuard } from "@/components/RoleGuard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { StatusBadge } from "@/components/StatusBadge";
import { api, type Allocation, type AllocationStatus, type Inventory, type Role, type User } from "@/lib/api";

export const Route = createFileRoute("/admin")({
  validateSearch: z.object({
    tab: z.enum(["allocations", "users", "inventory"]).optional(),
  }),
  component: () => (<RoleGuard allow={["ADMIN"]}><AdminDashboard /></RoleGuard>),
});

function AdminDashboard() {
  const navigate = Route.useNavigate();
  const search = Route.useSearch();
  const tab = search.tab ?? "allocations";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Admin Console</h1>
        <p className="text-muted-foreground">
          Monitor allocations, manage accounts, and maintain inventory records.
        </p>
      </div>
      <Tabs
        value={tab}
        onValueChange={(v) => navigate({ search: (prev) => ({ ...prev, tab: v as typeof tab }) })}
      >
        <TabsList>
          <TabsTrigger value="allocations"><ClipboardList className="mr-2 h-4 w-4" />Allocations</TabsTrigger>
          <TabsTrigger value="users"><Users className="mr-2 h-4 w-4" />Users</TabsTrigger>
          <TabsTrigger value="inventory"><Boxes className="mr-2 h-4 w-4" />Inventory</TabsTrigger>
        </TabsList>
        <TabsContent value="allocations" className="mt-4"><AllocationsAdmin /></TabsContent>
        <TabsContent value="users" className="mt-4"><UsersAdmin /></TabsContent>
        <TabsContent value="inventory" className="mt-4"><InventoryAdmin /></TabsContent>
      </Tabs>
    </div>
  );
}

/* -------------------- Allocations -------------------- */
function AllocationsAdmin() {
  const [status, setStatus] = useState<AllocationStatus>("PENDING");
  const [items, setItems] = useState<Allocation[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const data = status === "PENDING"
        ? await api<Allocation[]>(`/api/v1/allocations/pending`)
        : await api<Allocation[]>(`/api/v1/allocations?status=${status}`);
      setItems(data);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Failed to load allocations");
    } finally { setLoading(false); }
  };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [status]);

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <div>
          <CardTitle>Allocation requests</CardTitle>
          <CardDescription>Review and act on dealer allocation requests.</CardDescription>
        </div>
        <Tabs value={status} onValueChange={(v) => setStatus(v as AllocationStatus)}>
          <TabsList>
            <TabsTrigger value="PENDING">Pending</TabsTrigger>
            <TabsTrigger value="APPROVED">Approved</TabsTrigger>
            <TabsTrigger value="REJECTED">Rejected</TabsTrigger>
            <TabsTrigger value="DELIVERED">Delivered</TabsTrigger>
          </TabsList>
        </Tabs>
      </CardHeader>
      <CardContent>
        {loading ? <p className="text-sm text-muted-foreground">Loading…</p> : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader><TableRow>
                <TableHead>ID</TableHead><TableHead>Dealer</TableHead><TableHead>Requested</TableHead>
                <TableHead>Approved</TableHead><TableHead>Status</TableHead><TableHead>Date</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow></TableHeader>
              <TableBody>
                {items.length === 0 && (
                  <TableRow><TableCell colSpan={7} className="text-center text-sm text-muted-foreground py-6">No allocations.</TableCell></TableRow>
                )}
                {items.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell className="font-mono text-xs">{a.id.slice(0, 8)}…</TableCell>
                    <TableCell>{a.dealerName}</TableCell>
                    <TableCell>{a.requestedQuantity}</TableCell>
                    <TableCell>{a.approvedQuantity ?? "—"}</TableCell>
                    <TableCell><StatusBadge status={a.status} /></TableCell>
                    <TableCell className="text-xs">{new Date(a.requestedAt).toLocaleString()}</TableCell>
                    <TableCell className="text-right">
                      {a.status === "PENDING" && (
                        <div className="flex justify-end gap-2">
                          <ApproveDialog allocation={a} onDone={load} />
                          <RejectDialog allocation={a} onDone={load} />
                        </div>
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
  );
}

function ApproveDialog({ allocation, onDone }: { allocation: Allocation; onDone: () => void }) {
  const [open, setOpen] = useState(false);
  const [qty, setQty] = useState(String(allocation.requestedQuantity));
  const [loading, setLoading] = useState(false);
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api(`/api/v1/allocations/${allocation.id}/approve`, {
        method: "PUT", body: JSON.stringify({ approvedQuantity: Number(qty) }),
      });
      toast.success("Allocation approved");
      setOpen(false); onDone();
    } catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
    finally { setLoading(false); }
  };
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button size="sm">Approve</Button></DialogTrigger>
      <DialogContent>
        <DialogHeader><DialogTitle>Approve allocation</DialogTitle>
          <DialogDescription>Dealer {allocation.dealerName} requested {allocation.requestedQuantity} units.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <div className="space-y-2"><Label>Approved quantity</Label>
            <Input type="number" min={1} value={qty} onChange={(e) => setQty(e.target.value)} required />
          </div>
          <DialogFooter><Button type="submit" disabled={loading}>{loading ? "Approving…" : "Approve"}</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function RejectDialog({ allocation, onDone }: { allocation: Allocation; onDone: () => void }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api(`/api/v1/allocations/${allocation.id}/reject`, {
        method: "PUT", body: JSON.stringify({ reason }),
      });
      toast.success("Allocation rejected");
      setOpen(false); onDone();
    } catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
    finally { setLoading(false); }
  };
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button size="sm" variant="destructive">Reject</Button></DialogTrigger>
      <DialogContent>
        <DialogHeader><DialogTitle>Reject allocation</DialogTitle>
          <DialogDescription>Provide a reason for {allocation.dealerName}.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <div className="space-y-2"><Label>Reason</Label>
            <Textarea value={reason} onChange={(e) => setReason(e.target.value)} required rows={3} />
          </div>
          <DialogFooter><Button type="submit" variant="destructive" disabled={loading}>{loading ? "Rejecting…" : "Reject"}</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/* -------------------- Users -------------------- */
const roleBadge: Record<Role, string> = {
  CITIZEN: "bg-sky-100 text-sky-700",
  DEALER: "bg-violet-100 text-violet-700",
  ADMIN: "bg-rose-100 text-rose-700",
};

function UsersAdmin() {
  const [role, setRole] = useState<Role>("DEALER");
  const [items, setItems] = useState<User[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const data = await api<User[]>(`/api/v1/users/role/${role}`);
      setItems(data);
    } catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [role]);

  const remove = async (id: string) => {
    if (!confirm("Delete this user?")) return;
    try {
      await api(`/api/v1/users/${id}`, { method: "DELETE" });
      toast.success("User deleted"); load();
    } catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
  };

  const filtered = items.filter((u) =>
    !search || u.email.toLowerCase().includes(search.toLowerCase()) || u.name.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-4 flex-wrap">
        <div>
          <CardTitle>Users</CardTitle>
          <CardDescription>Manage platform accounts.</CardDescription>
        </div>
        <div className="flex items-center gap-2">
          <Tabs value={role} onValueChange={(v) => setRole(v as Role)}>
            <TabsList>
              <TabsTrigger value="CITIZEN">Citizens</TabsTrigger>
              <TabsTrigger value="DEALER">Dealers</TabsTrigger>
              <TabsTrigger value="ADMIN">Admins</TabsTrigger>
            </TabsList>
          </Tabs>
          <RegisterDealerDialog onDone={load} />
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <Input placeholder="Search by name or email…" value={search} onChange={(e) => setSearch(e.target.value)} />
        {loading ? <p className="text-sm text-muted-foreground">Loading…</p> : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader><TableRow>
                <TableHead>Name</TableHead><TableHead>NIC</TableHead><TableHead>Email</TableHead>
                <TableHead>Role</TableHead><TableHead>Created</TableHead><TableHead className="text-right">Actions</TableHead>
              </TableRow></TableHeader>
              <TableBody>
                {filtered.length === 0 && (
                  <TableRow><TableCell colSpan={6} className="text-center text-sm text-muted-foreground py-6">No users.</TableCell></TableRow>
                )}
                {filtered.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell>{u.name}</TableCell>
                    <TableCell>{u.nic}</TableCell>
                    <TableCell className="text-xs">{u.email}</TableCell>
                    <TableCell><Badge variant="secondary" className={roleBadge[u.role]}>{u.role}</Badge></TableCell>
                    <TableCell className="text-xs">{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}</TableCell>
                    <TableCell className="text-right">
                      <Button size="icon" variant="ghost" onClick={() => remove(u.id)}>
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function RegisterDealerDialog({ onDone }: { onDone: () => void }) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    nic: "", email: "", password: "", name: "", phone: "", address: "", businessName: "", businessRegNo: "",
  });
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api(`/api/v1/users/register/dealer`, { method: "POST", body: JSON.stringify(form) });
      toast.success("Dealer registered");
      setOpen(false); onDone();
      setForm({ nic: "", email: "", password: "", name: "", phone: "", address: "", businessName: "", businessRegNo: "" });
    } catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
    finally { setLoading(false); }
  };
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button><Plus className="mr-2 h-4 w-4" />Register dealer</Button></DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader><DialogTitle>Register a new dealer</DialogTitle></DialogHeader>
        <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2">
          <FormField label="Full name" value={form.name} onChange={set("name")} required />
          <FormField label="Business name" value={form.businessName} onChange={set("businessName")} required />
          <FormField label="NIC" value={form.nic} onChange={set("nic")} required />
          <FormField label="Business reg no" value={form.businessRegNo} onChange={set("businessRegNo")} />
          <FormField label="Email" type="email" value={form.email} onChange={set("email")} required />
          <FormField label="Phone" value={form.phone} onChange={set("phone")} />
          <FormField label="Address" value={form.address} onChange={set("address")} className="sm:col-span-2" />
          <FormField label="Password" type="password" value={form.password} onChange={set("password")} required className="sm:col-span-2" />
          <DialogFooter className="sm:col-span-2">
            <Button type="submit" disabled={loading}>{loading ? "Creating…" : "Create dealer"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function FormField({ label, className, ...props }: { label: string; className?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div className={`space-y-2 ${className || ""}`}>
      <Label>{label}</Label>
      <Input {...props} />
    </div>
  );
}

/* -------------------- Inventory -------------------- */
function InventoryAdmin() {
  const [items, setItems] = useState<Inventory[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const data = await api<Inventory[]>(`/api/v1/inventory/available`);
      setItems(data);
    } catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); }, []);

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <div>
          <CardTitle>Dealer inventories</CardTitle>
          <CardDescription>All dealers with stock available.</CardDescription>
        </div>
        <CreateInventoryDialog onDone={load} />
      </CardHeader>
      <CardContent>
        {loading ? <p className="text-sm text-muted-foreground">Loading…</p> : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader><TableRow>
                <TableHead>Dealer</TableHead><TableHead>Address</TableHead><TableHead>Lat</TableHead>
                <TableHead>Lng</TableHead><TableHead>Stock</TableHead><TableHead>Updated</TableHead>
              </TableRow></TableHeader>
              <TableBody>
                {items.length === 0 && (
                  <TableRow><TableCell colSpan={6} className="text-center text-sm text-muted-foreground py-6">No inventories.</TableCell></TableRow>
                )}
                {items.map((i) => (
                  <TableRow key={i.id}>
                    <TableCell className="font-medium">{i.dealerName}</TableCell>
                    <TableCell className="text-xs">{i.address}</TableCell>
                    <TableCell className="text-xs">{i.latitude}</TableCell>
                    <TableCell className="text-xs">{i.longitude}</TableCell>
                    <TableCell><Badge variant="secondary">{i.availableStock}</Badge></TableCell>
                    <TableCell className="text-xs">{i.lastUpdated ? new Date(i.lastUpdated).toLocaleString() : "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function CreateInventoryDialog({ onDone }: { onDone: () => void }) {
  const [open, setOpen] = useState(false);
  const [dealers, setDealers] = useState<User[]>([]);
  const [form, setForm] = useState({
    dealerId: "", dealerName: "", address: "", latitude: "6.9271", longitude: "79.8612", availableStock: "0",
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    api<User[]>(`/api/v1/users/role/DEALER`).then(setDealers).catch((e) => toast.error(e.message));
  }, [open]);

  const onDealerChange = (id: string) => {
    const d = dealers.find((x) => x.id === id);
    setForm((f) => ({
      ...f, dealerId: id,
      dealerName: d?.businessName || d?.name || "",
      address: d?.address || f.address,
    }));
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api(`/api/v1/inventory`, {
        method: "POST",
        body: JSON.stringify({
          dealerId: form.dealerId, dealerName: form.dealerName, address: form.address,
          latitude: Number(form.latitude), longitude: Number(form.longitude),
          availableStock: Number(form.availableStock),
        }),
      });
      toast.success("Inventory created");
      setOpen(false); onDone();
    } catch (e) { toast.error(e instanceof Error ? e.message : "Failed"); }
    finally { setLoading(false); }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild><Button><Plus className="mr-2 h-4 w-4" />Create inventory</Button></DialogTrigger>
      <DialogContent className="max-w-xl">
        <DialogHeader><DialogTitle>Create dealer inventory</DialogTitle></DialogHeader>
        <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-2 sm:col-span-2">
            <Label>Dealer</Label>
            <Select value={form.dealerId} onValueChange={onDealerChange}>
              <SelectTrigger><SelectValue placeholder="Select dealer" /></SelectTrigger>
              <SelectContent>
                {dealers.map((d) => (
                  <SelectItem key={d.id} value={d.id}>{d.businessName || d.name} — {d.email}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <FormField label="Dealer name" value={form.dealerName} onChange={(e) => setForm({ ...form, dealerName: e.target.value })} required className="sm:col-span-2" />
          <FormField label="Address" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} required className="sm:col-span-2" />
          <FormField label="Latitude" value={form.latitude} onChange={(e) => setForm({ ...form, latitude: e.target.value })} required />
          <FormField label="Longitude" value={form.longitude} onChange={(e) => setForm({ ...form, longitude: e.target.value })} required />
          <FormField label="Initial stock" type="number" value={form.availableStock} onChange={(e) => setForm({ ...form, availableStock: e.target.value })} required className="sm:col-span-2" />
          <DialogFooter className="sm:col-span-2">
            <Button type="submit" disabled={loading || !form.dealerId}>{loading ? "Creating…" : "Create"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
