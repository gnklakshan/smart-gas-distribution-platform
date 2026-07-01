import type { ReactNode } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  LayoutDashboard,
  Boxes,
  ClipboardList,
  Users,
  Package,
  History,
  User as UserIcon,
  LogOut,
  ShieldCheck,
} from "lucide-react";

import { useAuth } from "@/lib/auth";
import type { Role } from "@/lib/api";
import { cn } from "@/lib/utils";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

type NavItem = { to: string; label: string; icon: ReactNode };

const roleBadge: Record<Role, { label: string; className: string }> = {
  CITIZEN: { label: "Citizen", className: "bg-sky-100 text-sky-800 hover:bg-sky-100" },
  DEALER: { label: "Dealer", className: "bg-violet-100 text-violet-800 hover:bg-violet-100" },
  ADMIN: { label: "Admin", className: "bg-rose-100 text-rose-800 hover:bg-rose-100" },
};

function navFor(role: Role): NavItem[] {
  if (role === "ADMIN") {
    return [
      { to: "/admin?tab=allocations", label: "Allocations", icon: <ClipboardList className="h-4 w-4" /> },
      { to: "/admin?tab=users", label: "Users", icon: <Users className="h-4 w-4" /> },
      { to: "/admin?tab=inventory", label: "Inventory", icon: <Boxes className="h-4 w-4" /> },
    ];
  }
  if (role === "DEALER") {
    return [
      { to: "/dealer?tab=stock", label: "Stock", icon: <Package className="h-4 w-4" /> },
      { to: "/dealer?tab=allocations", label: "Allocations", icon: <History className="h-4 w-4" /> },
      { to: "/dealer?tab=profile", label: "Profile", icon: <UserIcon className="h-4 w-4" /> },
    ];
  }
  return [
    { to: "/citizen?tab=discover", label: "Discover", icon: <LayoutDashboard className="h-4 w-4" /> },
    { to: "/citizen?tab=profile", label: "Profile", icon: <UserIcon className="h-4 w-4" /> },
  ];
}

export function AppShell({
  activePath,
  children,
}: {
  activePath?: string;
  children: ReactNode;
}) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;
  const items = navFor(user.role);

  const onLogout = () => {
    logout();
    navigate({ to: "/login" });
  };

  const badge = roleBadge[user.role];
  const orgLabel =
    user.role === "DEALER"
      ? user.businessName || user.name
      : user.role === "ADMIN"
        ? "Admin Console"
        : "Citizen Portal";

  return (
    <SidebarProvider defaultOpen>
      <Sidebar variant="inset" collapsible="icon">
        <SidebarHeader className="gap-2">
          <div className="flex items-center gap-2 px-2 pt-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div className="min-w-0 group-data-[collapsible=icon]:hidden">
              <p className="truncate text-sm font-semibold leading-tight">Smart Gas Platform</p>
              <p className="truncate text-xs text-muted-foreground">{orgLabel}</p>
            </div>
          </div>
          <Separator className="mx-2 w-auto" />
        </SidebarHeader>

        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel>Navigation</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {items.map((item) => {
                  const isActive = activePath ? activePath === item.to : undefined;
                  return (
                    <SidebarMenuItem key={item.to}>
                      <SidebarMenuButton asChild isActive={isActive} tooltip={item.label}>
                        <Link to={item.to} className="gap-2">
                          {item.icon}
                          <span>{item.label}</span>
                        </Link>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  );
                })}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>

        <SidebarFooter>
          <div className="flex items-center justify-between gap-2 px-2 py-2">
            <div className="min-w-0 group-data-[collapsible=icon]:hidden">
              <p className="truncate text-sm font-medium">{user.name}</p>
              <p className="truncate text-xs text-muted-foreground">{user.email}</p>
            </div>
            <Badge variant="secondary" className={cn("shrink-0", badge.className)}>
              {badge.label}
            </Badge>
          </div>
        </SidebarFooter>
      </Sidebar>

      <SidebarInset>
        <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-3 border-b bg-background/80 px-4 backdrop-blur supports-[backdrop-filter]:bg-background/60">
          <div className="flex items-center gap-2">
            <SidebarTrigger />
            <Link to="/" className="text-sm font-semibold tracking-tight">
              Smart Gas Distribution Platform
            </Link>
          </div>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="gap-2">
                <span className="max-w-[18ch] truncate">{user.name}</span>
                <Badge variant="secondary" className={badge.className}>
                  {badge.label}
                </Badge>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-64">
              <DropdownMenuLabel className="space-y-1">
                <div className="text-sm font-medium leading-none">{user.name}</div>
                <div className="text-xs font-normal text-muted-foreground">{user.email}</div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={onLogout}>
                <LogOut className="mr-2 h-4 w-4" />
                Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </header>

        <main className="mx-auto w-full max-w-[1400px] px-4 py-6">{children}</main>
      </SidebarInset>
    </SidebarProvider>
  );
}
