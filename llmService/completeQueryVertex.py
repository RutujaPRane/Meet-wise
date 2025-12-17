import vertexai
from vertexai.generative_models import GenerativeModel, ChatSession
from datetime import datetime
import re
import json

time_format = "%H:%M:%S"

captions = open("caption.txt", "r")
prevTime = None
meetingExtracts = []
temp = []

vertexai.init(project="assignmentdatacenterscale", location="us-central1")

model = GenerativeModel("gemini-1.5-flash-002")

for i in captions.readlines():
    time = i[:8]
    text = i[9:].replace("\n", "")

    print(text)
    
    timeObj = datetime.strptime(time, time_format)
    if prevTime is None:
        prevTime = timeObj

    difference = timeObj - prevTime
    print("diff", difference.total_seconds())
    temp.append(text)
    if difference.total_seconds() >= 60.0:
        print("1 min done")
        meetingExtracts.append(' '.join(temp))
        temp = [text]
        prevTime = timeObj
    
meetingExtracts.append(' '.join(temp))

taskList = []
prevTaskList = []

def getCompletePrompt(actionItemList, transcript):
    promptTemplate = f"""Give a list of action items from the given Transcript. The list should not include an action item if PrevActionItemList contains an item with similar Description. The list should be in the same format as ActionItemList.
    ActionItemList: {[{"type": "type of task", "Assignee": "Name of the person assigned the task", "Priority": "Priority of task", "Description": "Description of task"}]}
    PrevActionItemList: {actionItemList}
    Transcript: {transcript}
    NewActionItemList: 
    """

    return promptTemplate

def extractActionItem(transcriptChunk, previousActionItems):
    prompt = getCompletePrompt(previousActionItems, transcriptChunk)
    print("prompt is", prompt)
    response = model.generate_content(prompt)
    newF = open("temp.json", "w")
    newF.write(re.sub("(NewActionItemList:)", "", re.sub(r"'", '"', re.sub(r"[```](json)?", "", response.candidates[0].text))))

    print("the result is", re.sub("(NewActionItemList:)", "", re.sub(r"'", '"', re.sub(r"[```](json)?", "", response.candidates[0].text))))
    newF.close()
    
prevChunk = ""
overallActionList = []
prevActionList = ""

for i in meetingExtracts:
    extractActionItem(prevChunk + i, prevActionList)

    with open('temp.json', 'r') as file:
        prevActionList = json.load(file)
        overallActionList.append(prevActionList)
    
    prevChunk = i