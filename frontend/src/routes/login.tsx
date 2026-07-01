import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { Flame } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export const Route = createFileRoute("/login")({ component: LoginPage });

function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [nic, setNic] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const u = await login(nic.trim(), password);
      toast.success(`Welcome back, ${u.name}`);
      navigate({ to: "/" });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 px-4">
      <div className="w-full max-w-md">
        <div className="mb-6 flex flex-col items-center text-center">
          <span className="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-lg shadow-primary/30">
            <Flame className="h-6 w-6" />
          </span>
          <h1 className="text-2xl font-semibold tracking-tight">Smart Gas Distribution Platform</h1>
          <p className="text-sm text-muted-foreground">Sign in to continue</p>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>Sign in</CardTitle>
            <CardDescription>Use your NIC and password to continue.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="nic">NIC</Label>
                <Input id="nic" placeholder="123456789V or 200012345678" value={nic} onChange={(e) => setNic(e.target.value)} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
              </div>
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? "Signing in..." : "Sign in"}
              </Button>
              <p className="text-center text-sm text-muted-foreground">
                New user? <Link to="/register" className="font-medium text-primary hover:underline">Register here</Link>
              </p>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
