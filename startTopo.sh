#!/bin/bash

# Clean pid of dhcp and start topo.py
# History:
# 	2019/3/5 Steven Yuan create and test

cd /run
test -e dhcpd.pid && sudo rm dhcpd.pid
test -e dhclient.pid && sudo rm dhclient.pid

sleep 2s

cd /var/lib/dhcp
sudo chmod -R 777 dhcpd.leases

sleep 2s

cd ~/ID-based-master
sudo python topo.py

exit 0
