from pymongo import MongoClient
from pprint import pprint

week = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

client = MongoClient("mongodb://192.168.44.128:27017/")
db = client.portal
collection = db.Flow
counters = db.counters

db=MySQLdb.connect(host="192.168.44.128", user="root", passwd="root", db="portal")
cursor = db.cursor()

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

