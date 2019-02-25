# Captive Portal for ID-based Network

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

8. Run mininet.
```
$ sudo python topo.py
```
