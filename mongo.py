from pymongo import MongoClient
from pprint import pprint

client = MongoClient("mongodb://192.168.44.128:27017/")
db = client.portal
collection = db.Flow
counters = db.counters

def getNextSequence(flowId):
    # $inc: increase
    counters.update_one({ '_id': flowId }, {'$inc': { 'seq': 1 }}, upsert=True)
    result = counters.find_one({ '_id': flowId })
    return result['seq']

collection.insert_one(
    {
        '_id': getNextSequence("flowId"),
        'name': "Sarah C."
    })

for r in collection.find():
    print r

