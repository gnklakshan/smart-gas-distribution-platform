import { createFileRoute, Link, Navigate } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import Autoplay from "embla-carousel-autoplay";
import {
  Flame, MapPin, Gauge, ShieldCheck, ArrowRight, Truck, Clock,
  LogIn, UserPlus, Phone, Quote, Star, Search, ChevronRight,
} from "lucide-react";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious, type CarouselApi,
} from "@/components/ui/carousel";
import { Card, CardContent } from "@/components/ui/card";
import heroImg from "@/assets/sri-lanka-hero.jpg";
import familyImg from "@/assets/carousel-family.jpg";
import dealerImg from "@/assets/carousel-dealer.jpg";
import appImg from "@/assets/carousel-app.jpg";

export const Route = createFileRoute("/")({
  component: Home,
  head: () => ({
    meta: [
      { title: "Smart Gas LK — Find LPG cylinders near you in Sri Lanka" },
      { name: "description", content: "Smart Gas LK helps Sri Lankans find LPG cylinder availability nearby, reserve a slot, and skip the queue. Live stock from Colombo to Jaffna." },
      { property: "og:title", content: "Smart Gas LK" },
      { property: "og:description", content: "LPG distribution & virtual queue platform for Sri Lanka." },
    ],
  }),
});

function Home() {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    );
  }
  if (user) {
    if (user.role === "ADMIN") return <Navigate to="/admin" />;
    if (user.role === "DEALER") return <Navigate to="/dealer" />;
    return <Navigate to="/citizen" />;
  }
  return <Landing />;
}

const heroSlides = [
  {
    img: heroImg,
    eyebrow: "ආයුබෝවන් · வணக்கம்",
    title: "No more queues for your gas cylinder",
    desc: "From Pettah to Point Pedro — find live LPG availability near you and skip the wait.",
    accent: "from-primary via-orange-500 to-amber-500",
  },
  {
    img: appImg,
    eyebrow: "Smart search",
    title: "Dealers near you, in real time",
    desc: "Use GPS or pick a city. See live stock counts and distance — all on your phone.",
    accent: "from-emerald-600 via-emerald-500 to-amber-500",
  },
  {
    img: familyImg,
    eyebrow: "For every household",
    title: "Cook on, without worry",
    desc: "Reserve your cylinder ahead of time. We'll let you know when it's ready to collect.",
    accent: "from-rose-600 via-orange-500 to-amber-400",
  },
  {
    img: dealerImg,
    eyebrow: "For dealers",
    title: "Manage stock & allocations",
    desc: "Update inventory, request new allocations, and serve your community better.",
    accent: "from-amber-600 via-orange-500 to-rose-500",
  },
];

const cities = [
  { name: "Colombo", count: 142 },
  { name: "Kandy", count: 86 },
  { name: "Galle", count: 54 },
  { name: "Jaffna", count: 38 },
  { name: "Negombo", count: 47 },
  { name: "Anuradhapura", count: 29 },
  { name: "Matara", count: 33 },
  { name: "Kurunegala", count: 41 },
];

const testimonials = [
  { name: "Nimali Perera", city: "Nugegoda", quote: "I used to spend half my Saturday in a queue. Now I just open the app, reserve, and walk in. Game changer for working mums." },
  { name: "Kasun Silva", city: "Galle", quote: "As a dealer, updating stock takes 5 seconds. Customers don't crowd outside anymore — they come when their slot is ready." },
  { name: "Priya Rajan", city: "Jaffna", quote: "Available in Tamil and easy to use. My parents finally have a way to find gas without calling 10 shops." },
  { name: "Ravindu Fernando", city: "Kandy", quote: "The map view shows me exactly which dealer has stock. No more wasted trips on the bus." },
];

function Landing() {
  return (
    <div className="min-h-screen bg-background">
      <SiteHeader />
      <HeroCarousel />
      <FlagStripe />
      <StatsStrip />
      <FeaturesSection />
      <HowItWorksSection />
      <CoverageSection />
      <TestimonialsSection />
      <RolesSection />
      <CtaSection />
      <SiteFooter />
    </div>
  );
}

