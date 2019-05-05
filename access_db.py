from flask import Flask, jsonify, make_response, abort, request
from flaskext.mysql import MySQL
from datetime import timedelta, datetime
from pymongo import MongoClient
from pprint import pprint
import json

app = Flask( __name__ )
mysql = MySQL()

app.config['MYSQL_DATABASE_HOST'] = '127.0.0.1'
app.config['MYSQL_DATABASE_USER'] = 'root'
app.config['MYSQL_DATABASE_PASSWORD'] = '!1Qazwsxedc'
app.config['MYSQL_DATABASE_DB'] = 'portal'

mysql.init_app(app)
conn = mysql.connect()
cursor = conn.cursor()

client = MongoClient("mongodb://127.0.0.1:27017/")
db = client.portal
collection = db.Flow
counters = db.counters

@app.route ( '/macToGroup',  methods = [ 'GET' ]) 
def macToGroup(): 
    mac = request.args.get('mac')

    cursor.execute("SELECT Group_ID FROM Registered_MAC WHERE MAC='" + mac + "' and Enable = 1")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)



@app.route ( '/query_acl',  methods = [ 'GET' ]) 
def query_acl(): 
    group_id = request.args.get('group_id')
    ip = request.args.get('ip')
    cursor.execute("SELECT * FROM ACL_Group_IP WHERE IP='" + ip + "' and Group_ID='" + group_id + "'")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)



@app.route ( '/query_mac',  methods = [ 'GET' ]) 
def query_mac(): 
    mac = request.args.get('mac')

    cursor.execute("SELECT Enable FROM Registered_MAC WHERE MAC='" + mac + "'")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)

@app.route ( '/insert_mac',  methods = [ 'GET' ])
def insert_mac():
    mac = request.args.get('mac')
    user = ''
    group = ''
    enable = request.args.get('enable')

    cursor.execute("INSERT INTO Registered_MAC (MAC, User_ID, Group_ID, Enable) VALUES ('" + mac + "', '" + user + "', '" + group + "', '" + enable + "')")
    conn.commit()

    return "finish"

@app.route ( '/query_ip',  methods = [ 'GET' ]) 
def query_ip(): 
    ip = request.args.get('ip')

    cursor.execute("SELECT MAC FROM IP_MAC WHERE IP='" + ip + "'")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)

@app.route ( '/update_ip',  methods = [ 'GET' ]) 
def update_ip(): 
    ip = request.args.get('ip')
    mac = request.args.get('mac')
    time = request.args.get('time')

    cursor.execute("UPDATE IP_MAC SET MAC='" + mac + "', Time='" + time + "' WHERE IP='" + ip + "'")
    conn.commit()

    return "finish"

@app.route ( '/insert_ip',  methods = [ 'GET' ]) 
def insert_ip(): 
    ip = request.args.get('ip')
    mac = request.args.get('mac')
    time = request.args.get('time')

    cursor.execute("INSERT INTO IP_MAC (IP, MAC, Time) VALUES ('" + ip + "', '" + mac + "', '" + time + "')")
    conn.commit()

    return "finish"

@app.route ( '/macToUser',  methods = [ 'GET' ]) 
def macToUser(): 
    mac = request.args.get('mac')

    cursor.execute("SELECT User_ID FROM Registered_MAC WHERE MAC='" + mac + "' and Enable=true")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)

@app.route ( '/macToUG',  methods = [ 'GET' ]) 
def macToUG(): 
    mac = request.args.get('mac')

    cursor.execute("SELECT User_ID, Group_ID FROM Registered_MAC WHERE MAC='" + mac + "' AND Enable = 1")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)

@app.route ( '/userToMac',  methods = [ 'GET' ]) 
def userToMac(): 
    user_id = request.args.get('user_id')

    cursor.execute("SELECT MAC FROM Registered_MAC WHERE User_ID='" + user_id + "' AND Enable = 1")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    result_list = []
    for row in result:
        result_list.append(dict(zip(columns_name, row)))

    return json.dumps(result_list)

@app.route ( '/groupToMac',  methods = [ 'GET' ]) 
def groupToMac(): 
    group_id = request.args.get('group_id')

    cursor.execute("SELECT MAC FROM Registered_MAC WHERE Group_ID='" + group_id + "' AND Enable = 1")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    result_list = []
    for row in result:
        result_list.append(dict(zip(columns_name, row)))

    return json.dumps(result_list)

@app.route ( '/insertACL',  methods = [ 'GET' ])
def insertACL():
    acl_id = request.args.get('acl_id')
    src_attr = request.args.get('src_attr')
    src_id = request.args.get('src_id')
    ip = request.args.get('ip')
    port = request.args.get('port')
    proto_type = request.args.get('proto_type')
    permission = request.args.get('permission')
    priority = request.args.get('priority')

    cursor.execute("INSERT INTO Access_control (ACL_ID, Src_attr, Src_ID, Dst_IP, Dst_Port, Protocol, Permission, Priority) VALUES \
    ('" + acl_id + "', '" + src_attr + "', '" + src_id + "', '" + ip + "', '" + port + "', '" + proto_type + "', '" + permission + "', '" + priority + "')")
    conn.commit()

    return "finish"

