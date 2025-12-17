from confluent_kafka import Producer
import jsonpickle
import uuid

producer_config = {
    'bootstrap.servers': 'localhost:9092',
    'acks': 'all'
}

producer = Producer(producer_config)

# Topic your consumer listens to
topic = "llm_service.events.generate.action.items"

# Fake transcript chunk
message = {
    "chunk": "In today’s meeting, Alice will prepare the project timeline and Bob will review the budget.",
    "prevId": "651234abcd1234567890abcd",  # placeholder ObjectId
    "chunkId": "651234abcd1234567890abce", # new placeholder ObjectId
    "fileId": "meetingFile001"
}

producer.produce(topic, key=str(uuid.uuid4()), value=jsonpickle.encode(message))
producer.flush()

print(f"Produced event to topic {topic}: {message}")







""" #!/usr/bin/env python

from random import choice
from confluent_kafka import Producer

if __name__ == '__main__':

    config = {
        # User-specific properties that you must set
        'bootstrap.servers': 'localhost:9092',

        # Fixed properties
        'acks': 'all'
    }

    # Create Producer instance
    producer = Producer(config)

    # Optional per-message delivery callback (triggered by poll() or flush())
    # when a message has been successfully delivered or permanently
    # failed delivery (after retries).
    def delivery_callback(err, msg):
        if err:
            print('ERROR: Message failed delivery: {}'.format(err))
        else:
            print("Produced event to topic {topic}: key = {key:12} value = {value:12}".format(
                topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))

    # Produce data by selecting random values from these lists.
    topic = "purchases"
    user_ids = ['eabara', 'jsmith', 'sgarcia', 'jbernard', 'htanaka', 'awalther']
    products = ['book', 'alarm clock', 't-shirts', 'gift card', 'batteries']

    count = 0
    for _ in range(10):
        user_id = choice(user_ids)
        product = choice(products)
        producer.produce(topic, product, user_id, callback=delivery_callback)
        count += 1

    # Block until the messages are sent.
    producer.poll(10000)
    # producer.flush() """