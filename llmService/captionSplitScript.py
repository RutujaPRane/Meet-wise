from datetime import datetime
import re
import json
import sys

time_format = "%H:%M:%S"

def createSplits(fileName, outputFile):
    captions = open(fileName, "r")

    prevTime = None
    meetingExtracts = []
    temp = []
    output = open(outputFile, "w")

    for i in captions.readlines():
        time = i[:8]
        text = i[9:].replace("\n", "")
        
        timeObj = datetime.strptime(time, time_format)
        if prevTime is None:
            prevTime = timeObj

        difference = timeObj - prevTime
        
        temp.append(text)

        if difference.total_seconds() >= 60.0:
            output.write(' '.join(temp) + '\n')
            meetingExtracts.append(' '.join(temp))
            temp = [text]
            prevTime = timeObj
    
    meetingExtracts.append(' '.join(temp))
    output.write(' '.join(temp) + '\n')
    output.close()


if __name__ == '__main__':
    _, fileName, outputFileName = sys.argv
    createSplits(fileName, outputFileName)