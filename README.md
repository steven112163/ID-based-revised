# Captive Portal for ID-based Network on mininet

## Files

1. Apache server IP: Add "ServerName 192.168.44.200" in apache2.conf and default-ssl.conf.
```
$ sudo vim /etc/apache2/apache2.conf
in /etc/apache2/apache2.conf
219 # vim: syntax=apache ts=4 sw=4 sts=4 sr noet
220 ServerName 192.168.44.200

$ sudo vim /etc/apache2/sites-available/default-ssl.conf
in /etc/apache2/sites-available/default-ssl.conf
3 ServerAdmin webmaster@localhost
4 ServerName 192.168.44.200
5 DocumentRoot /var/www/html
```

2. Replace the content of 000-default.conf, with that of httpd.conf.
```
$ sudo cp ~/ID-based-master/httpd.conf /etc/apache2/sites-available/httpd.conf
$ sudo cp /etc/apache2/sites-available/000-default.conf /etc/apache2/sites-available/000-default.conf.bak
$ sudo cp /etc/apache2/sites-available/httpd.conf /etc/apache2/sites-available/000-default.conf
```

You can find how to setup the environment in step 1 and 2 on
(https://www.digitalocean.com/community/tutorials/how-to-install-linux-apache-mysql-php-lamp-stack-on-ubuntu-16-04),
(https://www.digitalocean.com/community/tutorials/how-to-create-a-self-signed-ssl-certificate-for-apache-in-ubuntu-16-04),
and (https://www.digitalocean.com/community/tutorials/how-to-create-a-ssl-certificate-on-apache-for-ubuntu-14-04).

3. Copy Login directory to /var/www/html.
```
$ cd ID-based-master
$ sudo cp -r Login /var/www/html/Login
```

4. Create database and tables.
```
mysql> CREATE DATABASE portal CHARACTER SET utf8 COLLATE utf8_general_ci;
$ mysql -uroot -p<password> -Dportal<~/ID-based-master/portal.sql
```

## Usage

1. Start ONOS.
```
$ cd $ONOS_ROOT
$ bazel run onos-local -- clean     # ok clean
```

2. Compile all ID-based applications and install them.
```
$ cd iacl-app/      # take `iacl-app` for example
$ mvn clean install -DskipTests
$ onos-app localhost install target/iacl-app-1.10.0.oar
```

3. Tell the ovsdb server to start listening on port 6640.
```
$ sudo ovs-vsctl set-manager tcp:127.0.0.1:6640
```

4. Activate `ovsdb` app and driver on ONOS.
```
onos> app activate org.onosproject.ovsdb org.onosproject.drivers.ovsdb
```

You can find step 3 and 4 on [ONOS Wiki](https://wiki.onosproject.org/display/ONOS/OVSDB+interaction+and+ONOS+cli+example).

5. Modify default drivers for OVSDB (`$ONOS_ROOT/drivers/ovsdb/src/main/resources/ovsdb-drivers.xml`) to enable QoS and Queue API. Add following line at the default driver.
```
        <behaviour api="org.onosproject.net.behaviour.QosConfigBehaviour"
                   impl="org.onosproject.drivers.ovsdb.OvsdbQosConfig"/>
        <behaviour api="org.onosproject.net.behaviour.QueueConfigBehaviour"
                   impl="org.onosproject.drivers.ovsdb.OvsdbQueueConfig"/>
```

Step 3 to 5 are required by the `ibwd-app` application.

6. Activate all ID-based applications on ONOS.
```
onos> app activate iacl.app ibwd.app ifwd.app
```

7. Configure ONOS.
```
$ onos-netcfg localhost net_config.json
```

8. Run access_db.
```
$ sudo python access_db.py
```

9. Run mininet.
```
$ sudo python topo.py
```



# Captive Portal for ID-based Network on multiple VMs and physical switch
There are five virtaul machines and one switch.
1. ID-based-web         (ct) IP = 192.168.44.101
2. ID-based-portal      (ct) IP = 192.168.44.200
3. ID-based-dhcp        (ct) IP = 192.168.44.201
4. ID-based-controller  (vm) IP = 192.168.20.xxx vlan 20 (for controll plane) IP = 192.168.44.128 (for data plane)
5. ID-based-host        (vm) IP = dynamic IP
6. Switch               (switch) IP = 192.168.20.203 ID = "of:000078321bdf7000"

## ID-based-web

1. Run simple web server in background on port 80
```
$ python -m SimpleHTTPServer 80 &
```

## ID-based-portal

1. Enable SSL on the apache server.
```
$ vim /etc/apache2/sites-available/default-ssl.conf
in /etc/apache2/sites-available/default-ssl.conf
3 ServerAdim webmaster@localhost
4 ServerName 192.168.44.200
5 DocumentRoot /var/www/html
            .
            .
            .
25 SSLEngine on
26 
27 SSLCertificateFile /etc/ssl/certs/apache-selfsigned.crt
28 SSLCertificateKeyFile /etc/ssl/private/apache-selfsigned.key
            .
            .
            .
105 BrowserMatch "MSIE [2-6]"\
106     nokeepalive ssl-unclean-shotdown\
107     downgrade-1.0 force-response-1.0
```

You can find step 1 on (https://www.digitalocean.com/community/tutorials/how-to-create-a-self-signed-ssl-certificate-for-apache-in-ubuntu-16-04).

2. Copy httpd.conf to replace 000-default.conf
```
$ cp /etc/apache2/sites-available/000-default.conf /etc/apache2/sites-available/000-default.conf.bak
$ cp /etc/apache2/sites-available/httpd.conf /etc/apache2/sites-available/000-default.conf
```

3. Make apache server listen on port 3000
```
$ vim /etc/apache2/ports.conf
in /etc/apache2/ports.conf
5 Listen 80
6 Listen 3000
```

4. Open port 3000 on firewall
```
$ ufw allow 3000/tcp
```

5. Restart apache server
```
$ systemctl restart apache2
```

## ID-based-dhcp

1. Create a subnet for DHCP server.
```
$ vim /etc/dhcp/dhcpd.conf
in /etc/dhcp/dhcpd.conf
63 subnet 192.168.44.0 netmask 255.255.255.0 {
64      range 192.168.44.160 192.168.44.180;
65      option domain-name-servers 8.8.8.8;
66      option subnet-mask 255.255.255.0;
67      option broadcast-address 192.168.44.255;
68      default-lease-time 20;
69      max-lease-time 60;
70 }
```

2. Configure which interface it should serve.
```
$ vim /etc/default/isc-dhcp-server
in /etc/default/isc-dhcp-server
21 INTERFACES="eth0"
```

3. Restart DHCP server.
```
$ systemctl restart isc-dhcp-server
```

## ID-based-controller

1. Grant remote access privilege to 'root' for mysql.
```
mysql> GRANT ALL PRIVILEGES ON *.* TO 'root'@'192.168.44.200' IDENTIFIED BY 'root' WITH GRANT OPTION;
mysql> flush privileges;
```

2. Create database and tables.
```
mysql> CREATE DATABASE portal CHARACTER SET utf8 COLLATE utf8_general_ci;

mysql> USE portal;

mysql> CREATE TABLE IF NOT EXISTS `Access_control` (
     >   `ACL_ID` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
     >   `Src_attr` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Src_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Dst_IP` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   `Dst_port` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Protocol` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Permission` tinyint(1) NOT NULL,
     >   `Priority` int(11) NOT NULL,
     >   PRIMARY KEY (`ACL_ID`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
     
mysql> CREATE TABLE IF NOT EXISTS `Area_flow` (
     >   `ID` int(11) NOT NULL,
     >   `Week` varchar(3) COLLATE utf8_unicode_ci NOT NULL,
     >   `Time_period` int(11) NOT NULL,
     >   `Building` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Kbps` double NOT NULL,
     >   `Percentage` double NOT NULL,
     >   PRIMARY KEY (`ID`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
     
mysql> CREATE TABLE IF NOT EXISTS `Flow_classification` (
     >   `ID` int(11) NOT NULL,
     >   `User_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Week` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Time_period` int(11) NOT NULL,
     >   `Building` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Room` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Kbps` double NOT NULL,
     >   `Day_counts` int(11) NOT NULL,
     >   `Bwd_req` varchar(5) COLLATE utf8_unicode_ci NOT NULL,
     >   PRIMARY KEY (`ID`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
     
mysql> CREATE TABLE IF NOT EXISTS `Group` (
     >   `Group_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Name` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   PRIMARY KEY (`Group_ID`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

mysql> CREATE TABLE IF NOT EXISTS `IP_MAC` (
     >   `IP` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   `MAC` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   `Time` datetime NOT NULL,
     >   PRIMARY KEY (`IP`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
     
mysql> CREATE TABLE IF NOT EXISTS `Registered_MAC` (
     >   `MAC` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   `User_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Group_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Enable` tinyint(1) NOT NULL,
     >   PRIMARY KEY (`MAC`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

mysql> CREATE TABLE IF NOT EXISTS `Switch` (
     >   `Switch_ID` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   `Building` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Room` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   PRIMARY KEY (`Switch_ID`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

mysql> CREATE TABLE IF NOT EXISTS `User` (
     >   `User_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Name` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   `Group_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
     >   `Account` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   `Password` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
     >   PRIMARY KEY (`User_ID`)
     > ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
```

3. Insert values into tables.
```
mysql> INSERT INTO `Group` (`Group_ID`, `Name`) VALUES
     >   ('Guest', 'Guest'),
     >   ('Staff', 'Staff'),
     >   ('Studnet', 'Student'),
     >   ('Teacher', 'Teacher');
     
mysql> INSERT INTO `Switch` (`Switch_ID`, `Building`, `Room`) VALUES
     >   ('of:000078321bdf7000', 'Building1', 'Room1');
     
mysql> INSERT INTO `User` (`User_ID`, `Name`, `Group_ID`, `Account`, `Password`) VALUES
     >   ('A', 'A', 'Teacher', '1', '1'),
     >   ('B', 'B', 'Teacher', '2', '2'),
     >   ('C', 'C', 'Staff', '3', '3'),
     >   ('D', 'D', 'Student', '4', '4'),
     >   ('E', 'E', 'Student', '5', '5'),
     >   ('F', 'F', 'Guest', '6', '6');

mysql> INSERT INTO `Registered_MAC` (`MAC`, `User_ID`, `Group_ID`, `Enable`) VALUES
     >   ('EA:E9:78:FB:FD:00', '', '', 1)
```

4. Modify default driver in ovsdb.
```
$ sudo vim $ONOS_ROOT/drivers/ovsdb/src/main/resources/ovsdb-drivers.xml
in $ONOS_ROOT/drivers/ovsdb/src/main/resources/ovsdb-drivers.xml
26      <behaviour api="org.onosproject.net.behaviour.QosConfigBehaviour"
27                 impl="org.onosproject.drivers.ovsdb.OvsdbQosConfig"/>
28      <behaviour api="org.onosproject.net.behaviour.QueueConfigBehaviour"
29                 impl="org.onosproject.drivers.ovsdb.OvsdbQueueConfig"/>
```

5. Run onos.
```
$ cd $ONOS_ROOT
$ bazel run onos-local -- clean debug  # ok clean debug
```

6. Tell the ovsdb to start listening on port 6640.
```
$ sudo ovs-vsctl set-manager tcp:127.0.0.1:6640
```

7. Build ifwd, ibwd, iacl and install them
```
$ cd ~/ID-based-master/ifwd
$ maven clean install -DskipTests  # mci -DskipTests
$ onos-app localhost install target/ifwd-app-1.10.0.oar
$ cd ~/ID-based-master/ibwd
$ maven clean install -DskipTests  # mci -DskipTests
$ onos-app localhost install target/ibwd-app-1.10.0.oar
$ cd ~/ID-based-master/iacl
$ maven clean install -DskipTests  # mci -DskipTests
$ onos-app localhost install target/iacl-app-1.10.0.oar
```

8. Activate three apps and ovsdb
```
$ onos localhost app activate org.onosproject.ovsdb org.onosproject.drivers.ovsdb org.ifwd.app org.ibwd.app org.iacl.app
```

9. Upload configuration JSON file
```
$ onos-netcfg localhost new_net_config.json
```

10. Start accessdb api
```
$ sudo python ID-based-master/accessdb.py
```

## ID-based-host

1. Use browser connect to http://192.168.44.101
2. It will be redirected to portal(http://192.168.44.200:3000)
3. Enter username and password(ex: 1, 1)
4. If authentication is successful, then you can access http://192.168.44.101