function SiteHeader() {
  return (
    <header className="sticky top-0 z-30 border-b border-border/50 bg-background/80 backdrop-blur-xl">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4">
        <Link to="/" className="flex items-center gap-2 font-semibold">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground shadow-md shadow-primary/30">
            <Flame className="h-5 w-5" />
          </span>
          <span>Smart Gas <span className="text-primary">LK</span></span>
        </Link>
        <nav className="hidden items-center gap-6 text-sm text-muted-foreground md:flex">
          <a href="#features" className="hover:text-foreground transition">Features</a>
          <a href="#how" className="hover:text-foreground transition">How it works</a>
          <a href="#coverage" className="hover:text-foreground transition">Coverage</a>
          <a href="#voices" className="hover:text-foreground transition">Voices</a>
        </nav>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" asChild>
            <Link to="/login"><LogIn className="mr-1 h-4 w-4" /> Sign in</Link>
          </Button>
          <Button size="sm" asChild>
            <Link to="/register"><UserPlus className="mr-1 h-4 w-4" /> Register</Link>
          </Button>
        </div>
      </div>
    </header>
  );
}

function HeroCarousel() {
  const [api, setApi] = useState<CarouselApi>();
  const [selected, setSelected] = useState(0);

  useEffect(() => {
    if (!api) return;
    setSelected(api.selectedScrollSnap());
    api.on("select", () => setSelected(api.selectedScrollSnap()));
  }, [api]);

  return (
    <section className="relative">
      <Carousel
        setApi={setApi}
        opts={{ loop: true }}
        plugins={[Autoplay({ delay: 5500, stopOnInteraction: true })]}
        className="mx-auto max-w-7xl px-4 pt-6"
      >
        <CarouselContent>
          {heroSlides.map((s, i) => (
            <CarouselItem key={i}>
              <div className="relative overflow-hidden rounded-3xl border bg-card shadow-2xl shadow-primary/10">
                <img
                  src={s.img}
                  alt={s.title}
                  width={1600}
                  height={1024}
                  loading={i === 0 ? "eager" : "lazy"}
                  className="h-[420px] w-full object-cover sm:h-[520px] lg:h-[560px]"
                />
                <div className="absolute inset-0 bg-gradient-to-r from-black/75 via-black/40 to-black/10" />
                <div className="absolute inset-0 flex items-end sm:items-center">
                  <div className="max-w-2xl p-6 text-white sm:p-12">
                    <Badge variant="secondary" className="mb-4 bg-white/15 text-white hover:bg-white/15 backdrop-blur">
                      🇱🇰 {s.eyebrow}
                    </Badge>
                    <h1 className="text-balance text-3xl font-semibold tracking-tight sm:text-5xl lg:text-6xl">
                      <span className={`bg-gradient-to-r ${s.accent} bg-clip-text text-transparent`}>{s.title}</span>
                    </h1>
                    <p className="mt-4 max-w-lg text-balance text-base text-white/90 sm:text-lg">{s.desc}</p>
                    <div className="mt-6 flex flex-wrap gap-3">
                      <Button size="lg" asChild>
                        <Link to="/register">
                          <UserPlus className="mr-1 h-4 w-4" /> Register free
                          <ArrowRight className="ml-1 h-4 w-4" />
                        </Link>
                      </Button>
                      <Button size="lg" variant="outline" className="border-white/40 bg-white/10 text-white hover:bg-white/20 hover:text-white" asChild>
                        <Link to="/login"><LogIn className="mr-1 h-4 w-4" /> Sign in</Link>
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </CarouselItem>
          ))}
        </CarouselContent>
        <CarouselPrevious className="left-6 hidden sm:flex" />
        <CarouselNext className="right-6 hidden sm:flex" />
      </Carousel>

      {/* Dots */}
      <div className="mt-5 flex items-center justify-center gap-2">
        {heroSlides.map((_, i) => (
          <button
            key={i}
            aria-label={`Go to slide ${i + 1}`}
            onClick={() => api?.scrollTo(i)}
            className={`h-1.5 rounded-full transition-all ${selected === i ? "w-8 bg-primary" : "w-2.5 bg-border hover:bg-muted-foreground/40"}`}
          />
        ))}
      </div>
    </section>
  );
}

