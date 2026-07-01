import { useEffect, useMemo, useState } from "react";
import { loadStripe } from "@stripe/stripe-js";
import {
  Elements, PaymentElement, useElements, useStripe,
} from "@stripe/react-stripe-js";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog";
import { api, type Payment } from "@/lib/api";

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY ?? "");

interface PaymentDialogProps {
  allocationId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onPaid: () => void;
}

export function PaymentDialog({ allocationId, open, onOpenChange, onPaid }: PaymentDialogProps) {
  const [payment, setPayment] = useState<Payment | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) { setPayment(null); return; }
    setLoading(true);
    api<Payment>(`/api/v1/payments/allocations/${allocationId}`, { method: "POST" })
      .then(setPayment)
      .catch((e) => toast.error(e instanceof Error ? e.message : "Could not start payment"))
      .finally(() => setLoading(false));
  }, [open, allocationId]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Pay for allocation</DialogTitle>
          <DialogDescription>
            {payment ? `Amount due: LKR ${payment.amount.toFixed(2)}` : "Preparing payment…"}
          </DialogDescription>
        </DialogHeader>
        {loading && <p className="text-sm text-muted-foreground">Loading…</p>}
        {payment && payment.status === "PAID" && (
          <p className="text-sm text-green-600">Payment already completed.</p>
        )}
        {payment && payment.status !== "PAID" && (
          <Elements stripe={stripePromise} options={{ clientSecret: payment.clientSecret }}>
            <CheckoutForm onPaid={onPaid} onOpenChange={onOpenChange} />
          </Elements>
        )}
      </DialogContent>
    </Dialog>
  );
}

function CheckoutForm({ onPaid, onOpenChange }: { onPaid: () => void; onOpenChange: (open: boolean) => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!stripe || !elements) return;
    setSubmitting(true);
    try {
      const { error, paymentIntent } = await stripe.confirmPayment({ elements, redirect: "if_required" });
      if (error) {
        toast.error(error.message ?? "Payment failed");
        return;
      }
      if (paymentIntent?.status === "succeeded") {
        toast.success("Payment successful");
        onOpenChange(false);
        onPaid();
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-4">
      <PaymentElement />
      <DialogFooter>
        <Button onClick={handleSubmit} disabled={!stripe || submitting}>
          {submitting ? "Processing…" : "Pay now"}
        </Button>
      </DialogFooter>
    </div>
  );
}
