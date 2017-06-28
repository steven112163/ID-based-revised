from flask import Flask, jsonify, make_response, abort, request
from flaskext.mysql import MySQL
from datetime import timedelta, datetime
import json

app = Flask( __name__ )
mysql = MySQL()

app.config['MYSQL_DATABASE_HOST'] = '192.168.44.128'
app.config['MYSQL_DATABASE_USER'] = 'root'
app.config['MYSQL_DATABASE_PASSWORD'] = 'root'
app.config['MYSQL_DATABASE_DB'] = 'portal'

mysql.init_app(app)
conn = mysql.connect()
cursor = conn.cursor()

@app.route ( '/query_mac',  methods = [ 'GET' ]) 
def query_mac(): 
    mac = request.args.get('mac')

    cursor.execute("SELECT Enable FROM Registered_MAC WHERE MAC='" + mac + "'")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
	    for row in result:
	        row = dict(zip(columns_name, row))

    return json.dumps(row)

@app.route ( '/insert_mac',  methods = [ 'GET' ])
def insert_mac():
    mac = request.args.get('mac')
    enable = request.args.get('enable')

    cursor.execute("INSERT INTO Registered_MAC (MAC, Enable) VALUES ('" + mac + "', '" + enable + "')")
    conn.commit()

    return "finish"

@app.route ( '/query_ip',  methods = [ 'GET' ]) 
def query_ip(): 
    ip = request.args.get('ip')

    cursor.execute("SELECT MAC FROM IP_MAC WHERE IP='" + ip + "'")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
	    for row in result:
	        row = dict(zip(columns_name, row))

    return json.dumps(row)

@app.route ( '/update_ip',  methods = [ 'GET' ]) 
def update_ip(): 
    ip = request.args.get('ip')
    mac = request.args.get('mac')

    cursor.execute("UPDATE IP_MAC SET MAC='" + mac + "' WHERE IP='" + ip + "'")
    conn.commit()

    return "finish"

@app.route ( '/insert_ip',  methods = [ 'GET' ]) 
def insert_ip(): 
    ip = request.args.get('ip')
    mac = request.args.get('mac')

    cursor.execute("INSERT INTO IP_MAC (IP, MAC) VALUES ('" + ip + "', '" + mac + "')")
    conn.commit()

    return "finish"

@app.route ( '/macToUser',  methods = [ 'GET' ]) 
def macToUser(): 
    mac = request.args.get('mac')

    cursor.execute("SELECT User_ID FROM Registered_MAC WHERE MAC='" + mac + "' and Enable=true")
    result = cursor.fetchall()
    columns_name = [d[0] for d in cursor.description]

    if not result:
        return "empty"
    else:
	    for row in result:
	        row = dict(zip(columns_name, row))

    return json.dumps(row)

@app.route ( '/insert_asso',  methods = [ 'GET' ])
def insert_asso():
    src_mac = request.args.get('src_mac')
    dst_mac = request.args.get('dst_mac')
    src_ip = request.args.get('src_ip')
    dst_ip = request.args.get('dst_ip')
    src_port = request.args.get('src_port')
    dst_port = request.args.get('dst_port')
    protocol = request.args.get('protocol')
    src_user = request.args.get('src_user')
    dst_user = request.args.get('dst_user')
    in_sw = request.args.get('in_sw')
    in_port = request.args.get('in_port')
    time = request.args.get('time')
    access_sw = request.args.get('access_sw')
    access_port = request.args.get('access_port')

    cursor.execute("INSERT INTO Association (Src_MAC, Dst_MAC, Src_IP, Dst_IP, Src_Port, Dst_Port, Protocol, Src_User_ID, Dst_User_ID, Switch_ID, Switch_port, Time, Access_sw, Access_port) VALUES ('" + src_mac + "', '" + dst_mac + "', '" + src_ip + "', '" + dst_ip + "', '" + src_port + "', '" + dst_port + "', '" + protocol + "', '" + src_user + "', '" + dst_user + "', '" + in_sw + "', '" + in_port + "', '" + time + "', '" + access_sw + "', '" + access_port + "')")
    conn.commit()

    return "finish"

if  __name__  ==  '__main__' : 
    app.run ( host = '0.0.0.0', port = 5000, debug = True )
