# Dispute Copilot

> A production-grade AI system for bank back-office agents to handle card transaction disputes.

**Building AI-centric software is 20% AI and 80% engineering: architecture, APIs, testing, observability, security, and cost control.**

---

## Quick Start

```bash
# 1. Set your OpenAI API key
export OPENAI_API_KEY=sk-...

# 2. Start the full stack
docker compose up

# 3. Open the UI
open http://localhost:3000
```

That's it. A colleague can resolve a dispute end-to-end.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Dispute Copilot                               │
│                                                                         │
│  ┌──────────┐    ┌──────────────────────────┐    ┌──────────────────┐  │
│  │ React UI │───▶│  Spring Boot Core Service │───▶│  OpenAI GPT-4o  │  │
│  │ :3000    │    │  :8080                   │    │                  │  │
│  └──────────┘    │                          │    └──────────────────┘  │
│                  │  ┌─────────────────────┐ │                          │
│  ┌──────────┐    │  │  Spring AI 2.0      │ │    ┌──────────────────┐  │
│  │ MCP      │───▶│  │  - ChatClient       │ │───▶│  PostgreSQL      │  │
│  │ Server   │    │  │  - VectorStore(RAG) │ │    │  + pgvector      │  │
│  │ :8002    │    │  │  - Tool Calling     │ │    │  :5432           │  │
│  └──────────┘    │  │  - Memory           │ │    └──────────────────┘  │
│                  │  └─────────────────────┘ │                          │
│  ┌──────────┐    └──────────────────────────┘    ┌──────────────────┐  │
│  │ Document │                                     │  OpenTelemetry  │  │
│  │ Intake   │    ┌──────────────────────────┐    │  Collector       │  │
│  │ :8001    │    │  Eval Suite (CI Gate)    │    │  → Jaeger :16686 │  │
│  │ FastAPI  │    │  50+ golden scenarios    │    └──────────────────┘  │
│  │ Pydantic │    │  LLM-as-judge            │                          │
│  │ AI       │    │  75% pass threshold      │                          │
│  └──────────┘    └──────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Services

| Service | Port | Description |
|---------|------|-------------|
| React UI | 3000 | Ops agent interface for dispute resolution |
| Spring Boot Core | 8080 | Main AI-powered dispute service |
| Document Intake | 8001 | Python/FastAPI — parse PDF/image evidence |
| MCP Server | 8002 | MCP protocol server for agent hosts |
| PostgreSQL + pgvector | 5432 | Domain DB + vector store |
| Jaeger UI | 16686 | Distributed tracing |

---

## Key Features

### AI Assistant
- Conversational endpoint for ops agents (`POST /api/v1/chat`)
- Maintains per-session conversation history
- Every response includes **citations** from policy documents
- Human-in-the-loop: write actions require explicit confirmation

### RAG over Policy Documents
- Visa/Mastercard chargeback rules, reason codes, internal procedures
- Hybrid retrieval (vector + keyword) via pgvector
- Mandatory citations in every answer

### Tool Calling
- `getTransactionHistory` — fetch customer transactions
- `checkScaStatus` — verify SCA/3DS authentication
- `draftChargebackCase` — draft a chargeback (pending confirmation)
- `searchPolicyDocuments` — search the policy knowledge base

### Document Intake (Python)
- Parse PDFs and screenshots into typed `ParsedEvidence` structures
- Pydantic AI for structured extraction
- Architecturally immune to prompt injection (see Threat Model)

### MCP Server
- Exposes full dispute domain to any MCP-compatible agent host
- Test with Claude Desktop or any MCP client

### Eval Suite (CI Gate)
- 50+ golden dispute scenarios across categories: fraud, chargeback, SCA, policy, security
- LLM-as-judge scoring: faithfulness, relevance, citation quality
- 75% pass rate required to merge to main

### Observability
- OpenTelemetry GenAI semantic conventions
- Token usage per conversation logged
- End-to-end traces: Java → Python → OpenAI
- Jaeger for trace visualization

---

## Domain Model

```
Customer
  └── Transaction (scaStatus, threeDsVersion, amount, merchantName)
        └── Dispute (status, reason, reasonCode)
              └── ChargebackCase (pendingConfirmation, reasonCode, evidenceSummary)
```

---

## API Reference

### Chat
```http
POST /api/v1/chat
{
  "sessionId": "string",
  "message": "What's the SCA status for transaction t001?",
  "agentId": "ops-agent-1"
}
```
Response includes `response`, `citations[]`, `pendingActions[]`.

### Disputes
```
GET  /api/v1/disputes
GET  /api/v1/disputes/{id}
POST /api/v1/disputes
PUT  /api/v1/disputes/{id}/confirm-action
```

### Transactions
```
GET /api/v1/transactions/{customerId}
GET /api/v1/transactions/{transactionId}/sca
```

### Document Intake (port 8001)
```
POST /api/v1/documents/parse   (multipart: file, dispute_id)
```

---

## Security

See **[THREAT_MODEL.md](./THREAT_MODEL.md)** for the complete threat analysis.

**TL;DR on prompt injection defense:**
1. Structural isolation: all document content wrapped in `<document_content>` tags
2. System prompt instructs AI to treat document content as data, never instructions
3. Pattern detection flags known injection attempts
4. Human-in-the-loop: write actions require out-of-band confirmation
5. Full audit trail via OpenTelemetry

---

## Development

```bash
# Run dispute service locally
cd dispute-service
mvn spring-boot:run

# Run document intake locally
cd document-intake
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001

# Run eval suite against local service
cd eval-suite
pip install -r requirements.txt
DISPUTE_SERVICE_URL=http://localhost:8080 python eval_suite.py
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | required | OpenAI API key |
| `OPENAI_MODEL` | `gpt-4o` | Chat model |
| `DB_URL` | `jdbc:postgresql://localhost:5432/disputedb` | Database URL |
| `DB_USER` | `disputeuser` | Database user |
| `DB_PASS` | `disputepass` | Database password |
| `OTEL_ENDPOINT` | `http://localhost:4317` | OTLP endpoint |
| `DISPUTE_SERVICE_URL` | `http://localhost:8080` | For MCP server and eval suite |
