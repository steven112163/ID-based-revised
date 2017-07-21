from datetime import datetime
from matplotlib.ticker import MultipleLocator
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import MySQLdb, time, csv, os, re

week = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

start = 0

db=MySQLdb.connect(host="192.168.44.128", user="root", passwd="root", db="portal")
cursor = db.cursor()
cursor.execute("select Asso_ID from Association where Asso_ID > " + str(start) + " order by Asso_ID desc limit 1")
count = cursor.fetchone()

end = start + count[0]

def load_data(user, direction, exec_str1, exec_str2):
    path = "/tmp/result/" + user + "/" + direction
    if not os.path.exists(path):
        os.makedirs(path)

    fileList = os.listdir(path)
    r = re.compile("^test")
    fileList = filter(r.match, fileList)

    count = len(fileList) + 1

    cursor.execute(exec_str1)
    result = cursor.fetchone()

    exist = result[0]

    if(exist == 1):
        cursor.execute(exec_str2)
        result = cursor.fetchall()

        fp = open("/tmp/result/" + user + "/" + direction + "/test" + str(count) + ".csv", "w")
        myFile = csv.writer(fp)
        myFile.writerows(result)
        fp.close()

    return count

def analyze_data(user, direction, count):
    f = open("/tmp/result/" + user + "/" + direction + "/test" + str(count) + ".csv", "r")
    df = pd.read_csv(f, delimiter = ',', names = ["Date", "Time", "Location", "Bytes"])
    f.close()

    location = list(set(df["Location"]))
    for i in range(len(df.index)):
        #df.ix[i,"Date"] = week[datetime.strptime(df.ix[i,"Date"],'%Y-%m-%d').weekday()]
        df.ix[i,"Time"] = int(df.ix[i,"Time"].split(':')[0])

    result = []
    for d in list(set(df["Date"])):
        for h in range(23):
            for l in location:
                df_filter = (df[(df.Date == d) & (df.Time == h) & (df.Location == l)])

                if len(df_filter.index) > 0:
                    #sum_bytes = np.sum(df_filter["Bytes"])
                    mean_bytes = float(np.sum(df_filter["Bytes"])*8/(3600.0*1024.0))
                    result.append([d, h, l, mean_bytes])

    out_result = pd.DataFrame(result, columns=["Date", "Time", "Location", "Kbps"])

    #print out_result

    #out_result = out_result.groupby(["Time", "Location"], as_index=False)["Kbps"].mean()

    #print out_result
    #print

    fp = open("/tmp/result/" + user + "/" + direction + "/result.csv", "w")
    myFile = csv.writer(fp)
    myFile.writerows(result)
    fp.close()

    return out_result

def std_error(numbers, mean):
    total = 0

    for i in numbers:
        total = total + ((i - mean)**2)

    return (total/len(numbers))**0.5

