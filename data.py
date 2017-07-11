from datetime import datetime
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
        df.ix[i,"Date"] = week[datetime.strptime(df.ix[i,"Date"],'%Y-%m-%d').weekday()]
        df.ix[i,"Time"] = int(df.ix[i,"Time"].split(':')[0])

    result = []
    for w in week:
        for h in range(23):
            for l in location:
                df_filter = (df[(df.Date == w) & (df.Time == h) & (df.Location == l)])

                if len(df_filter.index) > 0:
                    mean_bytes = float('{:.2f}'.format(np.sum(df_filter["Bytes"])*8/(3600.0*1024.0)))
                    result.append([w, h, l, mean_bytes])

    out_result = pd.DataFrame(result, columns=["Week", "Time", "Location", "Kbps"])

    fp = open("/tmp/result/" + user + "/" + direction + "/result.csv", "w")
    myFile = csv.writer(fp)
    myFile.writerows(result)
    fp.close()

    return out_result

while True:
    cursor.execute("select distinct User_ID from Registered_MAC where User_ID != \"\"")
    result = cursor.fetchall()
    
    if count[0] > 0:
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
            

            total_result = pd.concat([in_result, out_result]).groupby(["Week", "Time", "Location"], as_index=False)["Kbps"].sum()
            
            total_result["Kbps"] = total_result["Kbps"].map('{:.2f}'.format)
            total_result.Kbps = total_result.Kbps.astype(float)

            path = "/tmp/result/" + str(row[0]) + "/result.csv"
            if os.path.exists(path):
                f = open("/tmp/result/" + str(row[0]) + "/result.csv", "r")
                pre_result = pd.read_csv(f, delimiter = ',', names = ["Week", "Time", "Location", "Kbps"])
                f.close()

                total_result = pd.concat([total_result, pre_result]).groupby(["Week", "Time", "Location"], as_index=False)["Kbps"].mean()
            
            total_result.to_csv('/tmp/result/' + str(row[0]) + '/result.csv', index=False, header=False)
            total_result["User"] = str(row[0])

    '''
    N = len(total_result.index)
    men_means = (1,2,3,4,5)
    #men_std = (2, 3, 4, 1, 2)

    ind = np.arange(N)  # the x locations for the groups
    width = 0.35       # the width of the bars

    fig, ax = plt.subplots()

    rects1 = ax.bar(ind, total_result["Kbps"], width, color='r')

    ax.set_ylabel('Kbps')
    ax.set_title('One User')
    ax.set_xticks(ind + width/2)
    ax.set_xticklabels(total_result["Time"])

    def autolabel(rects):
        for rect in rects:
            height = rect.get_height()
            ax.text(rect.get_x() + rect.get_width()/2., 1.05*height,
                    '%.2f' % float(height),
                    ha='center', va='bottom')

    autolabel(rects1)

    #plt.plot(x,y)
    #plt.xlim(9,12)
    #plt.ylim(0,50)

    #plt.xlabel("Time Interval") 
    #plt.ylabel("Kbps") 
    #plt.title("One User")

    plt.show()
    '''
    
    start = end
    time.sleep(3)

    db=MySQLdb.connect(host="192.168.44.128", user="root", passwd="root", db="portal")
    cursor = db.cursor()
    cursor.execute("select count(*) from Association where Asso_ID > " + str(start) + " order by Asso_ID desc limit 1")
    count = cursor.fetchone()

    end = start + count[0]





