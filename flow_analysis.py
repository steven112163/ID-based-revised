import time
import sched  
import MySQLdb
import datetime
import numpy as np
import pandas as pd
from pymongo import MongoClient

client = MongoClient('mongodb://192.168.44.128:27017/')
db = client.portal
collection = db.Flow

db=MySQLdb.connect(host='192.168.44.128', user='root', passwd='root', db='portal')
cursor = db.cursor()

week = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

start = 0
count = collection.find_one(sort=[('_id', -1)])['_id']
end = count

def loadFlow():
    data = []
    for flow in collection.find({'_id': {'$gt': start, '$lte': end}}):
        if (flow['Src_User_ID'] != '' and flow['Switch_ID'] == flow['Src_access_sw'] and flow['Switch_port'] == flow['Src_access_port'] and flow['Bytes'] != 0):
            cursor.execute('select Building, Room from Switch where Switch_ID = "' + flow['Src_access_sw'] + '"')
            result = cursor.fetchall()
            building = result[0][0]
            room = result[0][1]

            data.append([flow['Src_User_ID'], flow['Date'], flow['Time'], building, room, flow['Bytes']])

        elif (flow['Dst_User_ID'] != '' and flow['Switch_ID'] == flow['Dst_access_sw'] and flow['Bytes'] != 0):
            cursor.execute('select Building, Room from Switch where Switch_ID = "' + flow['Dst_access_sw'] + '"')
            result = cursor.fetchall()
            building = result[0][0]
            room = result[0][1]

            data.append([flow['Dst_User_ID'], flow['Date'], flow['Time'], building, room, flow['Bytes']])

    allFlow = pd.DataFrame(data, columns=['User', 'Date', 'Time', 'Building', 'Room', 'Bytes'])
    print allFlow
    return allFlow

def calculateAverage(allFlow):
    for i in range(len(allFlow.index)):
        allFlow.ix[i,'Time'] = int(allFlow.ix[i,'Time'].split(':')[0])

    allFlow.columns = ['User', 'Date', 'TimePeriod', 'Building', 'Room', 'Bytes']

    averageFlow = allFlow.groupby(['User', 'Date', 'TimePeriod', 'Building', 'Room'], as_index=False)['Bytes'].sum()
    averageFlow['Bytes'] = (averageFlow['Bytes']*8)/(3600.0*1024.0)
    averageFlow.columns = ['User', 'Date', 'TimePeriod', 'Building', 'Room', 'Kbps']
    
    for i in range(len(averageFlow.index)):
        averageFlow.ix[i,'Week'] = week[datetime.date(int(averageFlow.ix[i,'Date'].split('-')[0]), \
        int(averageFlow.ix[i,'Date'].split('-')[1]), int(averageFlow.ix[i,'Date'].split('-')[2])).isocalendar()[2]-1]

    columns = ['User', 'Date', 'Week', 'TimePeriod', 'Building', 'Room', 'Kbps']
    averageFlow = averageFlow[columns]

    dayCounts = averageFlow.groupby(['User', 'Week', 'TimePeriod', 'Building', 'Room']).size().reset_index(name='DayCounts')
    averageFlow = averageFlow.groupby(['User', 'Week', 'TimePeriod', 'Building', 'Room'], as_index=False)['Kbps'].sum()
    averageFlow['DayCounts'] = dayCounts['DayCounts']
    averageFlow['Kbps'] = averageFlow['Kbps'] / averageFlow['DayCounts']

    print averageFlow
    return averageFlow

def loadOldResult(averageFlow):
    cursor.execute('select User_ID, Week, Time_period, Building, Room, Kbps, Day_counts from Flow_classification')
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return averageFlow

    data = []
    for row in result:
        row = dict(zip(columns_name, row))
        data.append([row['User_ID'], row['Week'], row['Time_period'], row['Building'], row['Room'], row['Kbps'], row['Day_counts']])

    oldResult = pd.DataFrame(data, columns=['User', 'Week', 'TimePeriod', 'Building', 'Room', 'Kbps', 'DayCounts'])

    oldResult['Kbps'] = oldResult['Kbps'] * oldResult['DayCounts']
    averageFlow['Kbps'] = averageFlow['Kbps'] * averageFlow['DayCounts']

    averageFlow = pd.concat([averageFlow, oldResult]).groupby(['User', 'Week', 'TimePeriod', 'Building', 'Room'], as_index=False).sum()
    averageFlow['Kbps'] = averageFlow['Kbps'] / averageFlow['DayCounts']

    print averageFlow
    return averageFlow

def classifyFlow(averageFlow):
    userCounts = averageFlow.groupby(['Week', 'TimePeriod', 'Building', 'Room']).size().reset_index(name='UserCounts')
    stdError = averageFlow.groupby(['Week', 'TimePeriod', 'Building', 'Room'], as_index=False)['Kbps'].sum()

    stdError['MeanKbps'] = stdError['Kbps'] / userCounts['UserCounts']
    columns = ['Week', 'TimePeriod', 'Building', 'Room', 'MeanKbps']
    stdError = stdError[columns]
    #print stdError

    for i in range(len(stdError.index)):
        flowFilter = (averageFlow[(averageFlow.Week == stdError.ix[i,'Week']) & (averageFlow.TimePeriod == stdError.ix[i,'TimePeriod']) & \
        (averageFlow.Building == stdError.ix[i,'Building']) & (averageFlow.Room == stdError.ix[i,'Room'])])
        #print flowFilter

        numbers = flowFilter["Kbps"].values.tolist()
        mean = stdError.ix[i,'MeanKbps']
        error = calculateStdError(numbers, mean)
        high = mean + error
        low = mean - error

        stdError.ix[i,'StdError'] = error
        stdError.ix[i,'HighThreshold'] = high
        stdError.ix[i,'LowThreshold'] = low

        for j in flowFilter.index:
            if averageFlow.ix[j,'Kbps'] >= high:
                averageFlow.ix[j,'BwdReq'] = 'High'
            elif averageFlow.ix[j,'Kbps'] <= low:
                averageFlow.ix[j,'BwdReq'] = 'Low'
            else:
                averageFlow.ix[j,'BwdReq'] = 'Mid'

    print averageFlow
    return averageFlow

def predictFlow(averageFlow):
    predictResult = averageFlow.groupby(['Week', 'TimePeriod', 'Building'], as_index=False)['Kbps'].sum()
    sumKbps = np.sum(predictResult['Kbps'])

    predictResult['Percentage'] = predictResult['Kbps'] / sumKbps * 100

    return predictResult
    print predictResult
    

def calculateStdError(numbers, mean):
    total = 0
    for i in numbers:
        total = total + ((i - mean)**2)

    return (total/len(numbers))**0.5

while True:
    if start != end:
        allFlow = loadFlow()
        averageFlow = calculateAverage(allFlow)
        averageFlow = loadOldResult(averageFlow)
        classifyResult = classifyFlow(averageFlow)
        predictResult = predictFlow(averageFlow)

        print classifyResult
        print predictResult

    start = end
    time.sleep(3)
    count = collection.find_one(sort=[('_id', -1)])['_id']
    end = count
