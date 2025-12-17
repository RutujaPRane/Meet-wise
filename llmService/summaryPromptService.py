from confluent_kafka import Producer, Consumer
import json
import uuid
import requests
from pymongo import MongoClient

DB_NAME = 'Meetwise'
DOC_COLLECTION = 'transcript-summaries'

client = MongoClient("localhost", 27017)
col = client[DB_NAME][DOC_COLLECTION]

# ---------- OLLAMA CONFIG ----------
OLLAMA_MODEL = "llama3.2:latest"  # change this to whatever model you pulled

# ---------- KAFKA CONFIG ----------
consumeTopic = "llm_service.events.generate.summary"
produceTopic = "summary.generator.events.summary"

consumerConfig = {
    'bootstrap.servers': 'localhost:9092',
    'group.id':          'summary-events',
    'auto.offset.reset': 'earliest'
}

producerConfig = {
    'bootstrap.servers': 'localhost:9092',
    'acks': 'all'
}

consumer = Consumer(consumerConfig)
producer = Producer(producerConfig)

consumer.subscribe([consumeTopic])


def getCompletePrompt(transcript):
    return f"""
You are an assistant that summarizes meeting transcripts.

Transcript:
\"\"\"{transcript}\"\"\"

Write a clear, concise summary of this meeting, focusing on:
- Key decisions
- Important discussions
- Next steps and owners
- Risks or blockers

Return ONLY the summary text. No explanation, no bullet labels, no JSON.
""".strip()


def acked(err, msg):
    if err is not None:
        print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
    else:
        print("Summary message produced: %s" % (str(msg)))


def call_ollama_for_summary(transcript_text):
    prompt = getCompletePrompt(transcript_text)

    payload = {
        "model": OLLAMA_MODEL,
        "prompt": prompt,
        "stream": False
    }

    print(">>> Calling Ollama for SUMMARY...")

    resp = requests.post("http://localhost:11434/api/generate", json=payload)

    print("Ollama HTTP status:", resp.status_code)
    print("Ollama raw body:", resp.text[:300], "..." if len(resp.text) > 300 else "")

    resp.raise_for_status()

    

    raw = resp.json().get("response", "").strip()
    print("Raw Ollama summary:", raw[:200], "..." if len(raw) > 200 else "")
    return raw


def generate_summary_for_file(fileId):
    """Reads transcript for fileId, calls Ollama, saves summary, and notifies Java."""
    document = col.find_one({"_id": fileId})

    if document is None:
        print(f"No transcript document found for fileId={fileId}")
        return

    transcript_data = document.get('transcript')
    if not transcript_data:
        print(f"No transcript field for fileId={fileId}")
        return

    # 1) Call Ollama
    summary_text = call_ollama_for_summary(transcript_data)

    # 2) Update Mongo with summary
    result = col.update_one(
        {"_id": fileId},
        {'$set': {"summary": summary_text}},
        upsert=True
    )
    print("Updated summary document:", result)

    # 3) Produce JSON event { "fileId": "<id>" } for Java
    event = {'fileId': fileId}

    producer.produce(
        produceTopic,
        key=str(uuid.uuid4()),
        value=json.dumps(event),   # <-- plain JSON, NOT jsonpickle
        callback=acked
    )
    producer.poll(1)


# ---------- MAIN LOOP ----------
try:
    print("\n=== SUMMARY WORKER (Ollama) STARTED ===\n")
    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            print("Waiting...")
            continue
        elif msg.error():
            print("ERROR:", msg.error())
            continue
        else:
            print("raw message", msg)

            # Java sends plain JSON bytes → parse with json.loads
            try:
                msg_json = json.loads(msg.value().decode("utf-8"))
            except Exception as e:
                print("Failed to parse summary event JSON:", e, "raw:", msg.value())
                continue

            print("msg_json is", msg_json)

            fileId = msg_json.get('fileId')
            print("got fileId", fileId)

            if not fileId:
                print("No fileId in message, skipping.")
                continue

            generate_summary_for_file(fileId)

except KeyboardInterrupt:
    pass
finally:
    consumer.close()