@app.route ( '/removeACL',  methods = [ 'GET' ]) 
def removeACL(): 
    acl_id = request.args.get('acl_id')

    cursor.execute("DELETE FROM Access_control WHERE ACL_ID='" + acl_id + "'")
    conn.commit()

    return "finish"

@app.route ( '/getAcl',  methods = [ 'GET' ]) 
def getAcl(): 
    cursor.execute("SELECT * FROM Access_control")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    result_list = []
    for row in result:
        result_list.append(dict(zip(columns_name, row)))

    return json.dumps(result_list)

@app.route ( '/swToLocation',  methods = [ 'GET' ]) 
def swToLocation(): 
    sw = request.args.get('sw')

    cursor.execute("SELECT Building, Room FROM Switch WHERE Switch_ID='" + sw + "'")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)

@app.route ( '/flowClassToUserCount',  methods = [ 'GET' ]) 
def flowClassToUserCount(): 
    building = request.args.get('building')
    room = request.args.get('room')
    time_interval = request.args.get('time_interval')
    week = request.args.get('week')

    cursor.execute("SELECT User_ID, Bwd_req FROM Flow_classification WHERE Building='" + building + "' and Room='" + room + \
    "' and Time_period='" + time_interval + "' and Week='" + week + "'")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    result_list = []  
    if not result:
        return "empty"
    else:
        for row in result:
            result_list.append(dict(zip(columns_name, row)))

    return json.dumps(result_list)

@app.route ( '/userToPriority',  methods = [ 'GET' ]) 
def userToPriority(): 
    user = request.args.get('user')
    building = request.args.get('building')
    room = request.args.get('room')
    time_interval = request.args.get('time_interval')
    week = request.args.get('week')


    cursor.execute("SELECT Bwd_req FROM Flow_classification WHERE Building='" + building + "' and Room='" + room + \
    "' and Time_period='" + time_interval + "' and User_ID='" + user + "' and Week='" + week + "'")
    result = cursor.fetchone()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
        row = dict(zip(columns_name, result))
        return json.dumps(row)

@app.route ( '/buildingToPercent',  methods = [ 'GET' ]) 
def buildingToPercent(): 
    time_interval = request.args.get('time_interval')
    week = request.args.get('week')

    cursor.execute("SELECT Building, Percentage FROM Area_flow WHERE Time_period='" + time_interval + "' and Week='" + week + "'")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    result_list = []  
    if not result:
        return "empty"
    else:
        for row in result:
            result_list.append(dict(zip(columns_name, row)))

    return json.dumps(result_list)

@app.route ( '/update_bytes',  methods = [ 'GET' ])
def update_bytes():
    src_mac = request.args.get('src_mac')
    dst_mac = request.args.get('dst_mac')
    src_ip = request.args.get('src_ip')
    dst_ip = request.args.get('dst_ip')
    src_port = request.args.get('src_port')
    dst_port = request.args.get('dst_port')
    protocol = request.args.get('protocol')
    swId = request.args.get('swId')
    port = request.args.get('port')
    bytes = request.args.get('bytes')

    collection.find_and_modify(
        query={
            'Src_MAC': src_mac,
            'Dst_MAC': dst_mac,
            'Src_IP': src_ip,
            'Dst_IP': dst_ip,
            'Src_port': src_port,
            'Dst_port': dst_port,
            'Protocol': protocol,
            'Switch_ID': swId,
            'Switch_port': port
        },
        update={
            '$inc': { 'Bytes': long(bytes) }
        },
        sort={ '_id': -1 }
    )

    return "finish"

@app.route ( '/insertFlow',  methods = [ 'GET' ]) 
def insertFlow():
    src_mac = request.args.get('src_mac')
    dst_mac = request.args.get('dst_mac')
    src_ip = request.args.get('src_ip')
    dst_ip = request.args.get('dst_ip')
    src_port = request.args.get('src_port')
    dst_port = request.args.get('dst_port')
    protocol = request.args.get('protocol')
    swId = request.args.get('swId')
    port = request.args.get('port')
    src_user = request.args.get('src_user')
    dst_user = request.args.get('dst_user')
    src_swId = request.args.get('src_swId')
    src_access_port = request.args.get('src_access_port')
    dst_swId = request.args.get('dst_swId')
    dst_access_port = request.args.get('dst_access_port')
    data = request.args.get('date')
    time = request.args.get('time')

    collection.insert_one(
        {
            '_id': getNextSequence("flowId"),
            'Src_MAC': src_mac,
            'Dst_MAC': dst_mac,
            'Src_IP': src_ip,
            'Dst_IP': dst_ip,
            'Src_port': src_port,
            'Dst_port': dst_port,
            'Protocol': protocol,
            'Switch_ID': swId,
            'Switch_port': port,
            'Src_User_ID': src_user,
            'Dst_User_ID': dst_user,
            'Src_access_sw': src_swId,
            'Src_access_port': src_access_port,
            'Dst_access_sw': dst_swId,
            'Dst_access_port': dst_access_port,
            'Date': data,
            'Time': time,
            'Bytes': long(0)
        })

    return "finish"

def getNextSequence(flowId):
    # $inc: increase
    counters.update_one({ '_id': flowId }, {'$inc': { 'seq': 1 }}, upsert=True)
    result = counters.find_one({ '_id': flowId })
    return result['seq']

if  __name__  ==  '__main__' : 
    app.run ( host = '0.0.0.0', port = 5000, debug = True )