while True:
    cursor.execute("select distinct User_ID from Registered_MAC where User_ID != \"\"")
    result = cursor.fetchall()
    
    if count[0] > 0:
        all_result = pd.DataFrame(columns=["User", "Time", "Location", "Days", "Kbps"])

        for row in result: # each User
            exec_str1 = "select if (exists(select * from Association where Asso_ID > " + str(start) + " \
                and Asso_ID <= " + str(end) + " and Src_User_ID = '" + str(row[0]) + "'), 1, 0)"

            exec_str2 = "select Date, Time, (select Location from Switch where \
                    Switch_ID = Src_access_sw), Bytes from Association where Asso_ID > " + str(start) + " \
                    and Asso_ID <= " + str(end) + " and Switch_ID = Src_access_sw and Switch_port = Src_access_port \
                    and Src_User_ID = '" + str(row[0]) + "' and Bytes != 0"

            out_n = load_data(str(row[0]), "out", exec_str1, exec_str2)


            exec_str1 = "select if (exists(select * from Association where Asso_ID > " + str(start) + " \
                and Asso_ID <= " + str(end) + " and Dst_User_ID = '" + str(row[0]) + "'), 1, 0)"

            exec_str2 = "select Date, Time, (select Location from Switch where \
                Switch_ID = Dst_access_sw), Bytes from Association where Asso_ID > " + str(start) + " \
                and Asso_ID <= " + str(end) + " and Switch_ID = Dst_access_sw \
                and Dst_User_ID = '" + str(row[0]) + "' and Bytes != 0"

            in_n = load_data(str(row[0]), "in", exec_str1, exec_str2)


            out_result = analyze_data(str(row[0]), "out", out_n)
            in_result = analyze_data(str(row[0]), "in", in_n)
            

            total_result = pd.concat([in_result, out_result]).groupby(["Date", "Time", "Location"], as_index=False)["Kbps"].sum()
            total_result.Kbps = total_result.Kbps.astype(float)

            days_count = pd.DataFrame(total_result.groupby(["Time", "Location"]).size().reset_index(name='Count'))

            for i in range(len(total_result.index)):
                for v in days_count[(days_count["Time"] == total_result.ix[i,"Time"]) & (days_count["Location"] == total_result.ix[i,"Location"])]["Count"].values:
                    total_result.ix[i,"Days"] = v

            total_result = total_result.groupby(["Time", "Location", "Days"], as_index=False)["Kbps"].sum()

            path = "/tmp/result/" + str(row[0]) + "/result.csv"
            if os.path.exists(path):
                f = open("/tmp/result/" + str(row[0]) + "/result.csv", "r")
                pre_result = pd.read_csv(f, delimiter = ',', names = ["Time", "Location", "Days", "Kbps"])
                f.close()

                pre_result["Kbps"] = pre_result["Kbps"]*pre_result["Days"]
                
                total_result = pd.concat([total_result, pre_result]).groupby(["Time", "Location"], as_index=False).sum()

            total_result["Kbps"] = total_result["Kbps"]/total_result["Days"]
            total_result = total_result.sort(['Time'], ascending=[True])
            total_result.Kbps = total_result.Kbps.astype(float)
            total_result.to_csv('/tmp/result/' + str(row[0]) + '/result.csv', index=False, header=False)
            total_result["User"] = str(row[0])

            '''
            N = len(set(total_result["Time"]))
            M = int(max(total_result["Kbps"])) + 10

            fig = plt.figure()                                                               
            ax = fig.add_subplot(1,1,1)

            major_ticks_x = np.arange(N)
            ax.set_xticks(major_ticks_x)                                                                                                
            ax.grid(which='both') 
            
            width = 0.3 

            ax.set_title('User ' + str(row[0]))
            ax.set_xlabel('Time')
            ax.set_ylabel('Kbps')  

            #ax.set_xticks(major_ticks_x + width/2)
            #ax.set_xticklabels(list(set(total_result["Time"])))

            #rects = ax.bar(major_ticks_x, total_result["Kbps"], width, color='r')

            def autolabel(rects):
                for rect in rects:
                    height = rect.get_height()
                    ax.text(rect.get_x() + rect.get_width()/2., height+0.5,
                            '%.2f' % float(height), ha='center', va='bottom')
            
            x_label = []

            for t, x in zip(list(set(total_result["Time"])), major_ticks_x):
                df_filter = (total_result[total_result["Time"] == t])
                #print df_filter
                c = 0
                for i in df_filter.index:
                    rects = ax.bar(x + 0.35*c, df_filter.ix[i, "Kbps"], width, color='r')
                    autolabel(rects)
                    c = c + 1
                x_label.append((x + ((c-1)*0.35+0.3)/2))
                
            ax.set_xticks(x_label)
            ax.set_xticklabels(list(set(total_result["Time"])))
            
            #autolabel(rects)

            plt.ylim(0, M, 5)
            plt.show()
            '''
        
            all_result = pd.concat([all_result, total_result], ignore_index=True)
        
        all_result = all_result.drop("Days", 1)

        print all_result
        print

        location = list(set(all_result["Location"]))
        location_result = []
        for h in range(23):
            for l in location:
                df_filter = (all_result[(all_result.Time == h) & (all_result.Location == l)])

                if len(df_filter.index) > 0:
                    numbers = df_filter["Kbps"].values.tolist()
                    mean = np.mean(df_filter["Kbps"])
                    error = std_error(numbers, mean)
                    high = mean + error
                    low = mean - error
                                    
                    for i in df_filter.index:
                        if df_filter.ix[i, "Kbps"] > high:
                            all_result.ix[i, "Priority"] = "High"
                        elif df_filter.ix[i, "Kbps"] < low:
                            all_result.ix[i, "Priority"] = "Low"
                        else:
                            all_result.ix[i, "Priority"] = "Mid"

                    sum_bytes = np.sum(df_filter["Kbps"])
                    location_result.append([h, l, sum_bytes])

        location_result = pd.DataFrame(location_result, columns=["Time", "Location", "Kbps"])

        print all_result
        print

        #print location_result
        #print

    start = end
    time.sleep(3)

    db=MySQLdb.connect(host="192.168.44.128", user="root", passwd="root", db="portal")
    cursor = db.cursor()
    cursor.execute("select count(*) from Association where Asso_ID > " + str(start) + " order by Asso_ID desc limit 1")
    count = cursor.fetchone()

    end = start + count[0]





