"""
Dispute Copilot MCP Server
Exposes the dispute domain to any MCP-compatible agent host.
Uses the FastMCP library for a clean Python MCP implementation.
"""
import os
import httpx
from typing import Optional
from mcp.server.fastmcp import FastMCP

DISPUTE_SERVICE_URL = os.getenv("DISPUTE_SERVICE_URL", "http://dispute-service:8080")

mcp = FastMCP("Dispute Copilot")

# ---------------------------------------------------------------------------
# HTTP client helper
# ---------------------------------------------------------------------------

def api_get(path: str) -> dict:
    with httpx.Client(base_url=DISPUTE_SERVICE_URL, timeout=10) as client:
        resp = client.get(path)
        resp.raise_for_status()
        return resp.json()


def api_post(path: str, body: dict) -> dict:
    with httpx.Client(base_url=DISPUTE_SERVICE_URL, timeout=10) as client:
        resp = client.post(path, json=body)
        resp.raise_for_status()
        return resp.json()


def api_put(path: str, body: dict = None) -> dict:
    with httpx.Client(base_url=DISPUTE_SERVICE_URL, timeout=10) as client:
        resp = client.put(path, json=body or {})
        resp.raise_for_status()
        return resp.json()


# ---------------------------------------------------------------------------
# MCP Tools
# ---------------------------------------------------------------------------

@mcp.tool()
def list_disputes(status: Optional[str] = None) -> list[dict]:
    """
    List all card transaction disputes.
    
    Args:
        status: Optional filter. One of: OPEN, UNDER_REVIEW, RESOLVED_WIN, RESOLVED_LOSS, WITHDRAWN
    
    Returns:
        List of dispute objects with id, status, reason, customerId, transactionId.
    """
    path = "/api/v1/disputes"
    if status:
        path += f"?status={status}"
    return api_get(path)


@mcp.tool()
def get_dispute(dispute_id: str) -> dict:
    """
    Get full details of a specific dispute.
    
    Args:
        dispute_id: UUID of the dispute
    
    Returns:
        Full dispute object including status, reason, chargeback case info.
    """
    return api_get(f"/api/v1/disputes/{dispute_id}")


@mcp.tool()
def get_transaction_history(customer_id: str, limit: int = 20) -> list[dict]:
    """
    Retrieve transaction history for a customer.
    
    Args:
        customer_id: Customer identifier
        limit: Maximum number of transactions to return (default 20)
    
    Returns:
        List of transactions with amount, merchant, date, SCA status.
    """
    return api_get(f"/api/v1/transactions/{customer_id}?limit={limit}")


@mcp.tool()
def check_sca_status(transaction_id: str) -> dict:
    """
    Check the Strong Customer Authentication (SCA) / 3DS status for a transaction.
    
    Args:
        transaction_id: UUID of the transaction
    
    Returns:
        SCA status details including authentication method and 3DS version.
    """
    return api_get(f"/api/v1/transactions/{transaction_id}/sca")


@mcp.tool()
def chat_with_assistant(session_id: str, message: str, agent_id: str = "mcp-client") -> dict:
    """
    Send a message to the AI dispute resolution assistant.
    
    Args:
        session_id: Conversation session ID (use consistent ID for multi-turn)
        message: The message/question for the assistant
        agent_id: Identifier for the agent making the request
    
    Returns:
        Response with assistant message, citations, and any pending actions.
    """
    return api_post("/api/v1/chat", {
        "sessionId": session_id,
        "message": message,
        "agentId": agent_id,
    })


@mcp.tool()
def draft_chargeback_case(
    dispute_id: str,
    reason_code: str,
    evidence_summary: str,
) -> dict:
    """
    Draft a chargeback case for a dispute. REQUIRES human confirmation before filing.
    
    Args:
        dispute_id: UUID of the dispute
        reason_code: Visa/Mastercard chargeback reason code (e.g. "4853", "UA02")
        evidence_summary: Summary of supporting evidence
    
    Returns:
        Draft chargeback case. NOTE: pendingConfirmation=true until confirmed via confirm_action.
    """
    return api_post(f"/api/v1/disputes/{dispute_id}/chargeback", {
        "reasonCode": reason_code,
        "evidenceSummary": evidence_summary,
    })


