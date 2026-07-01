import { useEffect, useRef } from "react";
import maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import type { Inventory } from "@/lib/api";

const OSM_STYLE: maplibregl.StyleSpecification = {
  version: 8,
  sources: {
    osm: {
      type: "raster",
      tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      tileSize: 256,
      attribution: "&copy; OpenStreetMap contributors",
    },
  },
  layers: [{ id: "osm", type: "raster", source: "osm" }],
};

interface DealerMapProps {
  center: { lat: number; lng: number };
  dealers: Inventory[];
  selectedId?: string;
  onSelectDealer?: (id: string) => void;
}

export function DealerMap({ center, dealers, selectedId, onSelectDealer }: DealerMapProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<maplibregl.Map | null>(null);
  const markersRef = useRef<Map<string, maplibregl.Marker>>(new Map());

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;
    mapRef.current = new maplibregl.Map({
      container: containerRef.current,
      style: OSM_STYLE,
      center: [center.lng, center.lat],
      zoom: 12,
    });
    mapRef.current.addControl(new maplibregl.NavigationControl(), "top-right");
    return () => { mapRef.current?.remove(); mapRef.current = null; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    mapRef.current?.flyTo({ center: [center.lng, center.lat], zoom: 12 });
  }, [center.lat, center.lng]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    markersRef.current.forEach((m) => m.remove());
    markersRef.current.clear();

    const userEl = document.createElement("div");
    userEl.className = "h-4 w-4 rounded-full bg-blue-500 ring-4 ring-blue-200 border-2 border-white";
    new maplibregl.Marker({ element: userEl }).setLngLat([center.lng, center.lat]).addTo(map);

    dealers.forEach((d) => {
      const isSelected = d.id === selectedId;
      const el = document.createElement("button");
      el.className = `flex h-7 w-7 items-center justify-center rounded-full border-2 border-white text-xs font-bold text-white shadow-md transition-transform ${
        isSelected ? "scale-125 bg-orange-600" : "bg-emerald-600"
      }`;
      el.textContent = "G";
      el.onclick = () => onSelectDealer?.(d.id);

      const popup = new maplibregl.Popup({ offset: 16, closeButton: false }).setHTML(
        `<div style="font-size:13px"><strong>${escapeHtml(d.dealerName)}</strong><br/>${escapeHtml(d.address)}<br/>Stock: ${d.availableStock}${
          typeof d.distanceKm === "number" ? `<br/>${d.distanceKm.toFixed(1)} km away` : ""
        }</div>`
      );

      const marker = new maplibregl.Marker({ element: el })
        .setLngLat([d.longitude, d.latitude])
        .setPopup(popup)
        .addTo(map);
      markersRef.current.set(d.id, marker);
    });
  }, [dealers, selectedId, onSelectDealer, center.lat, center.lng]);

  useEffect(() => {
    if (!selectedId) return;
    const marker = markersRef.current.get(selectedId);
    marker?.togglePopup();
  }, [selectedId]);

  return <div ref={containerRef} className="h-full w-full rounded-lg" />;
}

function escapeHtml(s: string) {
  return s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c] as string));
}
