from confluent_kafka import Producer, Consumer
import json
import uuid
import vertexai
from vertexai.generative_models import GenerativeModel, GenerationConfig
from pymongo import MongoClient
from bson.objectid import ObjectId

DB_NAME = 'Meetwise'
DOC_COLLECTION = 'action-items'

client = MongoClient("localhost", 27017)
col = client[DB_NAME][DOC_COLLECTION]

# init Vertex (keep it even if you're using dummy response now)
vertexai.init(project="assignmentdatacenterscale", location="us-central1")
model = GenerativeModel("gemini-1.5-flash-002")

consumeTopic = "llm_service.events.generate.action.items"
produceTopic = "summary.generator.events.action.items"

consumerConfig = {
    'bootstrap.servers': 'localhost:9092',
    'group.id':          'kafka-python-getting-started',
    'auto.offset.reset': 'earliest'
}

producerConfig = {
    'bootstrap.servers': 'localhost:9092',
    'acks': 'all'
}

consumer = Consumer(consumerConfig)
producer = Producer(producerConfig)
consumer.subscribe([consumeTopic])


def getCompletePrompt(actionItemList, transcript):
    promptTemplate = f"""Give a list of action items from the given Transcript. The list should not include an action item if PrevActionItemList contains an item with similar Description. The list should be in the same format as ActionItemList and follow the given Constraints.
ActionItemList: [{{"issueType": "type of task", "assignee": "Name of the person assigned the task", "priority": "Priority of task", "description": "Description of task", "summary": "Title of the Task"}}]
Constraints: issueType can be one of [Task, Epic, Subtask, Story, Bug] and priority can be one of [Highest, High, Medium, Low, Lowest]
PrevActionItemList: {actionItemList}
Transcript: {transcript}
NewActionItemList:
"""
    return promptTemplate


def acked(err, msg):
    if err is not None:
        print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
    else:
        print("Message produced: %s" % (str(msg)))


def extractActionItem(transcriptChunk, previousActionItems, id, fileId):
    # This is your dummy response for now
    response_text = [
        {
            "issueType": "Task",
            "assignee": "Alice",
            "priority": "High",
            "description": "Prepare the project timeline",
            "summary": "Project timeline preparation"
        },
        {
            "issueType": "Task",
            "assignee": "Bob",
            "priority": "Medium",
            "description": "Review the budget",
            "summary": "Budget review"
        }
    ]

    print("Dummy response is", response_text)

    # Save simulated response into Mongo
    result = col.update_one(
        {"_id": ObjectId(id)},
        {'$set': {"actionItems": response_text}},
        upsert=True
    )
    print("updated the document", result)

    # Produce minimal event that matches ActionItemProcessedEvent(fileId, chunkId)
    event = {
        'fileId': fileId,
        'chunkId': id
    }

    producer.produce(
        produceTopic,
        key=str(uuid.uuid4()),
        value=json.dumps(event),  # <-- plain JSON now
        callback=acked
    )
    producer.poll(1)


try:
    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            print("Waiting...")
        elif msg.error():
            print("ERROR:", msg.error())
        else:
            print("raw message", msg)

            # Java producer will send plain JSON bytes, so decode with json.loads
            msg_json = json.loads(msg.value().decode("utf-8"))
            print("msg_json is", msg_json)

            transcript_data = msg_json['chunk']
            prevId = msg_json['prevId']
            currentId = msg_json['chunkId']
            fileId = msg_json['fileId']

            print("got prevId", prevId)

            prevActionList = []
            prevDocument = None

            if prevId:
                try:
                    prevDocument = col.find_one({"_id": ObjectId(prevId)})
                    print("fetched prevDocument", prevDocument)
                except Exception as e:
                    print("Could not convert prevId to ObjectId:", prevId, e)

            if prevDocument is not None and 'actionItems' in prevDocument:
                prevActionList = prevDocument['actionItems']
                print("got prevActionItems", prevActionList)

            extractActionItem(transcript_data, prevActionList, currentId, fileId)

except KeyboardInterrupt:
    pass
finally:
    consumer.close()
