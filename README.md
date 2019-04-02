# Captive Portal for ID-based Network

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
