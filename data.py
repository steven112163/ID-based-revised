from datetime import datetime
import pandas as pd
import numpy as np
import csv


#f = open('/tmp/0456502/out/test1.csv', 'r')

week = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
'''
l1=[]
l2=[]
for row in csv.reader(f):
    l1.append(week[datetime.strptime(row[0],'%Y-%m-%d').weekday()])
    l1.append(int(row[1].split(':')[0]))
    l1.append(row[2])
    l1.append(row[3])
    l2.append(l1)
    l1=[]
f.close()
'''
f = open('/tmp/0456502/out/test1.csv', 'r')
df = pd.read_csv(f, delimiter = ',', names = ["Week", "Time", "Location", "Bytes"])
#df = pd.DataFrame(columns=["Week", "Time", "Location", "Bytes"])
f.close()
print df

location = list(set(df["Location"]))

total_bytes = 0
total_bytes_list = []

for i in range(len(df.index)):
    df.ix[i,"Week"] = week[datetime.strptime(df.ix[i,"Week"],'%Y-%m-%d').weekday()]
    df.ix[i,"Time"] = int(df.ix[i,"Time"].split(':')[0])
print df

result = []
for w in week:
    for h in range(23):
        for l in location:
            df_filter = (df[(df.Week == w) & (df.Time == h) & (df.Location == l)])

            if len(df_filter.index) > 0:
                total_bytes = np.sum(df_filter["Bytes"]) 
                result.append([w, h, l,total_bytes])

df_result = pd.DataFrame(result, columns=["Week", "Time", "Location", "Total Bytes"])
print df_result
            
        
