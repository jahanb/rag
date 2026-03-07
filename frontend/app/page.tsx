"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import ReactMarkdown from "react-markdown";
import { v4 as uuidv4 } from "uuid";
import { sendChat, ingestDocument, checkHealth, ChatResponse, SourceDocument } from "./lib/api";
import { Send, Database, Zap, AlertTriangle, ChevronDown, ChevronUp, Plus, X, BookOpen, Loader2, CheckCircle, Wifi, WifiOff } from "lucide-react";

// ── Types ────────────────────────────────────────────────────────
interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  response?: ChatResponse;
  loading?: boolean;
  error?: boolean;
}

// ── Source Badge ────────────────────────────────────────────────
function SourceBadge({ source }: { source: ChatResponse["answerSource"] }) {
  if (source === "database") {
    return (
      <span style={{
        display: "inline-flex", alignItems: "center", gap: "5px",
        background: "var(--db-light)", color: "var(--db-color)",
        border: "1px solid var(--db-border)",
        padding: "3px 10px", borderRadius: "20px", fontSize: "12.5px", fontWeight: 600,
      }}>
        <Database size={12} /> From your database
      </span>
    );
  }
  if (source === "groq_only") {
    return (
      <span style={{
        display: "inline-flex", alignItems: "center", gap: "5px",
        background: "var(--groq-light)", color: "var(--groq-color)",
        border: "1px solid var(--groq-border)",
        padding: "3px 10px", borderRadius: "20px", fontSize: "12.5px", fontWeight: 600,
      }}>
        <Zap size={12} /> Groq AI knowledge
      </span>
    );
  }
  return (
    <span style={{
      display: "inline-flex", alignItems: "center", gap: "5px",
      background: "var(--accent-light)", color: "var(--accent)",
      border: "1px solid var(--accent-border)",
      padding: "3px 10px", borderRadius: "20px", fontSize: "12.5px", fontWeight: 600,
    }}>
      <Database size={12} /><Zap size={12} /> Database + Groq
    </span>
  );
}

// ── Score Bar ────────────────────────────────────────────────────
function ScoreBar({ score }: { score: number }) {
  const pct = Math.round(score * 100);
  const color = score >= 0.7 ? "var(--db-color)" : score >= 0.5 ? "var(--warn-color)" : "var(--text-muted)";
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
      <div style={{ flex: 1, height: "5px", background: "var(--border-light)", borderRadius: "3px", overflow: "hidden" }}>
        <div style={{ width: `${pct}%`, height: "100%", background: color, borderRadius: "3px", transition: "width 0.5s ease" }} />
      </div>
      <span style={{ fontSize: "11px", color: "var(--text-muted)", minWidth: "32px" }}>{pct}%</span>
    </div>
  );
}

