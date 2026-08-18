import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import ReactMarkdown from 'react-markdown';

const API_URL = process.env.REACT_APP_API_URL || '';
const api = axios.create({ baseURL: API_URL });

// ─── Utilities ───────────────────────────────────────────────────────────────
function generateSessionId() {
  return 'sess-' + Math.random().toString(36).slice(2);
}

// ─── Components ──────────────────────────────────────────────────────────────

function DisputeList({ disputes, onSelect, selectedId }) {
  const statusColor = {
    OPEN: '#3b82f6',
    UNDER_REVIEW: '#f59e0b',
    RESOLVED_WIN: '#10b981',
    RESOLVED_LOSS: '#ef4444',
    WITHDRAWN: '#6b7280',
  };

  return (
    <div style={{ height: '100%', overflowY: 'auto' }}>
      <h3 style={{ padding: '12px 16px', margin: 0, borderBottom: '1px solid #e5e7eb', fontSize: 14, fontWeight: 600 }}>
        Active Disputes
      </h3>
      {disputes.length === 0 && (
        <p style={{ padding: '16px', color: '#6b7280', fontSize: 13 }}>No disputes found</p>
      )}
      {disputes.map(d => (
        <div
          key={d.id}
          onClick={() => onSelect(d)}
          style={{
            padding: '12px 16px',
            cursor: 'pointer',
            borderBottom: '1px solid #f3f4f6',
            background: selectedId === d.id ? '#eff6ff' : 'white',
            borderLeft: selectedId === d.id ? '3px solid #3b82f6' : '3px solid transparent',
          }}
        >
          <div style={{ fontSize: 12, color: '#6b7280', fontFamily: 'monospace' }}>{d.id?.slice(0, 8)}…</div>
          <div style={{ fontSize: 13, fontWeight: 500, marginTop: 2 }}>{d.reason || 'No reason'}</div>
          <div style={{ marginTop: 4 }}>
            <span style={{
              fontSize: 11,
              padding: '2px 8px',
              borderRadius: 9999,
              background: statusColor[d.status] + '20',
              color: statusColor[d.status],
              fontWeight: 600,
            }}>
              {d.status}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

function ChatMessage({ msg }) {
  const isUser = msg.role === 'user';
  return (
    <div style={{
      display: 'flex',
      justifyContent: isUser ? 'flex-end' : 'flex-start',
      marginBottom: 12,
    }}>
      <div style={{
        maxWidth: '70%',
        padding: '10px 14px',
        borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
        background: isUser ? '#3b82f6' : '#f3f4f6',
        color: isUser ? 'white' : '#111827',
        fontSize: 14,
        lineHeight: 1.5,
      }}>
        {isUser ? msg.content : (
          <ReactMarkdown>{msg.content}</ReactMarkdown>
        )}
        {msg.citations && msg.citations.length > 0 && (
          <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px solid rgba(0,0,0,0.1)' }}>
            <div style={{ fontSize: 11, fontWeight: 600, color: '#6b7280', marginBottom: 4 }}>CITATIONS</div>
            {msg.citations.map((c, i) => (
              <div key={i} style={{ fontSize: 11, color: '#374151', marginBottom: 2 }}>
                [{i + 1}] {typeof c === 'string' ? c : (c.source || JSON.stringify(c))}
              </div>
            ))}
          </div>
        )}
        {msg.pendingActions && msg.pendingActions.length > 0 && (
          <div style={{ marginTop: 8, padding: 8, background: '#fef3c7', borderRadius: 8, border: '1px solid #f59e0b' }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: '#92400e', marginBottom: 4 }}>⚠️ ACTION REQUIRES YOUR CONFIRMATION</div>
            {msg.pendingActions.map((a, i) => (
              <div key={i} style={{ fontSize: 12, color: '#78350f' }}>{JSON.stringify(a)}</div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ChatPanel({ dispute }) {
  const [sessionId] = useState(generateSessionId);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (dispute) {
      setMessages([{
        role: 'assistant',
        content: `I'm ready to help you resolve dispute **${dispute.id?.slice(0, 8)}**.\n\nStatus: **${dispute.status}**\nReason: ${dispute.reason || 'Not specified'}\n\nWhat would you like to investigate?`,
        citations: [],
      }]);
    }
  }, [dispute?.id]);

  const sendMessage = async () => {
    if (!input.trim() || loading) return;
    const userMsg = input.trim();
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: userMsg }]);
    setLoading(true);

    try {
      const { data } = await api.post('/api/v1/chat', {
        sessionId,
        message: userMsg,
        agentId: 'ops-ui',
      });
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: data.response || '(no response)',
        citations: data.citations || [],
        pendingActions: data.pendingActions || [],
      }]);
    } catch (err) {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: `❌ Error: ${err.message}`,
        citations: [],
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKey = e => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  if (!dispute) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#6b7280' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>💬</div>
          <div>Select a dispute to start chatting</div>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <div style={{ padding: '12px 16px', borderBottom: '1px solid #e5e7eb', background: 'white' }}>
        <div style={{ fontSize: 13, fontWeight: 600 }}>Dispute {dispute.id?.slice(0, 8)}</div>
        <div style={{ fontSize: 12, color: '#6b7280' }}>{dispute.reason}</div>
      </div>

      {/* Messages */}
      <div style={{ flex: 1, overflowY: 'auto', padding: 16, background: '#fafafa' }}>
        {messages.map((m, i) => <ChatMessage key={i} msg={m} />)}
        {loading && (
          <div style={{ display: 'flex', justifyContent: 'flex-start', marginBottom: 12 }}>
            <div style={{ padding: '10px 14px', background: '#f3f4f6', borderRadius: '16px 16px 16px 4px', fontSize: 14, color: '#6b7280' }}>
              Thinking…
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div style={{ padding: 12, borderTop: '1px solid #e5e7eb', background: 'white', display: 'flex', gap: 8 }}>
        <textarea
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKey}
          placeholder="Ask about this dispute… (Enter to send)"
          style={{
            flex: 1,
            padding: '8px 12px',
            borderRadius: 8,
            border: '1px solid #d1d5db',
            resize: 'none',
            fontSize: 14,
            fontFamily: 'inherit',
            outline: 'none',
            height: 60,
          }}
        />
        <button
          onClick={sendMessage}
          disabled={loading || !input.trim()}
          style={{
            padding: '8px 20px',
            background: loading || !input.trim() ? '#9ca3af' : '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
            fontSize: 14,
            fontWeight: 600,
            alignSelf: 'flex-end',
          }}
        >
          Send
        </button>
      </div>
    </div>
  );
}

function DocumentUpload({ disputeId }) {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const upload = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    const form = new FormData();
    form.append('file', file);
    if (disputeId) form.append('dispute_id', disputeId);

    try {
      const { data } = await axios.post(
        (process.env.REACT_APP_INTAKE_URL || 'http://localhost:8001') + '/api/v1/documents/parse',
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } }
      );
      setResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <h4 style={{ marginTop: 0, fontSize: 14, fontWeight: 600 }}>Upload Evidence</h4>
      <input type="file" accept=".pdf,.png,.jpg,.jpeg" onChange={e => setFile(e.target.files[0])} />
      <button
        onClick={upload}
        disabled={!file || loading}
        style={{ marginLeft: 8, padding: '4px 12px', background: '#3b82f6', color: 'white', border: 'none', borderRadius: 6, cursor: 'pointer' }}
      >
        {loading ? 'Parsing…' : 'Parse'}
      </button>

      {error && <div style={{ color: '#ef4444', marginTop: 8, fontSize: 13 }}>Error: {error}</div>}

      {result && (
        <div style={{ marginTop: 12, background: '#f9fafb', borderRadius: 8, padding: 12, fontSize: 13 }}>
          <div><strong>Category:</strong> {result.parsed?.evidence_category}</div>
          <div style={{ marginTop: 4 }}><strong>Summary:</strong> {result.parsed?.summary}</div>
          {result.parsed?.red_flags?.length > 0 && (
            <div style={{ marginTop: 8, color: '#ef4444' }}>
              <strong>⚠️ Red Flags:</strong>
              <ul style={{ margin: '4px 0', paddingLeft: 20 }}>
                {result.parsed.red_flags.map((f, i) => <li key={i}>{f}</li>)}
              </ul>
            </div>
          )}
          {result.parsed?.key_facts?.length > 0 && (
            <div style={{ marginTop: 8 }}>
              <strong>Key Facts:</strong>
              <ul style={{ margin: '4px 0', paddingLeft: 20 }}>
                {result.parsed.key_facts.map((f, i) => <li key={i}>{f}</li>)}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Main App ─────────────────────────────────────────────────────────────────

export default function App() {
  const [disputes, setDisputes] = useState([]);
  const [selectedDispute, setSelectedDispute] = useState(null);
  const [activeTab, setActiveTab] = useState('chat');

  useEffect(() => {
    api.get('/api/v1/disputes')
      .then(r => setDisputes(r.data || []))
      .catch(() => setDisputes([]));
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', fontFamily: 'system-ui, sans-serif' }}>
      {/* Top Bar */}
      <div style={{ padding: '0 20px', background: '#1e3a5f', color: 'white', display: 'flex', alignItems: 'center', height: 52, flexShrink: 0 }}>
        <div style={{ fontWeight: 700, fontSize: 18 }}>🏦 Dispute Copilot</div>
        <div style={{ marginLeft: 8, fontSize: 12, opacity: 0.7 }}>Bank Operations — Card Dispute Resolution</div>
      </div>

      {/* Main layout */}
      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        {/* Left sidebar: dispute list */}
        <div style={{ width: 280, flexShrink: 0, borderRight: '1px solid #e5e7eb', background: 'white', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <DisputeList
            disputes={disputes}
            onSelect={setSelectedDispute}
            selectedId={selectedDispute?.id}
          />
        </div>

        {/* Main content */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          {/* Tabs */}
          {selectedDispute && (
            <div style={{ display: 'flex', borderBottom: '1px solid #e5e7eb', background: 'white', paddingLeft: 16 }}>
              {['chat', 'evidence'].map(tab => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  style={{
                    padding: '10px 16px',
                    border: 'none',
                    background: 'none',
                    cursor: 'pointer',
                    fontSize: 13,
                    fontWeight: activeTab === tab ? 600 : 400,
                    color: activeTab === tab ? '#3b82f6' : '#6b7280',
                    borderBottom: activeTab === tab ? '2px solid #3b82f6' : '2px solid transparent',
                    textTransform: 'capitalize',
                  }}
                >
                  {tab === 'chat' ? '💬 Chat' : '📎 Evidence'}
                </button>
              ))}
            </div>
          )}

          {/* Tab content */}
          <div style={{ flex: 1, overflow: 'hidden' }}>
            {activeTab === 'chat' ? (
              <ChatPanel dispute={selectedDispute} />
            ) : (
              <DocumentUpload disputeId={selectedDispute?.id} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
