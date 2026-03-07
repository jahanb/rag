RAG System
Setup & Installation Guide
Groq AI  ×  Ollama Embeddings  ×  MongoDB Atlas

Component
Details
LLM (answers)
Groq API — llama-3.3-70b-versatile (free, cloud)
Embeddings
Ollama — nomic-embed-text (local, 274MB, no API key)
Vector Database
MongoDB Atlas M0 (free forever, cloud)
Backend
Spring Boot 3.2 + Java 17
Frontend
Next.js 14 + React 18


1. How It Works
This system is called RAG — Retrieval Augmented Generation. Instead of relying only on AI knowledge, it first searches your own documents for relevant information, then uses AI to compose a clear answer.

RAG Pipeline
Step 1  Your question is converted to a vector (list of numbers) by Ollama locally
Step 2  MongoDB Atlas searches for the most similar vectors in your knowledge base
Step 3  The matching documents are sent to Groq AI as context
Step 4  Groq composes a clear answer based on YOUR documents
Step 5  The UI shows whether the answer came from your database or Groq's training data

Every answer shows one of two badges:
🟢  From your database — answer was found in your ingested documents
🟣  Groq AI knowledge — no relevant document found, Groq used its own training


2. Prerequisites
Install the following before starting:

Component
Details
Java 17
https://adoptium.net — install Temurin JDK 17
Node.js LTS
https://nodejs.org — for the frontend
IntelliJ IDEA
https://jetbrains.com/idea — Community edition is free
Ollama
https://ollama.com/download — Windows installer
Git (optional)
https://git-scm.com

Important: Set Gradle JVM to Java 17 in IntelliJ
File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM → select 17
Gradle 8.5 does not support Java 21+ and will fail with a class file version error.


3. Create Free Accounts
3.1  Groq API (LLM — text generation)
1. Go to https://console.groq.com and sign up for free
2. Click API Keys → Create API Key
3. Copy the key — it starts with gsk_...
4. Free tier: 14,400 requests per day, 30 requests per minute

3.2  MongoDB Atlas (vector database)
5. Go to https://cloud.mongodb.com and sign up
6. Create a free M0 cluster (512MB, free forever)
7. Create database: ragdb  and collection: document_embeddings
8. Network Access → Add IP Address → Allow Access from Anywhere (0.0.0.0/0)
9. Get connection string: Connect → Drivers → copy the mongodb+srv://... URI


4. Install Ollama & Embedding Model
Ollama runs the embedding model locally on your PC. It does NOT replace Groq — it only converts text to vectors for MongoDB search. The embedding model is small (274MB) and runs fast on CPU.

10. Download and install Ollama from https://ollama.com/download
11. Ollama starts automatically as a background service after installation
12. Open any terminal and pull the embedding model:

ollama pull nomic-embed-text

13. Verify Ollama is running:
ollama list
# Should show: nomic-embed-text

What each model does
nomic-embed-text (274MB)  →  converts text to vectors for MongoDB search — THIS is what you install
llama, mistral, etc (4-8GB)  →  full language models — NOT needed, Groq handles this in the cloud


5. Create MongoDB Atlas Vector Search Index
This index tells MongoDB how to search embedding vectors. Must be created once before ingesting documents.

14. Go to your Atlas cluster → Atlas Search tab
15. Click Create Search Index → Atlas Vector Search → JSON Editor
16. Select database: ragdb, collection: document_embeddings
17. Set index name: vector_index
18. Paste the following JSON and click Create:

{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 768,
      "similarity": "cosine"
    }
  ]
}

19. Wait 1-2 minutes until the index status shows Active

Important: numDimensions must be 768
nomic-embed-text produces 768-dimensional vectors.
If you previously used a different embedding model, delete the old index and recreate it.
Mismatched dimensions will cause a vector search error.


6. Configure & Run the Backend
6.1  Set your API keys in application.properties
Open: src/main/resources/application.properties and set:

# Groq API key (from console.groq.com)
app.groq.api-key=gsk_your_key_here

# MongoDB Atlas URI (from Atlas Connect button)
spring.data.mongodb.uri=mongodb+srv://user:pass@cluster.mongodb.net/ragdb

# Ollama runs locally — no key needed
app.ollama.base-url=http://localhost:11434
app.ollama.embedding-model=nomic-embed-text

6.2  Run the backend
20. Open project in IntelliJ
21. File → Settings → Build Tools → Gradle → Gradle JVM → select Java 17
22. Click the Reload Gradle button
23. Run RagApplication.java
24. Verify at: http://localhost:8080/api/diagnostics


7. Ingest Documents into the Knowledge Base
7.1  Using the provided countries dataset
A ready-made dataset of 93 countries with capitals, populations, and facts is included.

25. Make sure Ollama is running and backend is started
26. Place ingest-countries.ps1 and countries_ingest.json in the same folder
27. Open PowerShell and run:
PowerShell -ExecutionPolicy Bypass -File .\ingest-countries.ps1
28. Wait for all 93 countries to show OK
29. Verify at: http://localhost:8080/api/diagnostics — document_chunks should show 93

7.2  Adding your own documents
Send a POST request to add any document to the knowledge base:

POST http://localhost:8080/api/documents/ingest
Content-Type: application/json

{
  "title": "Your Document Title",
  "content": "Full text content of your document...",
  "source": "manual",
  "url": "https://optional-source-url.com"
}

7.3  Using the frontend UI
Click the Add Document button in the top right corner of the web interface to add documents interactively.


8. Run the Frontend
30. Open a terminal in the frontend folder
31. Copy .env.local.example to .env.local
32. Make sure NEXT_PUBLIC_API_URL=http://localhost:8080
33. Run:
npm install
npm run dev
34. Open http://localhost:3000 in your browser


9. Troubleshooting

Component
Details
Groq 401 error
Invalid API key. Check app.groq.api-key in application.properties
MongoDB connection failed
Check MONGODB_URI. Add 0.0.0.0/0 to Atlas IP whitelist
Vector search dimension error
Delete Atlas index and recreate with numDimensions: 768
Ollama embedding failed
Run: ollama serve  and  ollama pull nomic-embed-text
Gradle class file error
Set Gradle JVM to Java 17 in IntelliJ settings
Score below threshold
Documents are in DB but embedding quality is low — re-ingest after verifying Ollama is running
answer from Groq only
No relevant document found in DB. Check document_chunks > 0 at /api/diagnostics


10. Quick Reference

Component
Details
Backend URL
http://localhost:8080
Frontend URL
http://localhost:3000
Diagnostics
http://localhost:8080/api/diagnostics
Ingest endpoint
POST http://localhost:8080/api/documents/ingest
Chat endpoint
POST http://localhost:8080/api/chat
Ollama URL
http://localhost:11434
Groq dashboard
https://console.groq.com
Atlas dashboard
https://cloud.mongodb.com

System is ready when:
http://localhost:8080/api/diagnostics shows: mongodb ✅  local_embeddings ✅  groq_key_set ✅
document_chunks shows 93 (or your number of ingested documents)
Asking a question shows the 🟢 From your database badge with a high score
