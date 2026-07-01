import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { Flame } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export const Route = createFileRoute("/register")({ component: RegisterPage });

function RegisterPage() {
  const { registerCitizen } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ nic: "", email: "", name: "", password: "", confirm: "" });
  const [loading, setLoading] = useState(false);

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [k]: e.target.value });

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (form.password !== form.confirm) { toast.error("Passwords do not match"); return; }
    if (form.password.length < 6) { toast.error("Password must be at least 6 characters"); return; }
    setLoading(true);
    try {
      await registerCitizen({ nic: form.nic.trim(), email: form.email.trim(), name: form.name.trim(), password: form.password });
      toast.success("Account created");
      navigate({ to: "/" });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-6 flex flex-col items-center text-center">
          <span className="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-lg shadow-primary/30">
            <Flame className="h-6 w-6" />
          </span>
          <h1 className="text-2xl font-semibold tracking-tight">Create your account</h1>
          <p className="text-sm text-muted-foreground">Citizens can self-register below.</p>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>Register</CardTitle>
            <CardDescription>Dealers are registered by the platform admin.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Full name</Label>
                <Input id="name" value={form.name} onChange={set("name")} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="nic">NIC</Label>
                <Input id="nic" placeholder="123456789V or 200012345678" value={form.nic} onChange={set("nic")} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" type="email" value={form.email} onChange={set("email")} required />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="password">Password</Label>
                  <Input id="password" type="password" value={form.password} onChange={set("password")} required />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="confirm">Confirm</Label>
                  <Input id="confirm" type="password" value={form.confirm} onChange={set("confirm")} required />
                </div>
              </div>
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? "Creating..." : "Create account"}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                Already have an account? <Link to="/login" className="font-medium text-primary hover:underline">Sign in</Link>
              </p>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
