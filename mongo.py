from pymongo import MongoClient
from pprint import pprint

client = MongoClient("mongodb://192.168.44.128:27017/")
db = client.QQ
uu = db.uu
counters = db.counters

def getNextSequence(name):
    counters.update_one({ '_id': name }, {'$inc': { 'seq': 1 }}, upsert=True)
    result = counters.find_one({ '_id': name })
    return result['seq']

uu.insert_one(
    {
        '_id': getNextSequence("userid"),
        'name': "Sarah C."
    })

for r in uu.find():
    print r