function FlagStripe() {
  return (
    <div className="mx-auto mt-6 flex max-w-7xl items-center gap-1 px-4 opacity-80">
      <span className="h-1 flex-1 rounded-full bg-[#8D153A]" />
      <span className="h-1 w-10 rounded-full bg-[#FFBE29]" />
      <span className="h-1 w-10 rounded-full bg-[#00534E]" />
      <span className="h-1 w-10 rounded-full bg-[#EB7400]" />
      <span className="h-1 flex-1 rounded-full bg-[#8D153A]" />
    </div>
  );
}

function StatsStrip() {
  return (
    <section className="border-y mt-10 bg-secondary/40">
      <div className="mx-auto grid max-w-7xl grid-cols-2 gap-6 px-4 py-10 sm:grid-cols-4">
        <Stat value="9" label="Provinces covered" />
        <Stat value="470+" label="Registered dealers" />
        <Stat value="< 1s" label="Live stock lookup" />
        <Stat value="24/7" label="Always online" />
      </div>
    </section>
  );
}

function FeaturesSection() {
  return (
    <section id="features" className="mx-auto max-w-7xl px-4 py-20">
      <div className="mx-auto max-w-2xl text-center">
        <Badge variant="secondary" className="mb-3 bg-primary/10 text-primary hover:bg-primary/10">What we offer</Badge>
        <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
          Built for everyday life in <span className="text-primary">ශ්‍රී ලංකාව</span>
        </h2>
        <p className="mt-3 text-muted-foreground">Real-time stock, virtual queues, and a fairer distribution system from Colombo to Killinochchi.</p>
      </div>
      <div className="mt-12 grid gap-5 md:grid-cols-3">
        <Feature icon={<MapPin className="h-5 w-5" />} title="Find dealers nearby"
          text="See live cylinder availability at dealers around your home, office, or boarding place." />
        <Feature icon={<Gauge className="h-5 w-5" />} title="Live stock counts"
          text="Dealers update their inventory in real time — no more wasted trips on a hot afternoon." />
        <Feature icon={<Clock className="h-5 w-5" />} title="Virtual queues"
          text="Reserve your slot from your phone and arrive only when it's your turn." />
        <Feature icon={<Truck className="h-5 w-5" />} title="Allocation workflow"
          text="Dealers request stock; the platform admin approves and tracks every delivery." />
        <Feature icon={<ShieldCheck className="h-5 w-5" />} title="Secure & fair"
          text="NIC-based registration with role-based access — citizens, dealers and admins, all separate." />
        <Feature icon={<Phone className="h-5 w-5" />} title="Mobile first"
          text="Designed for the phone in your pocket — works smoothly even on slower 3G networks." />
      </div>
    </section>
  );
}

function HowItWorksSection() {
  return (
    <section id="how" className="border-t bg-gradient-to-b from-background to-secondary/40">
      <div className="mx-auto max-w-7xl px-4 py-20">
        <div className="mx-auto max-w-2xl text-center">
          <Badge variant="secondary" className="mb-3 bg-amber-100 text-amber-700 hover:bg-amber-100">How it works</Badge>
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">Three steps to a full cylinder</h2>
          <p className="mt-3 text-muted-foreground">No queues. No phone calls. No guesswork.</p>
        </div>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          <Step n="01" icon={<UserPlus className="h-5 w-5" />} title="Register with your NIC"
            text="Sign up as a citizen using your NIC number — old (V) or new format both work." />
          <Step n="02" icon={<Search className="h-5 w-5" />} title="Search your area"
            text="Enter your location or use GPS to see dealers within 5–15 km along with live stock." />
          <Step n="03" icon={<Truck className="h-5 w-5" />} title="Reserve & collect"
            text="Join the virtual queue, get notified when ready, and pick up your cylinder hassle-free." />
        </div>
        <div className="mt-10 flex justify-center gap-3">
          <Button size="lg" asChild>
            <Link to="/register">Start now <ArrowRight className="ml-1 h-4 w-4" /></Link>
          </Button>
          <Button size="lg" variant="outline" asChild>
            <Link to="/login">I have an account</Link>
          </Button>
        </div>
      </div>
    </section>
  );
}

