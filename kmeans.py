from sklearn.cluster import KMeans
from datetime import datetime
import matplotlib.pyplot as plt
import numpy as np
import csv

l1=[]
l2=[]
f = open('/tmp/0456501/out/test1.csv', 'r')
for row in csv.reader(f):
    l1.append(datetime.strptime(row[0],'%Y-%m-%d').weekday())
    l1.append(int(row[1].split(':')[0]))
    l2.append(l1)
    l1=[]
f.close()
print l2

X = np.array([[1,3], [2,6], [3,8], [4,22], [5,33], [6,6], [7,9], [8,4], [9,10], [10,1], [11,21], [12,3], [13,22], [14,2], [15,31], [16,7], [17,29], [18,4], [19,5], [20,9], [21,9], [22,8], [23,5], [24,23], [25,40], [26,3], [27,4], [28,10], [29,6], [30,5], [31,25], [32,26], [33,8], [34,10], [35,32], [36,8], [37,22], [38,7], [39,9], [40,4], [41,6], [42,1], [43,3], [44,1], [45,24], [46,1], [47,3], [48,21], [49,3], [50,3]])
#X = np.genfromtxt('/tmp/test.csv', delimiter=',')
#X = np.array(l2)
clusters = 3
kmeans = KMeans(n_clusters=clusters).fit(X)
centroids = kmeans.cluster_centers_
labels = kmeans.labels_

#center_color = [ '#0000FF', '#FF5511']
colors = ['#0000FF', '#FF5511', '#008800']

plt.figure(1)
plt.clf()

#for i in range(clusters):
    #plt.scatter(centroids[i, 0], centroids[i, 1], marker='x', s=100, linewidths=3, color=center_color[i], zorder=10)

for i in range(len(X)):
    plt.scatter(X[i][0], X[i][1], marker='o', color=colors[labels[i]])

plt.xlim(0, 50)
plt.ylim(0, 40)

plt.grid(True)
plt.show()
