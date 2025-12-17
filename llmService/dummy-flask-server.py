from flask import Flask, request, Response, send_file
import jsonpickle
import base64
import os
from confluent_kafka import Producer
import uuid

app = Flask(__name__)

config = {
        'bootstrap.servers': 'localhost:9092',
        'acks': 'all'
    }

producer = Producer(config)

topic = "transcript-action-item"

def log(l):
    print(l)

def delivery_callback(err, msg):
    if err:
        print('ERROR: Message failed delivery: {}'.format(err))
    else:
        print("Produced event to topic {topic}: key = {key:12} value = {value:12}".format(
            topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))

def publish_query(caption, prevActionItem):

    log("publishing the query for " + caption + ", prevAction " + ''.join(prevActionItem))

    rec = {
        'text': caption,
        'prevAction': prevActionItem
    }

    producer.produce(topic, key=str(uuid.uuid4()), value=jsonpickle.encode(rec), callback=delivery_callback)

    producer.poll(1)

@app.rout('/')


@app.route('/transcript', methods=['POST'])
def getActionItems():
    log("received transcripts")
    # request_body = jsonpickle.decode(request.data)
    
    # transcript_data = request_body['transcript']
    print("files", request.files)
    transcriptFile = request.files['file']
    transcriptFile.save("transcriptFiles/raw_transcript.txt")

    os.system("python3 captionSplitScript.py transcriptFiles/raw_transcript.txt transcriptFiles/output.txt")

    with open("transcriptFiles/output.txt", "r") as file:
        first_line = file.readline()
        publish_query(first_line, [])

    response = {
        'data' : 'received files'
    }

    return Response(response=jsonpickle.encode(response), status=200, mimetype="application/json")

app.run(host="0.0.0.0", port=6000)