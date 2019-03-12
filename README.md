# Captive Portal for ID-based Network

## Files

1. Apache server IP: Add "ServerName 192.168.44.200" in apache2.conf, which is in /etc/apache2, and add "ServerName 192.168.44.200" in default-ssl.conf, which is in /etc/apache2/sites-available.

2. httpd.conf: Replace the content of 000-default.conf, which is in /etc/apache2/sites-available, with that of httpd.conf.

You can find how to setup the environment in step 1 and 2 on (https://www.digitalocean.com/community/tutorials/how-to-install-linux-apache-mysql-php-lamp-stack-on-ubuntu-16-04), (https://www.digitalocean.com/community/tutorials/how-to-create-a-self-signed-ssl-certificate-for-apache-in-ubuntu-16-04), and (https://www.digitalocean.com/community/tutorials/how-to-create-a-ssl-certificate-on-apache-for-ubuntu-14-04).

3. Login directory: Place the whole Login directory in /var/www/html.

4. portal.sql: put portal.sql in your database.

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
