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
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
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
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IpAddress;
import org.onlab.util.Bandwidth;

import java.util.*;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class BandwidthF {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("fan.band.app");
        log.info("band Started");
        test();
    }

    @Deactivate
    protected void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
        log.info("band Stopped");
    }

    public void test() {
        /*
        //log.info("hi1");
        DriverHandler h = driverService.createHandler(DeviceId.deviceId("ovsdb:127.0.0.1"));
        //log.info("hi2");
        QueueConfigBehaviour queueConfig = h.behaviour(QueueConfigBehaviour.class);
        QosConfigBehaviour qosConfig = h.behaviour(QosConfigBehaviour.class);
        PortConfigBehaviour portConfig = h.behaviour(PortConfigBehaviour.class);
        BridgeConfig bridgeConfig = h.behaviour(BridgeConfig.class);
        //log.info("hi3");

        QueueDescription.Builder qd = DefaultQueueDescription.builder();
        QueueId queueId = QueueId.queueId("3");

        //Set<QueueDescription.Type> queueSet = EnumSet.of(QueueDescription.Type.MAX, QueueDescription.Type.MIN);
        qd.queueId(queueId).maxRate(Bandwidth.bps(Long.parseLong("40000"))).minRate(Bandwidth.bps(Long.valueOf("20000")));//.type(queueSet).;
        queueConfig.addQueue(qd.build());

        //log.info("hi4");

        Map<Long, QueueDescription> queues = new HashMap<>();
        queues.put(0L, qd.build());

        QosDescription qosDesc = DefaultQosDescription.builder()
                .qosId(QosId.qosId(UUID.randomUUID().toString()))
                .type(QosDescription.Type.HTB)
                .maxRate(Bandwidth.bps(Long.valueOf("40000")))
                .queues(queues)
                .build();

        qosConfig.addQoS(qosDesc);

        PortDescription portDesc = new DefaultPortDescription(
                PortNumber.portNumber(Long.valueOf(4), "s1-eth2"), true);

        portConfig.applyQoS(portDesc, qosDesc);

        log.info("Queue Info. {}", queueConfig.getQueue(qd.build()));
        //queueConfig.getQueue(qd.build());
        log.info("Bridge Info. {}", bridgeConfig.getPorts());
        */
        /*
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setQueue(Long.valueOf(1))
                .setOutput(PortNumber.portNumber(1))
                .build();

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthSrc(MacAddress.valueOf("ea:e9:78:fb:fd:cc"));

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(11)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent()
                .add();

        flowObjectiveService.forward(DeviceId.deviceId("of:0000000000000005"), forwardingObjective);
        */

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(MacAddress.valueOf("ea:e9:78:fb:fd:dd"))
                .setIpDst(IpAddress.valueOf("192.168.44.104"))
                .setOutput(PortNumber.portNumber(3))
                .build();

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthSrc(MacAddress.valueOf("ea:e9:78:fb:fd:cc")).matchEthType(Ethernet.TYPE_IPV4);

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(12)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent()
                .add();

        flowObjectiveService.forward(DeviceId.deviceId("of:0000000000000005"), forwardingObjective);
    }
}