@mcp.tool()
def confirm_action(dispute_id: str, action_id: str) -> dict:
    """
    Confirm a pending action (human-in-the-loop approval).
    This is required before write actions (like filing a chargeback) are executed.
    
    Args:
        dispute_id: UUID of the dispute
        action_id: ID of the pending action to confirm
    
    Returns:
        Updated dispute status after action execution.
    """
    return api_put(f"/api/v1/disputes/{dispute_id}/confirm-action", {"actionId": action_id})


@mcp.tool()
def search_policy_documents(query: str) -> list[dict]:
    """
    Search the internal policy knowledge base (chargeback rules, reason codes, procedures).
    
    Args:
        query: Natural language query about dispute policies or procedures
    
    Returns:
        List of relevant policy document excerpts with citations.
    """
    return api_post("/api/v1/documents/search", {"query": query})


# ---------------------------------------------------------------------------
# MCP Resources
# ---------------------------------------------------------------------------

@mcp.resource("disputes://active")
def active_disputes_resource() -> str:
    """Current list of active (OPEN + UNDER_REVIEW) disputes."""
    disputes = api_get("/api/v1/disputes?status=OPEN") + api_get("/api/v1/disputes?status=UNDER_REVIEW")
    lines = [f"- [{d['id']}] {d.get('reason','?')} ({d.get('status','?')})" for d in disputes]
    return "\n".join(lines) if lines else "No active disputes."


@mcp.resource("policies://chargeback-codes")
def chargeback_codes_resource() -> str:
    """Reference list of Visa and Mastercard chargeback reason codes."""
    return """
# Visa Chargeback Reason Codes
- 10.1: EMV Liability Shift - Card Present (Counterfeit)
- 10.2: EMV Liability Shift - Card Present (Lost/Stolen)
- 10.3: Other Fraud - Card Present
- 10.4: Other Fraud - Card Absent
- 10.5: Visa Fraud Monitoring Program
- 11.1: Card Recovery Bulletin
- 11.2: Declined Authorization
- 11.3: No Authorization
- 12.1: Late Presentment
- 12.2: Incorrect Transaction Code
- 12.3: Incorrect Currency
- 12.4: Incorrect Account Number
- 12.5: Incorrect Amount
- 12.6.1: Duplicate Processing
- 12.6.2: Paid by Other Means
- 12.7: Invalid Data
- 13.1: Merchandise/Services Not Received
- 13.2: Cancelled Recurring Transaction
- 13.3: Not as Described or Defective Merchandise/Services
- 13.4: Counterfeit Merchandise
- 13.5: Misrepresentation
- 13.6: Credit Not Processed
- 13.7: Cancelled Merchandise/Services
- 13.8: Original Credit Transaction Not Accepted
- 13.9: Non-receipt of Cash or Load Transaction Value

# Mastercard Chargeback Reason Codes
- 4808: Authorization-Related Chargeback
- 4834: Point-of-Interaction Error
- 4853: Cardholder Dispute
- 4855: Goods or Services Not Provided
- 4859: Addendum, No-show, or ATM Dispute
- 4863: Cardholder Does Not Recognize
- 4870: Chip Liability Shift
- 4871: Chip/PIN Liability Shift
- UA01: Fraud (Card Present)
- UA02: Fraud (Card Not Present)
- UA05: Issuer Fraud Alert
- UA06: Issuer Fraud Monitoring
- UA10: Request Transaction Receipt
- UA11: Cardholder Inquiry/Dispute - Fraud
- UA18: Non-Receipt of Cash
- UA28: Request for T&E Documentation
- UA30: Fraudulent Processing of Transactions
- UA31: Transaction Amount Altered
- UA38: Fraudulent Processing of Transactions (Repeat)
"""


if __name__ == "__main__":
    mcp.run()
