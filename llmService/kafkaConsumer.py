""" #!/usr/bin/env python

from confluent_kafka import Consumer

if __name__ == '__main__':

    config = {
        # User-specific properties that you must set
        'bootstrap.servers': 'localhost:9092',

        # Fixed properties
        'group.id':          'kafka-python-getting-started',
        'auto.offset.reset': 'earliest'
    }

    # Create Consumer instance
    consumer = Consumer(config)

    # Subscribe to topic
    topic = "purchases"
    consumer.subscribe([topic])

    # Poll for new messages from Kafka and print them.
    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                # Initial message consumption may take up to
                # `session.timeout.ms` for the consumer group to
                # rebalance and start consuming
                print("Waiting...")
            elif msg.error():
                print("ERROR: %s".format(msg.error()))
            else:
                print("raw message", msg)
                # Extract the (optional) key and value, and print.
                print("Consumed event from topic {topic}: key = {key:12} value = {value:12}".format(
                    topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))
    except KeyboardInterrupt:
        pass
    finally:
        # Leave group and commit final offsets
        consumer.close() """




""" #!/usr/bin/env python

from confluent_kafka import Consumer
import jsonpickle

if __name__ == '__main__':

    config = {
        'bootstrap.servers': 'localhost:9092',
        'group.id':          'llm-service-action-items',
        'auto.offset.reset': 'earliest',   # good for dev
    }

    consumer = Consumer(config)

    # ✅ Subscribe to the same topic Spring / your test producer uses
    topic = "llm_service.events.generate.action.items"
    consumer.subscribe([topic])

    try:
        while True:
            msg = consumer.poll(1.0)

            if msg is None:
                print("Waiting...")
                continue

            if msg.error():
                print(f"ERROR: {msg.error()}")
                continue

            raw_value = msg.value().decode("utf-8")
            event = jsonpickle.decode(raw_value)



            print(f"\nConsumed event from {msg.topic()}:")
            print("  key:  ", msg.key().decode("utf-8") if msg.key() else None)
            print("  value:", event)

            # 👉 here you would call the LLM, generate summary/action items,
            #    then produce to 'summary.generator.events.action.items'

    except KeyboardInterrupt:
        pass
    finally:
        consumer.close() """



#!/usr/bin/env python

from confluent_kafka import Consumer, Producer
import json
import jsonpickle

BOOTSTRAP_SERVERS = "localhost:9092"

INPUT_TOPIC = "llm_service.events.generate.action.items"
OUTPUT_TOPIC = "summary.generator.events.action.items"


def build_consumer():
    config = {
        "bootstrap.servers": BOOTSTRAP_SERVERS,
        "group.id": "llm-service-action-items",
        "auto.offset.reset": "earliest",
    }
    consumer = Consumer(config)
    consumer.subscribe([INPUT_TOPIC])
    return consumer


def build_producer():
    return Producer({"bootstrap.servers": BOOTSTRAP_SERVERS, "acks": "all"})


def generate_action_items(event: dict):
    """TEMP: replace with real LLM logic later."""
    chunk = event.get("chunk", "")
    return [
        {
            "title": "Prepare project timeline",
            "owner": "Alice",
            "description": chunk[:120],
            "dueDate": None,
        },
        {
            "title": "Review budget",
            "owner": "Bob",
            "description": chunk[:120],
            "dueDate": None,
        },
    ]


if __name__ == "__main__":
    consumer = build_consumer()
    producer = build_producer()

    print(f"Listening on topic {INPUT_TOPIC} ...")

    try:
        while True:
            msg = consumer.poll(1.0)

            if msg is None:
                print("Waiting...")
                continue

            if msg.error():
                print(f"ERROR: {msg.error()}")
                continue

            raw_value = msg.value().decode("utf-8")
            print("GOT MESSAGE:", raw_value)

            # decode JSON / jsonpickle
            try:
                event = jsonpickle.decode(raw_value)
            except Exception:
                event = json.loads(raw_value)

            # 👉 call your LLM / prompt service here
            action_items = generate_action_items(event)

            # Build outgoing payload that Spring Boot will consume
            out = {
                "fileId": event.get("fileId"),
                "chunkId": event.get("chunkId"),
                "prevId": event.get("prevId"),
                "actionItems": action_items,
            }

            out_json = json.dumps(out)
            producer.produce(
                OUTPUT_TOPIC,
                key=str(event.get("fileId", "")),
                value=out_json,
            )
            producer.flush()

            print(f"Produced to {OUTPUT_TOPIC}: {out_json}")

    except KeyboardInterrupt:
        print("Stopping...")
    finally:
        consumer.close()
