import { Link, useNavigate } from "@tanstack/react-router";
import { Flame, LogOut } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import type { Role } from "@/lib/api";

const roleLabels: Record<Role, { text: string; className: string }> = {
  CITIZEN: { text: "Citizen", className: "bg-sky-100 text-sky-700 hover:bg-sky-100" },
  DEALER: { text: "Dealer", className: "bg-violet-100 text-violet-700 hover:bg-violet-100" },
  ADMIN: { text: "Admin", className: "bg-rose-100 text-rose-700 hover:bg-rose-100" },
};

export function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const handleLogout = () => {
    logout();
    navigate({ to: "/login" });
  };
  if (!user) return null;
  const role = roleLabels[user.role];

  return (
    <header className="sticky top-0 z-30 border-b bg-background/80 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4">
        <Link to="/" className="flex items-center gap-2 font-semibold">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Flame className="h-5 w-5" />
          </span>
          <span className="hidden sm:inline">Smart Gas Platform</span>
        </Link>
        <div className="flex items-center gap-3">
          <div className="hidden sm:flex flex-col items-end leading-tight">
            <span className="text-sm font-medium">{user.name}</span>
            <span className="text-xs text-muted-foreground">{user.email}</span>
          </div>
          <Badge variant="secondary" className={role.className}>{role.text}</Badge>
          <Button variant="ghost" size="sm" onClick={handleLogout}>
            <LogOut className="h-4 w-4" /> <span className="hidden sm:inline ml-1">Logout</span>
          </Button>
        </div>
      </div>
    </header>
  );
}