function CoverageSection() {
  return (
    <section id="coverage" className="mx-auto max-w-7xl px-4 py-20">
      <div className="mx-auto max-w-2xl text-center">
        <Badge variant="secondary" className="mb-3 bg-emerald-100 text-emerald-700 hover:bg-emerald-100">Islandwide</Badge>
        <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">Across the island</h2>
        <p className="mt-3 text-muted-foreground">Active dealers in every major city — and growing every week.</p>
      </div>
      <div className="mt-10 grid gap-3 sm:grid-cols-2 md:grid-cols-4">
        {cities.map((c) => (
          <div key={c.name} className="group flex items-center justify-between rounded-2xl border bg-card p-5 transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5">
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <MapPin className="h-5 w-5" />
              </span>
              <div>
                <p className="font-semibold">{c.name}</p>
                <p className="text-xs text-muted-foreground">{c.count} dealers</p>
              </div>
            </div>
            <ChevronRight className="h-4 w-4 text-muted-foreground transition group-hover:translate-x-0.5 group-hover:text-primary" />
          </div>
        ))}
      </div>
    </section>
  );
}

function TestimonialsSection() {
  return (
    <section id="voices" className="border-t bg-gradient-to-b from-secondary/40 to-background">
      <div className="mx-auto max-w-7xl px-4 py-20">
        <div className="mx-auto max-w-2xl text-center">
          <Badge variant="secondary" className="mb-3 bg-rose-100 text-rose-700 hover:bg-rose-100">Voices from the island</Badge>
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">What people are saying</h2>
          <p className="mt-3 text-muted-foreground">Real Sri Lankans, real time saved.</p>
        </div>

        <Carousel
          opts={{ loop: true, align: "start" }}
          plugins={[Autoplay({ delay: 4500, stopOnInteraction: true })]}
          className="mx-auto mt-12 max-w-5xl"
        >
          <CarouselContent>
            {testimonials.map((t, i) => (
              <CarouselItem key={i} className="md:basis-1/2">
                <Card className="h-full">
                  <CardContent className="flex h-full flex-col p-6">
                    <div className="mb-3 flex items-center gap-1 text-amber-500">
                      {Array.from({ length: 5 }).map((_, j) => (
                        <Star key={j} className="h-4 w-4 fill-current" />
                      ))}
                    </div>
                    <Quote className="h-6 w-6 text-primary/40" />
                    <p className="mt-2 flex-1 text-sm text-foreground/90 sm:text-base">"{t.quote}"</p>
                    <div className="mt-4 flex items-center gap-3 border-t pt-4">
                      <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
                        {t.name.split(" ").map((n) => n[0]).join("")}
                      </span>
                      <div>
                        <p className="text-sm font-semibold">{t.name}</p>
                        <p className="text-xs text-muted-foreground">{t.city}, Sri Lanka</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </CarouselItem>
            ))}
          </CarouselContent>
          <CarouselPrevious />
          <CarouselNext />
        </Carousel>
      </div>
    </section>
  );
}

function RolesSection() {
  return (
    <section id="roles" className="mx-auto max-w-7xl px-4 py-20">
      <div className="mx-auto max-w-2xl text-center">
        <Badge variant="secondary" className="mb-3 bg-violet-100 text-violet-700 hover:bg-violet-100">For everyone</Badge>
        <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">A workspace for every role</h2>
        <p className="mt-3 text-muted-foreground">Tailored dashboards for citizens, dealers and platform admins.</p>
      </div>
      <div className="mt-12 grid gap-5 md:grid-cols-3">
        <RoleCard title="Citizen" sinhala="පුරවැසි" tag="Free forever" tagClass="bg-emerald-100 text-emerald-700"
          gradient="from-emerald-500/10 to-emerald-500/0"
          points={["Search dealers near you", "See live cylinder stock", "Join virtual queues"]} />
        <RoleCard title="Dealer" sinhala="වෙළෙන්දා" tag="By invitation" tagClass="bg-amber-100 text-amber-700"
          gradient="from-amber-500/10 to-amber-500/0"
          points={["Update stock instantly", "Request new allocations", "Confirm deliveries"]} />
        <RoleCard title="Admin" sinhala="පරිපාලක" tag="Operations" tagClass="bg-rose-100 text-rose-700"
          gradient="from-rose-500/10 to-rose-500/0"
          points={["Onboard dealers", "Approve allocations", "Audit the network"]} />
      </div>
    </section>
  );
}

