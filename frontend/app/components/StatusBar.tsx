"use client";
import { useEffect, useState } from "react";
import { ragApi } from "../lib/api";

export function StatusBar({ sessionId }: { sessionId: string }) {
  const [online, setOnline] = useState<boolean | null>(null);

  useEffect(() => {
    ragApi.health().then(setOnline);
    const t = setInterval(() => ragApi.health().then(setOnline), 30000);
    return () => clearInterval(t);
  }, []);

  // Don't render session ID until it's available on the client
  const sessionDisplay = sessionId ? sessionId.substring(0, 8) : "--------";

  return (
    <div className="flex items-center gap-4 text-[10px] font-mono text-gray-600">
      <span className="flex items-center gap-1.5">
        <span
          className={`w-1.5 h-1.5 rounded-full ${
            online === null
              ? "bg-gray-600"
              : online
              ? "bg-neon-400 animate-pulse"
              : "bg-red-500"
          }`}
        />
        {online === null ? "checking..." : online ? "backend:online" : "backend:offline"}
      </span>
      <span className="text-gray-700">|</span>
      <span suppressHydrationWarning>session:{sessionDisplay}</span>
    </div>
  );
}
