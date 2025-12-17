from confluent_kafka import Producer, Consumer
from pymongo import MongoClient
from bson.objectid import ObjectId

import json
import uuid
import requests

# ========= CONFIG =========

DB_NAME = "Meetwise"
DOC_COLLECTION = "action-items"

KAFKA_BOOTSTRAP = "localhost:9092"
CONSUME_TOPIC = "llm_service.events.generate.action.items"
PRODUCE_TOPIC = "summary.generator.events.action.items"

OLLAMA_URL = "http://localhost:11434/api/generate"
OLLAMA_MODEL = "llama3.2:latest"   # make sure this matches `ollama list`

# ========= SETUP =========

client = MongoClient("localhost", 27017)
col = client[DB_NAME][DOC_COLLECTION]

consumer_config = {
    "bootstrap.servers": KAFKA_BOOTSTRAP,
    "group.id": "action-items-worker",
    "auto.offset.reset": "earliest",
}

producer_config = {
    "bootstrap.servers": KAFKA_BOOTSTRAP,
    "acks": "all",
}

consumer = Consumer(consumer_config)
producer = Producer(producer_config)
consumer.subscribe([CONSUME_TOPIC])

print(">>> Running action_items_worker.py (with light cleaning) <<<")


# ========= LLM PROMPT + CALL =========

def get_complete_prompt(prev_items, transcript):
    return f"""
You are an assistant that extracts ACTION ITEMS from a meeting transcript.

Return a JSON ARRAY ONLY, no explanation, in this exact format:
[
  {{
    "issueType": "Bug | Task | Subtask | Epic | Story",
    "assignee": "Name of the person (or 'Unassigned')",
    "priority": "Highest | High | Medium | Low | Lowest",
    "description": "Full description of the action item",
    "summary": "Short title of the action item"
  }},
  ...
]

Previous action items (you should avoid duplicates if possible):
{json.dumps(prev_items, ensure_ascii=False)}

Transcript:
{transcript}
""".strip()


def call_ollama_for_action_items(transcript_chunk, previous_items):
    prompt = get_complete_prompt(previous_items, transcript_chunk)

    payload = {
        "model": OLLAMA_MODEL,
        "prompt": prompt,
        "stream": False,
    }

    print(">>> Calling Ollama for action items...")
    resp = requests.post(OLLAMA_URL, json=payload)
    print("Ollama HTTP status:", resp.status_code)
    print("Ollama raw body:", resp.text[:500], "...\n")

    resp.raise_for_status()
    data = resp.json()
    content = (data.get("response") or "").strip()

    # try to parse JSON directly
    try:
        parsed = json.loads(content)
        if isinstance(parsed, list):
            return parsed
        else:
            print("Parsed Ollama response is not a list, got:", type(parsed))
            return []
    except Exception as e:
        print("Failed to parse Ollama JSON, returning empty list. Error:", e)
        return []


# ========= LIGHT NORMALISATION (NO FILTERING) =========

def clean_items(items):
    """
    Do NOT drop items. Just make sure each one has the fields
    your UI / Jira pipeline expect.
    """
    cleaned = []

    for raw in items:
        if not isinstance(raw, dict):
            continue

        issue_type = str(raw.get("issueType", "Task")).strip() or "Task"
        assignee = str(raw.get("assignee", "Unassigned")).strip() or "Unassigned"
        priority = str(raw.get("priority", "Medium")).strip() or "Medium"

        description = (raw.get("description") or "").strip()
        summary = (raw.get("summary") or "").strip()

        # Guarantee summary
        if not summary:
            if description:
                summary = description[:60]
            else:
                summary = "Action Item"

        # Guarantee description
        if not description:
            description = summary

        cleaned.append({
            "issueType": issue_type,
            "assignee": assignee,
            "priority": priority,
            "description": description,
            "summary": summary,
            # if you ever want to pass through extra fields (like jiraIssueUrl later),
            # you could merge raw into this dict instead of recreating it.
        })

    return cleaned


# ========= MAIN ACTION ITEM HANDLER =========

def extract_action_item(transcript_chunk, previous_action_items, chunk_id, file_id):
    items = call_ollama_for_action_items(transcript_chunk, previous_action_items)
    print("LLM response (parsed):", items)

    # --- NEW: normalise but do not drop items ---
    items = clean_items(items)
    print("Cleaned items:", items)

    # Save whatever we got (even empty list)
    try:
        result = col.update_one(
            {"_id": ObjectId(chunk_id)},
            {"$set": {"actionItems": items}},
            upsert=True,
        )
        print("updated the document", result)
    except Exception as e:
        print("Failed to update Mongo document for chunk", chunk_id, "Error:", e)

    # produce minimal event for Java consumer
    event = {
        "fileId": file_id,
        "chunkId": chunk_id,
    }

    payload = json.dumps(event)
    print(">>> Producing to topic:", PRODUCE_TOPIC, "event:", payload)

    producer.produce(
        PRODUCE_TOPIC,
        key=str(uuid.uuid4()),
        value=payload,
    )
    producer.poll(1)


# ========= MAIN LOOP =========

try:
    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            print("Waiting...")
            continue

        if msg.error():
            print("ERROR from Kafka:", msg.error())
            continue

        print("raw message", msg)
        try:
            msg_json = json.loads(msg.value().decode("utf-8"))
        except Exception as e:
            print("Failed to decode Kafka message value:", msg.value(), "Error:", e)
            continue

        print("msg_json is", msg_json)

        transcript_data = msg_json.get("chunk", "")
        prev_id = msg_json.get("prevId")
        current_id = msg_json.get("chunkId")
        file_id = msg_json.get("fileId")

        print("got prevId", prev_id)

        prev_action_list = []
        if prev_id:
            try:
                prev_doc = col.find_one({"_id": ObjectId(prev_id)})
                print("fetched prevDocument", prev_doc)
                if prev_doc and "actionItems" in prev_doc:
                    prev_action_list = prev_doc["actionItems"]
            except Exception as e:
                print("Could not fetch previous document for prevId", prev_id, "Error:", e)

        extract_action_item(transcript_data, prev_action_list, current_id, file_id)

except KeyboardInterrupt:
    print("Shutting down worker...")
finally:
    consumer.close()
    producer.flush()