function CtaSection() {
  return (
    <section className="mx-auto max-w-7xl px-4 pb-24">
      <div className="relative overflow-hidden rounded-3xl border bg-gradient-to-br from-primary via-orange-600 to-amber-500 p-10 text-primary-foreground shadow-2xl shadow-primary/20 sm:p-14">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-white/15 blur-3xl" />
        <div className="absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-white/15 blur-3xl" />
        <div className="relative grid items-center gap-6 sm:grid-cols-[1fr_auto]">
          <div>
            <h3 className="text-2xl font-semibold sm:text-3xl">Skip the queue. Get on with your day.</h3>
            <p className="mt-2 max-w-xl text-sm text-white/85 sm:text-base">
              Join thousands of households across Sri Lanka already using Smart Gas LK to find their next cylinder.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button size="lg" variant="secondary" asChild>
              <Link to="/register">
                <UserPlus className="mr-1 h-4 w-4" /> Register now
                <ArrowRight className="ml-1 h-4 w-4" />
              </Link>
            </Button>
            <Button size="lg" variant="outline" className="border-white/40 bg-white/10 text-white hover:bg-white/20 hover:text-white" asChild>
              <Link to="/login"><LogIn className="mr-1 h-4 w-4" /> Sign in</Link>
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}

function SiteFooter() {
  return (
    <footer className="border-t">
      <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-3 px-4 py-8 text-sm text-muted-foreground sm:flex-row">
        <div className="flex items-center gap-2">
          <span className="flex h-7 w-7 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Flame className="h-4 w-4" />
          </span>
          <span>Smart Gas LK · ස්මාර්ට් ගෑස්</span>
        </div>
        <p>© {new Date().getFullYear()} · Built with ❤️ for Sri Lanka 🇱🇰</p>
      </div>
    </footer>
  );
}

/* -------------------- atoms -------------------- */
function Stat({ value, label }: { value: string; label: string }) {
  return (
    <div className="text-center">
      <p className="text-3xl font-semibold tracking-tight text-foreground">{value}</p>
      <p className="mt-1 text-xs uppercase tracking-wide text-muted-foreground">{label}</p>
    </div>
  );
}

function Feature({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return (
    <div className="group rounded-2xl border bg-card p-6 transition hover:-translate-y-0.5 hover:shadow-lg hover:shadow-primary/5">
      <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary transition group-hover:bg-primary group-hover:text-primary-foreground">
        {icon}
      </div>
      <h3 className="font-semibold">{title}</h3>
      <p className="mt-1.5 text-sm text-muted-foreground">{text}</p>
    </div>
  );
}

function Step({ n, icon, title, text }: { n: string; icon: React.ReactNode; title: string; text: string }) {
  return (
    <div className="relative rounded-2xl border bg-card p-6">
      <span className="absolute -top-3 left-6 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground">{n}</span>
      <div className="mt-2 flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">{icon}</div>
      <h3 className="mt-3 font-semibold">{title}</h3>
      <p className="mt-1.5 text-sm text-muted-foreground">{text}</p>
    </div>
  );
}

function RoleCard({ title, sinhala, tag, tagClass, gradient, points }: { title: string; sinhala: string; tag: string; tagClass: string; gradient: string; points: string[] }) {
  return (
    <div className="relative overflow-hidden rounded-2xl border bg-card p-6 transition hover:shadow-lg">
      <div className={`pointer-events-none absolute inset-0 bg-gradient-to-br ${gradient}`} />
      <div className="relative">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold">{title}</h3>
            <p className="text-xs text-muted-foreground">{sinhala}</p>
          </div>
          <Badge variant="secondary" className={tagClass}>{tag}</Badge>
        </div>
        <ul className="mt-5 space-y-2 text-sm">
          {points.map((p) => (
            <li key={p} className="flex items-start gap-2">
              <span className="mt-1 inline-block h-1.5 w-1.5 rounded-full bg-primary" />
              <span>{p}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
