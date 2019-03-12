#!/bin/bash

# Install ID-based three apps and ovsdb, then upload the configuration
# History:
# 	2019/3/5 Steven Yuan create and test

cd ~/ID-based-master/ifwd-app
onos-app localhost install target/ifwd-app-1.10.0.oar
cd ~/ID-based-master/ibwd-app
onos-app localhost install target/ibwd-app-1.10.0.oar
cd ~/ID-based-master/iacl-app
onos-app localhost install target/iacl-app-1.10.0.oar

sleep 2s

sudo ovs-vsctl set-manager tcp:127.0.0.1:6640

sleep 2s

onos localhost app activate org.onosproject.ovsdb
onos localhost app activate org.onosproject.drivers.ovsdb
onos localhost app activate org.ifwd.app
onos localhost app activate org.ibwd.app
onos localhost app activate org.iacl.app

sleep 2s

cd ~/ID-based-master
onos-netcfg localhost net_config.json

echo "*** Finished!!!\n"

sleep 2s

sudo python access_db.py

exit 0
