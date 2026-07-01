import type { ReactNode } from "react";
import { Navigate } from "@tanstack/react-router";
import { useAuth } from "@/lib/auth";
import type { Role } from "@/lib/api";
import { AppShell } from "@/components/AppShell";

export function RoleGuard({ allow, children }: { allow: Role[]; children: ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    );
  }
  if (!user) return <Navigate to="/login" />;
  if (!allow.includes(user.role)) return <Navigate to="/" />;
  return <AppShell>{children}</AppShell>;
}
