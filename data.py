from datetime import datetime
import pandas as pd
import numpy as np
import csv, os, re


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
dirList = os.listdir('/tmp/result/')
#print dirList

for dir_ in dirList:
    fileList = os.listdir('/tmp/result/' + dir_ + '/in/')
    r = re.compile("^test")
    fileList = filter(r.match, fileList)
    #print fileList
    
    for file_ in fileList:
        f = open('/tmp/result/' + dir_ + '/in/' +file_ , 'r')
        df = pd.read_csv(f, delimiter = ',', names = ["Week", "Time", "Location", "Bytes"])
        #df = pd.DataFrame(columns=["Week", "Time", "Location", "Bytes"])
        f.close()
        #print df

        location = list(set(df["Location"]))

        for i in range(len(df.index)):
            df.ix[i,"Week"] = week[datetime.strptime(df.ix[i,"Week"],'%Y-%m-%d').weekday()]
            df.ix[i,"Time"] = int(df.ix[i,"Time"].split(':')[0])
        #print df

        result = []
        for w in week:
            #if not os.path.exists('/tmp/result/0456502/in/'+w):
                #os.makedirs('/tmp/result/0456502/in/'+w)

            for h in range(23):
                for l in location:
                    #result = []
                    df_filter = (df[(df.Week == w) & (df.Time == h) & (df.Location == l)])

                    if len(df_filter.index) > 0:
                        mean_bytes = '{:.2f}'.format(np.mean(df_filter["Bytes"])*8/3600)
                        result.append([w, h, l, mean_bytes])

        df_result = pd.DataFrame(result, columns=["Week", "Time", "Location", "bps"])
        #print df_result
        df_result.to_csv('/tmp/result/' + dir_ + '/in/result.csv', index=False)

    
    fileList = os.listdir('/tmp/result/' + dir_ + '/out/')
    r = re.compile("^test")
    fileList = filter(r.match, fileList)
    #print fileList
    
    for file_ in fileList:
        f = open('/tmp/result/' + dir_ + '/out/' +file_ , 'r')
        df = pd.read_csv(f, delimiter = ',', names = ["Week", "Time", "Location", "Bytes"])
        #df = pd.DataFrame(columns=["Week", "Time", "Location", "Bytes"])
        f.close()
        #print df

        location = list(set(df["Location"]))

        for i in range(len(df.index)):
            df.ix[i,"Week"] = week[datetime.strptime(df.ix[i,"Week"],'%Y-%m-%d').weekday()]
            df.ix[i,"Time"] = int(df.ix[i,"Time"].split(':')[0])
        #print df

        result = []
        for w in week:
            #if not os.path.exists('/tmp/result/0456502/in/'+w):
                #os.makedirs('/tmp/result/0456502/in/'+w)

            for h in range(23):
                for l in location:
                    #result = []
                    df_filter = (df[(df.Week == w) & (df.Time == h) & (df.Location == l)])

                    if len(df_filter.index) > 0:
                        mean_bytes = '{:.2f}'.format(np.mean(df_filter["Bytes"])*8/3600)
                        result.append([w, h, l, mean_bytes])

        df_result = pd.DataFrame(result, columns=["Week", "Time", "Location", "bps"])
        #print df_result
        df_result.to_csv('/tmp/result/' + dir_ + '/out/result.csv', index=False)
            
                
            
        
