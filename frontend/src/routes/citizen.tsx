import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { MapPin, Search, Locate, Package, User as UserIcon, Navigation } from "lucide-react";
import { toast } from "sonner";
import { z } from "zod";
import { RoleGuard } from "@/components/RoleGuard";
import { DealerMap } from "@/components/DealerMap";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { api, type Inventory } from "@/lib/api";
import { useAuth } from "@/lib/auth";

export const Route = createFileRoute("/citizen")({
  validateSearch: z.object({
    tab: z.enum(["discover", "profile"]).optional(),
  }),
  component: () => (<RoleGuard allow={["CITIZEN", "ADMIN"]}><CitizenDashboard /></RoleGuard>),
});

function CitizenDashboard() {
  const { user } = useAuth();
  const navigate = Route.useNavigate();
  const search = Route.useSearch();
  const tab = search.tab ?? "discover";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Hello, {user?.name?.split(" ")[0]}</h1>
        <p className="text-muted-foreground">Find LPG availability near you.</p>
      </div>
      <Tabs
        value={tab}
        onValueChange={(v) => navigate({ search: (prev) => ({ ...prev, tab: v as typeof tab }) })}
      >
        <TabsList>
          <TabsTrigger value="discover"><Search className="mr-2 h-4 w-4" />Discover</TabsTrigger>
          <TabsTrigger value="profile"><UserIcon className="mr-2 h-4 w-4" />Profile</TabsTrigger>
        </TabsList>
        <TabsContent value="discover" className="mt-4"><Discover /></TabsContent>
        <TabsContent value="profile" className="mt-4"><Profile /></TabsContent>
      </Tabs>
    </div>
  );
}

function Discover() {
  const [coords, setCoords] = useState({ lat: 6.9271, lng: 79.8612 });
  const [radius, setRadius] = useState("10");
  const [items, setItems] = useState<Inventory[]>([]);
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [selectedId, setSelectedId] = useState<string | undefined>();

  const search = async (c = coords, r = radius) => {
    setLoading(true);
    try {
      const data = await api<Inventory[]>(`/api/v1/inventory/nearby?lat=${c.lat}&lng=${c.lng}&radius=${r}`);
      setItems(data);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Search failed");
    } finally { setLoading(false); }
  };

  const useGeo = (silent = false) => {
    if (!navigator.geolocation) {
      if (!silent) toast.error("Geolocation not supported");
      search();
      return;
    }
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (p) => {
        const c = { lat: p.coords.latitude, lng: p.coords.longitude };
        setCoords(c);
        setLocating(false);
        if (!silent) toast.success("Location updated");
        search(c);
      },
      () => {
        setLocating(false);
        if (!silent) toast.error("Could not get your location");
        search();
      },
    );
  };

  useEffect(() => { useGeo(true); /* eslint-disable-next-line */ }, []);

  const sorted = useMemo(
    () => [...items].sort((a, b) => (a.distanceKm ?? Infinity) - (b.distanceKm ?? Infinity)),
    [items],
  );

  return (
    <div className="space-y-4">
      <Card>
        <CardContent className="flex flex-wrap items-center gap-3 pt-6">
          <Button type="button" variant="outline" onClick={() => useGeo()} disabled={locating}>
            <Locate className="mr-2 h-4 w-4" />
            {locating ? "Locating..." : "Use my location"}
          </Button>
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">Radius</span>
            <Select value={radius} onValueChange={(v) => { setRadius(v); search(coords, v); }}>
              <SelectTrigger className="w-24"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="5">5 km</SelectItem>
                <SelectItem value="10">10 km</SelectItem>
                <SelectItem value="15">15 km</SelectItem>
                <SelectItem value="50">50 km</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button type="button" onClick={() => search()} disabled={loading} className="ml-auto">
            <Search className="mr-2 h-4 w-4" />
            {loading ? "Searching..." : "Refresh"}
          </Button>
        </CardContent>
      </Card>

      <div className="grid gap-4 lg:grid-cols-[1fr_1.2fr]">
        <div className="space-y-3 lg:max-h-[600px] lg:overflow-y-auto lg:pr-1">
          {sorted.length === 0 && !loading && (
            <p className="text-center text-sm text-muted-foreground py-8">No dealers found nearby. Try a larger radius.</p>
          )}
          {sorted.map((d) => (
            <Card
              key={d.id}
              className={`cursor-pointer transition hover:shadow-md ${selectedId === d.id ? "ring-2 ring-primary" : ""}`}
              onClick={() => setSelectedId(d.id)}
            >
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between gap-2">
                  <CardTitle className="text-base">{d.dealerName}</CardTitle>
                  {typeof d.distanceKm === "number" && (
                    <span className="flex items-center gap-1 whitespace-nowrap text-xs font-medium text-primary">
                      <Navigation className="h-3 w-3" /> {d.distanceKm.toFixed(1)} km
                    </span>
                  )}
                </div>
                <CardDescription className="flex items-start gap-1">
                  <MapPin className="h-3.5 w-3.5 mt-0.5 shrink-0" /> {d.address}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between rounded-lg bg-secondary p-3">
                  <div className="flex items-center gap-2">
                    <Package className="h-4 w-4 text-primary" />
                    <span className="text-sm font-medium">Available stock</span>
                  </div>
                  <span className="text-lg font-semibold">{d.availableStock}</span>
                </div>
                <Button className="w-full" variant="secondary" disabled>Join queue (coming soon)</Button>
              </CardContent>
            </Card>
          ))}
        </div>

        <Card className="h-[400px] overflow-hidden lg:h-[600px]">
          <DealerMap center={coords} dealers={sorted} selectedId={selectedId} onSelectDealer={setSelectedId} />
        </Card>
      </div>
    </div>
  );
}

function Profile() {
  const { user } = useAuth();
  if (!user) return null;
  return (
    <Card>
      <CardHeader><CardTitle>My profile</CardTitle></CardHeader>
      <CardContent className="grid gap-3 sm:grid-cols-2">
        <Field label="Name" value={user.name} />
        <Field label="NIC" value={user.nic} />
        <Field label="Email" value={user.email} />
        <Field label="Role" value={user.role} />
        <Field label="User ID" value={user.id} />
        {user.createdAt && <Field label="Joined" value={new Date(user.createdAt).toLocaleString()} />}
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
