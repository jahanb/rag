"use client";
import { ExternalLink, Database, Globe, ChevronDown, ChevronUp } from "lucide-react";
import { useState } from "react";
import type { SourceDocument, WebSearchResult } from "../lib/api";

export function SourceCards({
  docs,
  webResults,
}: {
  docs: SourceDocument[];
  webResults: WebSearchResult[];
}) {
  const [expanded, setExpanded] = useState(false);

  if (!docs.length && !webResults.length) return null;

  const totalSources = docs.length + webResults.length;

  return (
    <div className="mt-3 pt-3 border-t border-ink-700">
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center gap-2 text-xs text-gray-500 hover:text-gray-300 transition-colors mb-2"
      >
        {expanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
        <span className="font-mono">{totalSources} source{totalSources !== 1 ? "s" : ""} referenced</span>
      </button>

      {expanded && (
        <div className="space-y-3 animate-[fadeUp_0.2s_ease-out]">
          {docs.length > 0 && (
            <div>
              <div className="flex items-center gap-1.5 mb-1.5">
                <Database size={10} className="text-neon-400" />
                <span className="text-xs font-mono text-neon-400 uppercase tracking-widest">
                  Knowledge Base
                </span>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {docs.map((doc, i) => (
                  <a
                    key={i}
                    href={doc.url || "#"}
                    target="_blank"
                    rel="noreferrer"
                    title={doc.content?.substring(0, 120) + "..."}
                    className="group flex items-center gap-1.5 bg-ink-800 border border-ink-600 hover:border-neon-400/40 text-gray-400 hover:text-neon-400 px-2.5 py-1 rounded text-xs font-mono transition-all max-w-[180px]"
                  >
                    <span className="truncate">{doc.title}</span>
                    <span className="text-neon-400/60 shrink-0 text-[10px]">
                      {Math.round(doc.score * 100)}%
                    </span>
                  </a>
                ))}
              </div>
            </div>
          )}

          {webResults.length > 0 && (
            <div>
              <div className="flex items-center gap-1.5 mb-1.5">
                <Globe size={10} className="text-frost-400" />
                <span className="text-xs font-mono text-frost-400 uppercase tracking-widest">
                  Web Results
                </span>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {webResults.map((r, i) => (
                  <a
                    key={i}
                    href={r.url}
                    target="_blank"
                    rel="noreferrer"
                    title={r.snippet}
                    className="flex items-center gap-1.5 bg-ink-800 border border-ink-600 hover:border-frost-400/40 text-gray-400 hover:text-frost-400 px-2.5 py-1 rounded text-xs font-mono transition-all max-w-[200px]"
                  >
                    <ExternalLink size={9} className="shrink-0" />
                    <span className="truncate">{r.title}</span>
                  </a>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
