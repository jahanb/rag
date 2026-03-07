# Next.js Frontend — Setup Guide

## Step 1: Prerequisites

Make sure you have installed:
- **Node.js 20+** → https://nodejs.org (choose LTS)
- **npm** (comes with Node.js)

Verify:
```bash
node --version   # should be v20+
npm --version    # should be v9+
```

---

## Step 2: Create the Next.js project

You have two options:

### Option A — Use these files directly (recommended)

Copy the `frontend/` folder to your project location (e.g. `C:\1MAR2026\rag\frontend\`).

### Option B — Scaffold fresh then copy files

If you want to start from a clean Next.js scaffold:

```bash
cd C:\1MAR2026\rag
npx create-next-app@14.2.3 frontend --typescript --tailwind --eslint --app --no-src-dir --import-alias "@/*"
```

Then **replace** these files with the ones provided:
```
frontend/
  app/
    globals.css          ← replace
    layout.tsx           ← replace
    page.tsx             ← replace
    lib/
      api.ts             ← add this file
    components/
      SourceCards.tsx    ← add this file
      IngestPanel.tsx    ← add this file
      StatusBar.tsx      ← add this file
  tailwind.config.js     ← replace
  next.config.mjs        ← replace
  package.json           ← replace (then run npm install)
```

---

## Step 3: Install dependencies

```bash
cd frontend
npm install
```

This installs all packages from `package.json` including:
- `next` `react` `react-dom`
- `axios` — HTTP client to call Spring Boot backend
- `react-markdown` — renders Gemini's markdown responses
- `uuid` — generates session IDs
- `lucide-react` — icons
- `tailwindcss` — styling

---

## Step 4: Configure environment

Create the env file:

```bash
# In the frontend/ directory:
copy .env.local.example .env.local    # Windows
# or
cp .env.local.example .env.local      # Mac/Linux
```

Edit `.env.local`:
```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Change the URL if your Spring Boot backend runs on a different port.

---

## Step 5: Run in development mode

Make sure your **Spring Boot backend is running first** on port 8080, then:

```bash
npm run dev
```

Open → **http://localhost:3000**

---

## Step 6: Build for production

```bash
npm run build
npm start
```

---

## Project structure

```
frontend/
├── app/
│   ├── layout.tsx              # Root HTML layout
│   ├── page.tsx                # Main chat interface
│   ├── globals.css             # Global styles + design tokens
│   ├── lib/
│   │   └── api.ts              # Typed API client (axios)
│   └── components/
│       ├── SourceCards.tsx     # KB + web source badges
│       ├── IngestPanel.tsx     # Add documents modal
│       └── StatusBar.tsx       # Backend health indicator
├── next.config.mjs             # Next.js config + API proxy
├── tailwind.config.js          # Tailwind + custom tokens
├── postcss.config.js           # PostCSS config
├── tsconfig.json               # TypeScript config
└── package.json                # Dependencies
```

---

## Features

| Feature | Description |
|--------|-------------|
| 💬 Chat | Multi-turn conversation with session memory |
| 🔍 Web search toggle | Enable/disable Google search augmentation per query |
| 📚 Document ingest | Paste text + title to add to the knowledge base |
| 📎 Source attribution | See which KB chunks and web pages were used |
| 🟢 Health indicator | Live backend connectivity status in header |
| ⌨️ Keyboard shortcuts | Enter to send, Shift+Enter for newline |
| 🎨 Terminal aesthetic | Dark theme with neon accents, JetBrains Mono font |

---

## Troubleshooting

**CORS error in browser console**
→ Make sure `CORS_ORIGINS=http://localhost:3000` is set in your Spring Boot env

**"backend:offline" in header**
→ Spring Boot is not running or not on port 8080. Check `NEXT_PUBLIC_API_URL` in `.env.local`

**Module not found errors after npm install**
→ Delete `node_modules` and `.next`, then run `npm install` again

**Port 3000 already in use**
→ Run `npm run dev -- --port 3001` and update `CORS_ORIGINS` in backend accordingly
