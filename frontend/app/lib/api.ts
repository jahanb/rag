import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
});

export interface SourceDocument {
  content: string;
  title: string;
  source: string;
  url: string;
  score: number;
}

export interface ChatResponse {
  answer: string;
  sources: SourceDocument[];
  sessionId: string;
  answerSource: "database" | "groq_only" | "database+groq";
  dbChunksFound: number;
  topMatchScore: number;
  debugInfo: string;
}

export interface IngestResponse {
  documentId: string;
  chunksCreated: number;
  message: string;
}

export const sendChat = (question: string, sessionId: string) =>
  api.post<ChatResponse>("/api/chat", { question, sessionId }).then((r) => r.data);

export const ingestDocument = (payload: {
  title: string;
  content: string;
  source: string;
  url: string;
}) => api.post<IngestResponse>("/api/documents/ingest", payload).then((r) => r.data);

export const checkHealth = () =>
  api.get<string>("/api/health").then((r) => r.data);
