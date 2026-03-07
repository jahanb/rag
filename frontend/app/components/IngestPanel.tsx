"use client";
import { useState } from "react";
import { Upload, CheckCircle, AlertCircle, X, FileText, Link } from "lucide-react";
import { ragApi, type IngestResponse } from "../lib/api";

export function IngestPanel({ onClose }: { onClose: () => void }) {
  const [title, setTitle] = useState("");
  const [url, setUrl] = useState("");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<IngestResponse | null>(null);
  const [error, setError] = useState("");

  const handleIngest = async () => {
    if (!content.trim() || !title.trim()) return;
    setLoading(true);
    setError("");
    setResult(null);
    try {
      const res = await ragApi.ingest({ content, title, url, source: "upload" });
      setResult(res);
      setContent("");
      setTitle("");
      setUrl("");
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Ingestion failed";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div
        className="bg-ink-900 border border-ink-600 rounded-xl w-full max-w-lg shadow-2xl"
        style={{ boxShadow: "0 0 60px rgba(79,255,176,0.05)" }}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-ink-700">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded bg-neon-400/10 border border-neon-400/30 flex items-center justify-center">
              <Upload size={12} className="text-neon-400" />
            </div>
            <span className="text-sm font-mono text-gray-200">
              ingest_document<span className="text-neon-400">()</span>
            </span>
          </div>
          <button
            onClick={onClose}
            className="text-gray-600 hover:text-gray-300 transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 space-y-4">
          {/* Title */}
          <div>
            <label className="flex items-center gap-1.5 text-xs font-mono text-gray-500 mb-1.5 uppercase tracking-widest">
              <FileText size={10} /> title *
            </label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="My Document Title"
              className="w-full bg-ink-800 border border-ink-600 focus:border-neon-400/50 rounded-lg px-3 py-2.5 text-sm text-gray-200 placeholder-gray-600 font-mono focus:outline-none transition-colors"
            />
          </div>

          {/* URL */}
          <div>
            <label className="flex items-center gap-1.5 text-xs font-mono text-gray-500 mb-1.5 uppercase tracking-widest">
              <Link size={10} /> source url
              <span className="text-gray-700 normal-case">(optional)</span>
            </label>
            <input
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://example.com/article"
              className="w-full bg-ink-800 border border-ink-600 focus:border-neon-400/50 rounded-lg px-3 py-2.5 text-sm text-gray-200 placeholder-gray-600 font-mono focus:outline-none transition-colors"
            />
          </div>

          {/* Content */}
          <div>
            <label className="flex items-center gap-1.5 text-xs font-mono text-gray-500 mb-1.5 uppercase tracking-widest">
              content *
              {content.length > 0 && (
                <span className="text-neon-400/60 normal-case font-mono text-[10px]">
                  {content.length.toLocaleString()} chars
                </span>
              )}
            </label>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Paste document text here..."
              rows={7}
              className="w-full bg-ink-800 border border-ink-600 focus:border-neon-400/50 rounded-lg px-3 py-2.5 text-sm text-gray-200 placeholder-gray-600 font-mono focus:outline-none transition-colors resize-none"
            />
          </div>

          {/* Success */}
          {result && (
            <div className="flex items-start gap-2 bg-neon-400/5 border border-neon-400/20 rounded-lg p-3">
              <CheckCircle size={14} className="text-neon-400 mt-0.5 shrink-0" />
              <div>
                <p className="text-xs text-neon-400 font-mono">{result.message}</p>
                <p className="text-xs text-gray-500 font-mono mt-0.5">
                  {result.chunksCreated} chunks · ID: {result.documentId.substring(0, 8)}…
                </p>
              </div>
            </div>
          )}

          {/* Error */}
          {error && (
            <div className="flex items-start gap-2 bg-red-500/5 border border-red-500/20 rounded-lg p-3">
              <AlertCircle size={14} className="text-red-400 mt-0.5 shrink-0" />
              <p className="text-xs text-red-400 font-mono">{error}</p>
            </div>
          )}

          {/* Submit */}
          <button
            onClick={handleIngest}
            disabled={loading || !content.trim() || !title.trim()}
            className="w-full flex items-center justify-center gap-2 bg-neon-400/10 hover:bg-neon-400/20 border border-neon-400/30 hover:border-neon-400/60 disabled:opacity-40 disabled:cursor-not-allowed text-neon-400 rounded-lg py-2.5 text-sm font-mono transition-all"
          >
            {loading ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-neon-400/30 border-t-neon-400 rounded-full animate-spin" />
                processing...
              </>
            ) : (
              <>
                <Upload size={14} />
                ingest_document()
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