// ── Sources Panel ────────────────────────────────────────────────
function SourcesPanel({ sources, score }: { sources: SourceDocument[]; score: number }) {
  const [open, setOpen] = useState(false);
  if (!sources?.length) return null;
  return (
    <div style={{ marginTop: "12px", border: "1px solid var(--border-light)", borderRadius: "10px", overflow: "hidden" }}>
      <button
        onClick={() => setOpen(!open)}
        style={{
          width: "100%", display: "flex", alignItems: "center", justifyContent: "space-between",
          padding: "10px 14px", background: "var(--bg)", border: "none", cursor: "pointer",
          fontSize: "13px", color: "var(--text-secondary)", fontFamily: "inherit",
        }}
      >
        <span style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <BookOpen size={13} />
          {sources.length} source{sources.length > 1 ? "s" : ""} used &nbsp;·&nbsp; top match {Math.round(score * 100)}%
        </span>
        {open ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
      </button>
      {open && (
        <div style={{ borderTop: "1px solid var(--border-light)" }}>
          {sources.map((s, i) => (
            <div key={i} style={{ padding: "12px 14px", borderBottom: i < sources.length - 1 ? "1px solid var(--border-light)" : "none", background: "var(--bg-card)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "6px" }}>
                <span style={{ fontWeight: 600, fontSize: "13.5px", color: "var(--text-primary)" }}>{s.title}</span>
                <span style={{ fontSize: "11px", color: "var(--text-muted)", background: "var(--bg)", padding: "2px 7px", borderRadius: "10px", border: "1px solid var(--border)", whiteSpace: "nowrap", marginLeft: "8px" }}>
                  #{i + 1}
                </span>
              </div>
              <ScoreBar score={s.score} />
              <p style={{ fontSize: "13px", color: "var(--text-secondary)", marginTop: "8px", lineHeight: 1.6 }}>
                {s.content.length > 200 ? s.content.slice(0, 200) + "…" : s.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Ingest Modal ─────────────────────────────────────────────────
function IngestModal({ onClose, onSuccess }: { onClose: () => void; onSuccess: (msg: string) => void }) {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [source, setSource] = useState("manual");
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const submit = async () => {
    if (!title.trim() || !content.trim()) { setError("Title and content are required."); return; }
    setLoading(true); setError("");
    try {
      const res = await ingestDocument({ title, content, source, url });
      onSuccess(res.message);
      onClose();
    } catch {
      setError("Failed to ingest document. Is the backend running?");
    } finally { setLoading(false); }
  };

  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.35)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 100, padding: "20px" }}>
      <div style={{ background: "var(--bg-card)", borderRadius: "16px", width: "100%", maxWidth: "580px", border: "1px solid var(--border)", boxShadow: "0 20px 60px rgba(0,0,0,0.12)" }} className="animate-fade-up">
        <div style={{ padding: "22px 24px 18px", borderBottom: "1px solid var(--border-light)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <h2 style={{ fontFamily: "var(--font-display)", fontSize: "20px", fontWeight: 600, color: "var(--text-primary)" }}>Add to Knowledge Base</h2>
          <button onClick={onClose} style={{ border: "none", background: "none", cursor: "pointer", color: "var(--text-muted)", padding: "4px" }}><X size={20} /></button>
        </div>
        <div style={{ padding: "20px 24px", display: "flex", flexDirection: "column", gap: "14px" }}>
          {[
            { label: "Title", value: title, setter: setTitle, placeholder: "e.g. Introduction to Machine Learning", type: "input" },
            { label: "Source", value: source, setter: setSource, placeholder: "e.g. manual, wikipedia, docs", type: "input" },
            { label: "URL (optional)", value: url, setter: setUrl, placeholder: "https://...", type: "input" },
          ].map(({ label, value, setter, placeholder }) => (
            <div key={label}>
              <label style={{ display: "block", fontSize: "13px", fontWeight: 600, color: "var(--text-secondary)", marginBottom: "5px" }}>{label}</label>
              <input
                value={value} onChange={e => setter(e.target.value)} placeholder={placeholder}
                style={{ width: "100%", padding: "9px 12px", border: "1px solid var(--border)", borderRadius: "8px", fontSize: "15px", fontFamily: "inherit", background: "var(--bg)", color: "var(--text-primary)", outline: "none" }}
              />
            </div>
          ))}
          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: 600, color: "var(--text-secondary)", marginBottom: "5px" }}>Content</label>
            <textarea
              value={content} onChange={e => setContent(e.target.value)} placeholder="Paste or type the document content here..."
              rows={7}
              style={{ width: "100%", padding: "9px 12px", border: "1px solid var(--border)", borderRadius: "8px", fontSize: "14.5px", fontFamily: "inherit", background: "var(--bg)", color: "var(--text-primary)", outline: "none", resize: "vertical", lineHeight: 1.6 }}
            />
          </div>
          {error && (
            <div style={{ display: "flex", alignItems: "center", gap: "8px", color: "var(--warn-color)", fontSize: "13.5px", background: "var(--warn-light)", padding: "10px 12px", borderRadius: "8px", border: "1px solid var(--warn-border)" }}>
              <AlertTriangle size={14} /> {error}
            </div>
          )}
        </div>
        <div style={{ padding: "16px 24px 20px", borderTop: "1px solid var(--border-light)", display: "flex", justifyContent: "flex-end", gap: "10px" }}>
          <button onClick={onClose} style={{ padding: "9px 18px", border: "1px solid var(--border)", borderRadius: "8px", background: "none", cursor: "pointer", fontSize: "14.5px", color: "var(--text-secondary)", fontFamily: "inherit" }}>Cancel</button>
          <button onClick={submit} disabled={loading} style={{ padding: "9px 20px", border: "none", borderRadius: "8px", background: "var(--accent)", color: "#fff", cursor: loading ? "not-allowed" : "pointer", fontSize: "14.5px", fontWeight: 600, fontFamily: "inherit", display: "flex", alignItems: "center", gap: "7px", opacity: loading ? 0.7 : 1 }}>
            {loading ? <><Loader2 size={15} style={{ animation: "spin 1s linear infinite" }} />Ingesting…</> : "Add Document"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Typing Indicator ─────────────────────────────────────────────
function TypingIndicator() {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "5px", padding: "14px 18px", background: "var(--bg-card)", borderRadius: "14px", border: "1px solid var(--border-light)", width: "fit-content" }}>
      {[0,1,2].map(i => (
        <div key={i} className="typing-dot" style={{ width: "7px", height: "7px", borderRadius: "50%", background: "var(--text-muted)" }} />
      ))}
    </div>
  );
}

// ── Main Page ────────────────────────────────────────────────────
export default function Home() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [sessionId, setSessionId] = useState("");
  const [loading, setLoading] = useState(false);
  const [online, setOnline] = useState<boolean | null>(null);
  const [showIngest, setShowIngest] = useState(false);
  const [toast, setToast] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    setSessionId(uuidv4());
    checkHealth()
      .then(() => setOnline(true))
      .catch(() => setOnline(false));
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(""), 4000);
  };

  const send = useCallback(async () => {
    const q = input.trim();
    if (!q || loading) return;
    setInput("");

    const userMsg: Message = { id: uuidv4(), role: "user", content: q };
    const loadingMsg: Message = { id: uuidv4(), role: "assistant", content: "", loading: true };
    setMessages(prev => [...prev, userMsg, loadingMsg]);
    setLoading(true);

    try {
      const data = await sendChat(q, sessionId);
      setMessages(prev => prev.map(m =>
        m.loading ? { ...m, loading: false, content: data.answer, response: data } : m
      ));
    } catch {
      setMessages(prev => prev.map(m =>
        m.loading ? { ...m, loading: false, content: "Could not reach the backend. Is Spring Boot running on port 8080?", error: true } : m
      ));
    } finally {
      setLoading(false);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [input, loading, sessionId]);

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(); }
  };

  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column", background: "var(--bg)" }}>
      {/* ── Header ── */}
      <header style={{ background: "var(--bg-card)", borderBottom: "1px solid var(--border)", padding: "0 24px", height: "60px", display: "flex", alignItems: "center", justifyContent: "space-between", position: "sticky", top: 0, zIndex: 50 }}>
        <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
          <div style={{ width: "34px", height: "34px", borderRadius: "9px", background: "var(--accent)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <BookOpen size={17} color="#fff" />
          </div>
          <div>
            <h1 style={{ fontFamily: "var(--font-display)", fontSize: "18px", fontWeight: 600, color: "var(--text-primary)", lineHeight: 1.2 }}>RAG Assistant</h1>
            <p style={{ fontSize: "11.5px", color: "var(--text-muted)", lineHeight: 1 }}>Groq × MongoDB Knowledge Base</p>
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          {online !== null && (
            <span style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12.5px", color: online ? "var(--db-color)" : "var(--warn-color)" }}>
              {online ? <Wifi size={13} /> : <WifiOff size={13} />}
              {online ? "Backend online" : "Backend offline"}
            </span>
          )}
          <button
            onClick={() => setShowIngest(true)}
            style={{ display: "flex", alignItems: "center", gap: "6px", padding: "7px 14px", background: "var(--accent)", color: "#fff", border: "none", borderRadius: "8px", cursor: "pointer", fontSize: "13.5px", fontWeight: 600, fontFamily: "inherit" }}
          >
            <Plus size={14} /> Add Document
          </button>
        </div>
      </header>

      {/* ── Messages ── */}
      <main style={{ flex: 1, maxWidth: "820px", width: "100%", margin: "0 auto", padding: "28px 20px 20px", display: "flex", flexDirection: "column", gap: "24px" }}>
        {messages.length === 0 && (
          <div style={{ textAlign: "center", padding: "60px 20px" }} className="animate-fade-up">
            <div style={{ width: "64px", height: "64px", borderRadius: "18px", background: "var(--accent-light)", border: "2px solid var(--accent-border)", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 20px" }}>
              <BookOpen size={28} color="var(--accent)" />
            </div>
            <h2 style={{ fontFamily: "var(--font-display)", fontSize: "26px", fontWeight: 600, color: "var(--text-primary)", marginBottom: "10px" }}>Ask your knowledge base</h2>
            <p style={{ fontSize: "16px", color: "var(--text-secondary)", maxWidth: "420px", margin: "0 auto 28px", lineHeight: 1.6 }}>
              Questions answered from your ingested documents. When a topic is not in the database, Groq's AI fills in the gap — and you'll always know which is which.
            </p>
            <div style={{ display: "flex", gap: "10px", justifyContent: "center", flexWrap: "wrap" }}>
              {["What is the capital of France?", "Which country has the largest population?", "Tell me about Japan's economy"].map(q => (
                <button key={q} onClick={() => { setInput(q); setTimeout(() => inputRef.current?.focus(), 50); }}
                  style={{ padding: "8px 16px", background: "var(--bg-card)", border: "1px solid var(--border)", borderRadius: "20px", cursor: "pointer", fontSize: "13.5px", color: "var(--text-secondary)", fontFamily: "inherit", transition: "all 0.15s" }}
                  onMouseEnter={e => { (e.target as HTMLElement).style.borderColor = "var(--accent)"; (e.target as HTMLElement).style.color = "var(--accent)"; }}
                  onMouseLeave={e => { (e.target as HTMLElement).style.borderColor = "var(--border)"; (e.target as HTMLElement).style.color = "var(--text-secondary)"; }}
                >
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg, idx) => (
          <div key={msg.id} className="animate-fade-up" style={{ display: "flex", flexDirection: "column", alignItems: msg.role === "user" ? "flex-end" : "flex-start" }}>
            {msg.role === "user" ? (
              <div style={{ maxWidth: "75%", background: "var(--accent)", color: "#fff", padding: "12px 18px", borderRadius: "18px 18px 6px 18px", fontSize: "15.5px", lineHeight: 1.6 }}>
                {msg.content}
              </div>
            ) : (
              <div style={{ width: "100%", maxWidth: "100%" }}>
                {msg.loading ? (
                  <TypingIndicator />
                ) : (
                  <div style={{ background: "var(--bg-card)", border: `1px solid ${msg.error ? "var(--warn-border)" : "var(--border-light)"}`, borderRadius: "6px 18px 18px 18px", padding: "18px 20px", boxShadow: "0 1px 4px rgba(0,0,0,0.04)" }}>
                    {/* Source indicator */}
                    {msg.response && (
                      <div style={{ marginBottom: "14px", display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                        <SourceBadge source={msg.response.answerSource} />
                        {msg.response.answerSource === "groq_only" && msg.response.dbChunksFound === 0 && (
                          <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                            {msg.response.topMatchScore > 0
                              ? `(best DB match: ${Math.round(msg.response.topMatchScore * 100)}% — below threshold)`
                              : "(no documents in database)"}
                          </span>
                        )}
                        {msg.response.answerSource === "database" && (
                          <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                            {msg.response.dbChunksFound} chunk{msg.response.dbChunksFound !== 1 ? "s" : ""} · score {Math.round(msg.response.topMatchScore * 100)}%
                          </span>
                        )}
                      </div>
                    )}

                    {/* Answer */}
                    <div className="prose-answer" style={{ fontSize: "16px", color: "var(--text-primary)", lineHeight: 1.75 }}>
                      <ReactMarkdown>{msg.content}</ReactMarkdown>
                    </div>

                    {/* Sources */}
                    {msg.response?.sources?.length > 0 && (
                      <SourcesPanel sources={msg.response.sources} score={msg.response.topMatchScore} />
                    )}

                    {/* Debug info */}
                    {msg.response?.debugInfo && (
                      <div style={{ marginTop: "12px", fontSize: "12px", color: "var(--text-muted)", background: "var(--bg)", border: "1px solid var(--border-light)", padding: "8px 12px", borderRadius: "8px" }}>
                        {msg.response.debugInfo}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
        <div ref={bottomRef} />
      </main>

      {/* ── Input ── */}
      <div style={{ position: "sticky", bottom: 0, background: "var(--bg)", borderTop: "1px solid var(--border-light)", padding: "16px 20px 20px" }}>
        <div style={{ maxWidth: "820px", margin: "0 auto", display: "flex", gap: "10px", alignItems: "flex-end" }}>
          <textarea
            ref={inputRef}
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKey}
            placeholder="Ask a question… (Enter to send, Shift+Enter for new line)"
            rows={1}
            style={{
              flex: 1, padding: "12px 16px", border: "1px solid var(--border)", borderRadius: "12px",
              fontSize: "15.5px", fontFamily: "inherit", background: "var(--bg-card)",
              color: "var(--text-primary)", outline: "none", resize: "none",
              lineHeight: 1.6, maxHeight: "140px", overflow: "auto",
              boxShadow: "0 1px 4px rgba(0,0,0,0.04)",
              transition: "border-color 0.15s",
            }}
            onFocus={e => (e.target.style.borderColor = "var(--accent)")}
            onBlur={e => (e.target.style.borderColor = "var(--border)")}
          />
          <button
            onClick={send}
            disabled={loading || !input.trim()}
            style={{
              width: "46px", height: "46px", borderRadius: "12px", border: "none",
              background: loading || !input.trim() ? "var(--border)" : "var(--accent)",
              color: "#fff", cursor: loading || !input.trim() ? "not-allowed" : "pointer",
              display: "flex", alignItems: "center", justifyContent: "center",
              flexShrink: 0, transition: "background 0.15s",
            }}
          >
            {loading ? <Loader2 size={18} style={{ animation: "spin 1s linear infinite" }} /> : <Send size={18} />}
          </button>
        </div>
        <p style={{ textAlign: "center", fontSize: "11.5px", color: "var(--text-muted)", marginTop: "8px" }}>
          Answers sourced from your MongoDB knowledge base · Powered by Groq LLaMA 3.3
        </p>
      </div>

      {/* ── Modals & Toasts ── */}
      {showIngest && (
        <IngestModal
          onClose={() => setShowIngest(false)}
          onSuccess={(msg) => showToast(msg)}
        />
      )}
      {toast && (
        <div className="animate-fade-up" style={{ position: "fixed", bottom: "90px", left: "50%", transform: "translateX(-50%)", background: "var(--text-primary)", color: "#fff", padding: "11px 20px", borderRadius: "10px", fontSize: "14px", display: "flex", alignItems: "center", gap: "8px", boxShadow: "0 8px 24px rgba(0,0,0,0.15)", zIndex: 200, whiteSpace: "nowrap" }}>
          <CheckCircle size={15} style={{ color: "#4ade80" }} /> {toast}
        </div>
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
