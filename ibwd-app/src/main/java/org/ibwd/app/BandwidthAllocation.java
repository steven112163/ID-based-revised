/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ibwd.app;

import org.ibwd.app.config.ConfigService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.HostId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.QueueDescription;
import org.onosproject.net.behaviour.QueueConfigBehaviour;
import org.onosproject.net.behaviour.QueueInfo;
import org.onosproject.net.behaviour.DefaultQueueDescription;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.net.behaviour.QosId;
import org.onosproject.net.behaviour.DefaultQosDescription;
import org.onosproject.net.behaviour.QosConfigBehaviour;
import org.onosproject.net.behaviour.PortConfigBehaviour;
import org.onosproject.net.behaviour.QosDescription;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IpAddress;
import org.onlab.util.Bandwidth;

import java.util.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class BandwidthAllocation implements BandwidthAllocationService{

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    private ApplicationId appId;

    DriverHandler driverHandler;
    QueueConfigBehaviour queueConfig;
    QosConfigBehaviour qosConfig;
    PortConfigBehaviour portConfig;

    Calendar calendar = Calendar.getInstance();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private InternalNetworkConfigListener configListener = new InternalNetworkConfigListener();
    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    
    private HashMap<DeviceId, String> swType;

    private static final String CORE_SW = "coreSw";
    private static final String AGGRE_SW = "aggreSw";
    private static final String ACCESS_SW = "accessSw";
    private static final String SERVER_SW = "serverSw";

    private String accessDbUrl = "http://127.0.0.1:5000";

    private HashMap<String, Integer> userPriority;
    private HashMap<String, Integer> buildingPercentage;
    private HashMap<String, Long> buildingQueue;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("fan.band.app");
        networkConfigService.addListener(configListener);
        deviceService.addListener(deviceListener);
        log.info("band Started");
        setupConfiguration();
        setQueue();
        timer();
    }

    @Deactivate
    protected void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
        networkConfigService.removeListener(configListener);
        deviceService.removeListener(deviceListener);
        log.info("band Stopped");

        Collection<QueueDescription> queues = queueConfig.getQueues();
        Iterator<QueueDescription> it = queues.iterator();
        while(it.hasNext()) {
            QueueId queueId = it.next().queueId();
            queueConfig.deleteQueue(queueId);
        }

        Collection<QosDescription> qos = qosConfig.getQoses();
        Iterator<QosDescription> it_ = qos.iterator();
        while(it_.hasNext()) {
            QosId qosId = it_.next().qosId();
            qosConfig.deleteQoS(qosId);
        }
    }

    public Long getQueue(DeviceId swId, PortNumber outPort, MacAddress srcMac, MacAddress dstMac) {
        String type = swType.get(swId);
        log.info("switch {} type: {}", swId, type);

        if(type == null)
            return Long.valueOf(-1);

        ConnectPoint cp = new ConnectPoint(swId, outPort);
        Set<Link> linkSet = linkService.getEgressLinks(cp);
        Iterator<Link> linkIt = linkSet.iterator();

        //int currentHour = new Date().getTime();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        switch (type) {
            case CORE_SW:
                if(linkIt.hasNext()) {
                    Link link = linkIt.next();
                    DeviceId dstSwId = link.dst().deviceId();

                    if(swType.get(dstSwId).equalsIgnoreCase(AGGRE_SW)) { // Dst Room
                        return roomToQueue(dstMac);
                    }
                    else if(swType.get(dstSwId).equalsIgnoreCase(SERVER_SW)) { // Src Building
                        return buildingToQueue(srcMac);
                    }
                }
                break;
            case AGGRE_SW:
                if(linkIt.hasNext()) {
                    Link link = linkIt.next();
                    DeviceId dstSwId = link.dst().deviceId();

                    if(swType.get(dstSwId).equalsIgnoreCase(ACCESS_SW)) { // Dst User
                        return userToQueue(dstMac, currentHour);
                    }
                    else if(swType.get(dstSwId).equalsIgnoreCase(CORE_SW)) { //Src Room
                        return roomToQueue(srcMac);
                    }
                }
                break;
            case ACCESS_SW:
                log.info("ACCESS_SW");
                log.info("switch: {}", swId);
                if(linkIt.hasNext()) {
                    Link link = linkIt.next();
                    DeviceId dstSwId = link.dst().deviceId();

                    if(swType.get(dstSwId).equalsIgnoreCase(AGGRE_SW)) { // Src User
                        log.info("AGGRE_SW");
                        log.info("dstSwId: {}", dstSwId);
                        return userToQueue(srcMac, currentHour);
                    }
                }
                break;
            case SERVER_SW:
                if(linkIt.hasNext()) {
                    Link link = linkIt.next();
                    DeviceId dstSwId = link.dst().deviceId();

                    if(swType.get(dstSwId).equalsIgnoreCase(CORE_SW)) { // Dst Building
                        return buildingToQueue(dstMac);
                    }
                }
                break;
            default:
                break;
        }

        return Long.valueOf(-1);
    }

    private Long userToQueue(MacAddress mac, int currentHour) {
        try {
            URL url = new URL(accessDbUrl + "/macToUser?mac=" + mac);	
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();

            String userId = null;

            if(!inputLine.equals("empty")) {
                JSONObject j = new JSONObject(inputLine);
                userId = j.getString("User_ID");
            }
            else {
                return Long.valueOf(-1);
            }

            log.info("userId: {}", userId);

            HostId hostId = HostId.hostId(mac);
            DeviceId hostSw = hostService.getHost(hostId).location().deviceId();  

            url = new URL(accessDbUrl + "/swToLocation?sw=" + hostSw.toString());	
            yc = url.openConnection();
            in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            inputLine = in.readLine();
            in.close();

            String building = null;
            String room = null;

            if(!inputLine.equals("empty")) {
                JSONObject j = new JSONObject(inputLine);
                building = j.getString("Building");
                room = j.getString("Room");
            }
            else if(inputLine.equals("empty"))
                return Long.valueOf(-1);

            log.info("building: {}", building);
            log.info("room: {}", room);
            log.info("currentHour: {}", currentHour);

            url = new URL(accessDbUrl + "/userToPriority?user=" + userId
                + "&room=" + room + "&building=" + building  
                + "&time_interval=" + currentHour);	
            yc = url.openConnection();
            in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            inputLine = in.readLine();
            in.close();

            int priority = -1;

            if(!inputLine.equals("empty")) {
                JSONObject j = new JSONObject(inputLine);
                priority = j.getInt("Priority");
            }
            else if(inputLine.equals("empty"))
                return Long.valueOf(-1);

            log.info("priority: {}", priority);

            if(priority == 1)
                return Long.valueOf(1);
            else if(priority == 2)
                return Long.valueOf(2);
            else if(priority == 3)
                return Long.valueOf(3);
            else
                return Long.valueOf(-1);
        }
        catch (Exception e) {
            log.info("userToQueue exception: ", e);			
        }

        return Long.valueOf(-1); 
    }

    private Long roomToQueue(MacAddress mac) {
        try {
            HostId hostId = HostId.hostId(mac);
            DeviceId hostSw = hostService.getHost(hostId).location().deviceId();  

            URL url = new URL(accessDbUrl + "/swToLocation?sw=" + hostSw.toString());	
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();

            String building = null;
            String room = null;

            if(!inputLine.equals("empty")) {
                JSONObject j = new JSONObject(inputLine);
                building = j.getString("Building");
                room = j.getString("Room");
            }
            else if(inputLine.equals("empty"))
                return Long.valueOf(-1);


            if(room == null)
                return Long.valueOf(-1);
            else if(room.equals("Room1"))
                return Long.valueOf(1);
            else if(room.equals("Room2"))
                return Long.valueOf(2);      
        }
        catch (Exception e) {
            log.info("roomToQueue exception: ", e);			
        }

        return Long.valueOf(-1); 
    }

    private Long buildingToQueue(MacAddress mac) {
        try {
            HostId hostId = HostId.hostId(mac);
            DeviceId hostSw = hostService.getHost(hostId).location().deviceId();  

            URL url = new URL(accessDbUrl + "/swToLocation?sw=" + hostSw.toString());	
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();

            String building = null;
            String room = null;

            if(!inputLine.equals("empty")) {
                JSONObject j = new JSONObject(inputLine);
                building = j.getString("Building");
                room = j.getString("Room");
            }
            else if(inputLine.equals("empty"))
                return Long.valueOf(-1);

            if(building == null)
                return Long.valueOf(-1);
            else
                return buildingQueue.get(building);
        }
        catch (Exception e) {
            log.info("buildingToQueue exception: ", e);			
        }

        return Long.valueOf(-1); 
    }

    private void setupConfiguration() {
        swType = new HashMap<>();

        Set<String> coreSw = configService.getCoreSw();
        Iterator<String> it = coreSw.iterator();
        while(it.hasNext()) {
            swType.put(DeviceId.deviceId(it.next()), CORE_SW);
        }

        Set<String> aggreSw = configService.getAggreSw();
        it = aggreSw.iterator();
        while(it.hasNext()) {
            swType.put(DeviceId.deviceId(it.next()), AGGRE_SW);
        }

        Set<String> accessSw = configService.getAccessSw();
        it = accessSw.iterator();
        while(it.hasNext()) {
            swType.put(DeviceId.deviceId(it.next()), ACCESS_SW);
        }

        Set<String> serverSw = configService.getServerSw();
        it = serverSw.iterator();
        while(it.hasNext()) {
            swType.put(DeviceId.deviceId(it.next()), SERVER_SW);
        }
    }

    private void setQueue() {
        driverHandler = driverService.createHandler(DeviceId.deviceId("ovsdb:127.0.0.1"));
        queueConfig = driverHandler.behaviour(QueueConfigBehaviour.class);
        qosConfig = driverHandler.behaviour(QosConfigBehaviour.class);
        portConfig = driverHandler.behaviour(PortConfigBehaviour.class);

        QueueDescription.Builder queueDesc = DefaultQueueDescription.builder();
        QosDescription.Builder qosDesc = DefaultQosDescription.builder();
        
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        userPriority = new HashMap<>();
        buildingPercentage = new HashMap<>();
        buildingQueue = new HashMap<>();

        Iterator<Device> it = deviceService.getAvailableDevices().iterator();
        while(it.hasNext()) {
            DeviceId srcSwId = it.next().id();
            String type = swType.get(srcSwId);
            
            if(type == null)
                continue;

            Set<Link> linkSet = linkService.getDeviceEgressLinks(srcSwId);
            Iterator<Link> linkIt = linkSet.iterator();

            try {
                switch (type) {
                    case CORE_SW:
                        while(linkIt.hasNext()) {
                            Link link = linkIt.next();
                            DeviceId dstSwId = link.dst().deviceId();

                            if(swType.get(dstSwId).equalsIgnoreCase(AGGRE_SW)) {
                                PortNumber srcPort = link.src().port();
                                PortNumber dstPort = link.dst().port();

                                String srcPortName = deviceService.getPort(srcSwId, srcPort).annotations().value("portName");
                                String dstPortName = deviceService.getPort(dstSwId, dstPort).annotations().value("portName");
                                
                                Long bandwidth = Long.valueOf(link.annotations().value("bandwidth"));

                                Map<Long, QueueDescription> queues = new HashMap<>();

                                QueueId queueId = QueueId.queueId(srcPortName+"-queue1");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(Long.valueOf("0")));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(0L, queueDesc.build());

                                queueId = QueueId.queueId(srcPortName+"-queue2");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(Long.valueOf(bandwidth/2)));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(1L, queueDesc.build());

                                queueId = QueueId.queueId(srcPortName+"-queue3");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(Long.valueOf(bandwidth/2)));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(2L, queueDesc.build());

                                QosId qosId = QosId.qosId(srcPortName+"-qos1");
                                qosConfig.deleteQoS(qosId);
                                qosDesc.qosId(qosId)
                                        .type(QosDescription.Type.HTB)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .queues(queues);

                                qosConfig.addQoS(qosDesc.build());

                                PortDescription portDesc = new DefaultPortDescription(
                                    PortNumber.portNumber(srcPort.toLong(), srcPortName), true);
                                portConfig.applyQoS(portDesc, qosDesc.build());

                                portDesc = new DefaultPortDescription(
                                    PortNumber.portNumber(dstPort.toLong(), dstPortName), true);
                                portConfig.applyQoS(portDesc, qosDesc.build());
                            }
                        }
                        break;
                    case AGGRE_SW:
                        while(linkIt.hasNext()) {
                            Link link = linkIt.next();
                            DeviceId dstSwId = link.dst().deviceId();                            

                            if(swType.get(dstSwId).equalsIgnoreCase(ACCESS_SW)) {
                                String building = null;
                                String room = null;

                                URL url = new URL(accessDbUrl + "/swToLocation?sw=" + dstSwId.toString());	
                                URLConnection yc = url.openConnection();
                                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

                                String inputLine = in.readLine();
                                in.close();

                                if(!inputLine.equals("empty")) {
                                    JSONObject j = new JSONObject(inputLine);
                                    building = j.getString("Building");
                                    room = j.getString("Room");
                                }
                                else if(inputLine.equals("empty"))
                                    continue;

                                int userCount_1 = 0;
                                int userCount_2 = 0;
                                int userCount_3 = 0;

                                url = new URL(accessDbUrl + "/flowClassToUserCount?building=" + building 
                                    + "&room=" + room + "&time_interval=" + currentHour);	
                                yc = url.openConnection();
                                in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

                                inputLine = in.readLine();
                                in.close();

                                if(!inputLine.equals("empty")) {
                                    JSONArray ja = new JSONArray(inputLine);

                                    for(int i=0; i<ja.length(); i++) {
                                        JSONObject jo = ja.getJSONObject(i);

                                        String user = jo.getString("User_ID");
                                        int priority = jo.getInt("Priority");
                                        
                                        userPriority.put(user, priority);

                                        if(priority == 1)
                                            userCount_1++;
                                        else if(priority == 2)
                                            userCount_2++;
                                        else if(priority == 3)
                                            userCount_3++;
                                    }
                                }
                                else if(inputLine.equals("empty"))
                                    continue;

                                PortNumber srcPort = link.src().port();
                                PortNumber dstPort = link.dst().port();

                                String srcPortName = deviceService.getPort(srcSwId, srcPort).annotations().value("portName");
                                String dstPortName = deviceService.getPort(dstSwId, dstPort).annotations().value("portName");
                                
                                Long bandwidth = Long.valueOf(link.annotations().value("bandwidth"));

                                Map<Long, QueueDescription> queues = new HashMap<>();

                                QueueId queueId = QueueId.queueId(srcPortName+"-queue1");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(Long.valueOf("0")));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(0L, queueDesc.build());

                                double minRate = bandwidth/(userCount_1 + 2*userCount_2 + 3*userCount_3);
                                double minRate_1 = minRate*userCount_1;
                                double minRate_2 = 2*minRate*userCount_2;
                                double minRate_3 = 3*minRate*userCount_3;

                                queueId = QueueId.queueId(srcPortName+"-queue2");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(minRate_1));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(1L, queueDesc.build());

                                queueId = QueueId.queueId(srcPortName+"-queue3");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(minRate_2));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(2L, queueDesc.build());

                                queueId = QueueId.queueId(srcPortName+"-queue4");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(minRate_3));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(3L, queueDesc.build());

                                QosId qosId = QosId.qosId(srcPortName+"-qos1");
                                qosConfig.deleteQoS(qosId);
                                qosDesc.qosId(qosId)
                                        .type(QosDescription.Type.HTB)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .queues(queues);

                                qosConfig.addQoS(qosDesc.build());

                                PortDescription portDesc = new DefaultPortDescription(
                                    PortNumber.portNumber(srcPort.toLong(), srcPortName), true);
                                portConfig.applyQoS(portDesc, qosDesc.build());

                                portDesc = new DefaultPortDescription(
                                    PortNumber.portNumber(dstPort.toLong(), dstPortName), true);
                                portConfig.applyQoS(portDesc, qosDesc.build());
                            }
                        }
                        break;
                    case SERVER_SW:
                        while(linkIt.hasNext()) {
                            Link link = linkIt.next();
                            DeviceId dstSwId = link.dst().deviceId();

                            if(swType.get(dstSwId).equalsIgnoreCase(CORE_SW)) {
                                URL url = new URL(accessDbUrl + "/buildingToPercent?time_interval=" + currentHour);	
                                URLConnection yc = url.openConnection();
                                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

                                String inputLine = in.readLine();
                                in.close();

                                if(!inputLine.equals("empty")) {
                                    JSONArray ja = new JSONArray(inputLine);

                                    for(int i=0; i<ja.length(); i++) {
                                        JSONObject jo = ja.getJSONObject(i);

                                        String building_ = jo.getString("Building");
                                        int percent = jo.getInt("Percentage");
                                        
                                        buildingPercentage.put(building_, percent);
                                    }
                                }
                                else if(inputLine.equals("empty"))
                                    continue;

                                PortNumber srcPort = link.src().port();
                                PortNumber dstPort = link.dst().port();

                                String srcPortName = deviceService.getPort(srcSwId, srcPort).annotations().value("portName");
                                String dstPortName = deviceService.getPort(dstSwId, dstPort).annotations().value("portName");
                                
                                Long bandwidth = Long.valueOf(link.annotations().value("bandwidth"));

                                Map<Long, QueueDescription> queues = new HashMap<>();

                                QueueId queueId = QueueId.queueId(srcPortName+"-queue1");
                                queueConfig.deleteQueue(queueId);
                                queueDesc.queueId(queueId)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .minRate(Bandwidth.mbps(Long.valueOf("0")));

                                queueConfig.addQueue(queueDesc.build());
                                queues.put(0L, queueDesc.build());

                                int i = 2;
                                for(String b : buildingPercentage.keySet()) {
                                    double minRate = bandwidth*buildingPercentage.get(b)/100;

                                    queueId = QueueId.queueId(srcPortName+"-queue" + i);
                                    queueConfig.deleteQueue(queueId);
                                    queueDesc.queueId(queueId)
                                            .maxRate(Bandwidth.mbps(bandwidth))
                                            .minRate(Bandwidth.mbps(minRate));

                                    queueConfig.addQueue(queueDesc.build());
                                    queues.put(Long.valueOf(i-1), queueDesc.build());
                                    buildingQueue.put(b, Long.valueOf(i-1));

                                    i++;
                                }

                                QosId qosId = QosId.qosId(srcPortName+"-qos1");
                                qosConfig.deleteQoS(qosId);
                                qosDesc.qosId(qosId)
                                        .type(QosDescription.Type.HTB)
                                        .maxRate(Bandwidth.mbps(bandwidth))
                                        .queues(queues);

                                qosConfig.addQoS(qosDesc.build());

                                PortDescription portDesc = new DefaultPortDescription(
                                    PortNumber.portNumber(srcPort.toLong(), srcPortName), true);
                                portConfig.applyQoS(portDesc, qosDesc.build());

                                portDesc = new DefaultPortDescription(
                                    PortNumber.portNumber(dstPort.toLong(), dstPortName), true);
                                portConfig.applyQoS(portDesc, qosDesc.build());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            catch (Exception e) {
                log.info("setQueue exception: ", e);			
            } 
        }
    }

    public void timer() {
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Long currentTime = new Date().getTime();
        long startScheduler = calendar.getTime().getTime() - currentTime;

        scheduledExecutorService.scheduleAtFixedRate(new MyJob(), startScheduler, 3600000, TimeUnit.MILLISECONDS);
        
        calendar.setTime(new Date());
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            //log.info("DeviceEvent: {}", event);
            switch(event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:          
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    setQueue();
                    timer();
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == ConfigService.CONFIG_CLASS) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED:
                    case CONFIG_REMOVED:
                        setupConfiguration();
                        setQueue();
                        timer();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public class MyJob implements Runnable{
        @Override
            public void run() {  
                log.info("MyJob!!!!");      
                setQueue();
            }
    }
}
