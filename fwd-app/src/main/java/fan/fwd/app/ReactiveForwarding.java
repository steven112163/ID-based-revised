/*
 * Copyright 2014-present Open Networking Laboratory
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
package fan.fwd.app;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;

/**
 * Sample reactive forwarding application.
 */
@Component(immediate = true)
@Service(value = ReactiveForwarding.class)
public class ReactiveForwarding {

    private static final int FORWARD_TIMEOUT = 10;
    private static final int FORWARD_PRIORITY = 10;

    private static final int DEFAULT_PRIORITY = 40001;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private  EventuallyConsistentMap<MacAddress, ReactiveForwardMetrics> metrics;

    private ApplicationId appId;


    @Property(name = "flowTimeout", intValue = FORWARD_TIMEOUT,
            label = "Configure Flow Timeout for installed flow rules; " +
                    "default is 10 sec")
    private int flowTimeout = FORWARD_TIMEOUT;

    @Property(name = "flowPriority", intValue = FORWARD_PRIORITY,
            label = "Configure Flow Priority for installed flow rules; " +
                    "default is 10")
    private int flowPriority = FORWARD_PRIORITY;

    @Property(name = "defaultPriority", intValue = DEFAULT_PRIORITY,
            label = "Configure Default Flow Priority for installed flow rules; " +
                    "default is 40001")
    private int defaultPriority = DEFAULT_PRIORITY;


    @Property(name = "recordMetrics", boolValue = false,
            label = "Enable record metrics for reactive forwarding")
    private boolean recordMetrics = false;

    private final TopologyListener topologyListener = new InternalTopologyListener();
    private DeviceListener deviceListener = new InternalDeviceListener();
    private FlowRuleListener flowRuleListener = new InternalFlowRuleListener();

    private HashMap<PortNumber, MacAddress> tempMac;
    private HashMap<PortNumber, Ip4Address> tempIp;
    private HashMap<MacAddress, HashMap<PortNumber, MacAddress>> macMapping = new HashMap<MacAddress, HashMap<PortNumber, MacAddress>>();
    private HashMap<MacAddress, HashMap<PortNumber, Ip4Address>> ipMapping = new HashMap<MacAddress, HashMap<PortNumber, Ip4Address>>();

    private String portal_ip = "192.168.44.200";
    private String portal_mac = "ea:e9:78:fb:fd:2d";

    private String db_ip = "192.168.44.128";

    private Authentication auth;


    @Activate
    public void activate(ComponentContext context) {
        KryoNamespace.Builder metricSerializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(ReactiveForwardMetrics.class)
                .register(MultiValuedTimestamp.class);
        metrics =  storageService.<MacAddress, ReactiveForwardMetrics>eventuallyConsistentMapBuilder()
                .withName("metrics-fwd")
                .withSerializer(metricSerializer)
                .withTimestampProvider((key, metricsData) -> new
                        MultiValuedTimestamp<>(new WallClockTimestamp(), System.nanoTime()))
                .build();

        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("fan.fwd.app");

        packetService.addProcessor(processor, PacketProcessor.director(2));
        topologyService.addListener(topologyListener);
        deviceService.addListener(deviceListener);
        flowRuleService.addListener(flowRuleListener);
        readComponentConfiguration(context);
        requestIntercepts();

        Iterator<Device> it = deviceService.getAvailableDevices().iterator();
        while(it.hasNext()) {
            addDefaultRule(it.next().id());
        }

        log.info("fan.fwd Started", appId.id());
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        withdrawIntercepts();
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        topologyService.removeListener(topologyListener);
        deviceService.removeListener(deviceListener);
        flowRuleService.removeListener(flowRuleListener);
        processor = null;
        log.info("fan.fwd Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
        requestIntercepts();
    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Boolean recordMetricsEnabled =
                Tools.isPropertyEnabled(properties, "recordMetrics");
        if (recordMetricsEnabled == null) {
            log.info("IConfigured. Ignore record metrics  is {} ," +
                    "using current value of {}", recordMetrics);
        } else {
            recordMetrics = recordMetricsEnabled;
            log.info("Configured. record metrics  is {}",
                    recordMetrics ? "enabled" : "disabled");
        }

        flowTimeout = Tools.getIntegerProperty(properties, "flowTimeout", FORWARD_TIMEOUT);
        log.info("Configured. Flow Timeout is configured to {} seconds", flowTimeout);

        flowPriority = Tools.getIntegerProperty(properties, "flowPriority", FORWARD_PRIORITY);
        log.info("Configured. Flow Priority is configured to {}", flowPriority);

        defaultPriority = Tools.getIntegerProperty(properties, "defaultPriority", DEFAULT_PRIORITY);
        log.info("Configured. Default Flow Priority is configured to {}", defaultPriority);
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.

            if (context.isHandled()) {
                return;
            }

            InboundPacket InPkt = context.inPacket();
            Ethernet ethPkt = InPkt.parsed();

            if (ethPkt == null) {
                return;
            }

            MacAddress macAddress = ethPkt.getSourceMAC();
            ReactiveForwardMetrics macMetrics = null;
            macMetrics = createCounter(macAddress);
            inPacket(macMetrics);

            // Bail if this is deemed to be a control packet.
            if (isControlPacket(ethPkt)) {
                droppedPacket(macMetrics);
                return;
            }

            HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
            HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

            // Do not process LLDP MAC address in any way.
            if (dstId.mac().isLldp()) {
                droppedPacket(macMetrics);
                return;
            }

            // Do not process IPv4 multicast packets, let mfwd handle them
            if (ethPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                if (dstId.mac().isMulticast()) {
                    return;
                }

                MacAddress src_mac = ethPkt.getSourceMAC();
                MacAddress dst_mac = ethPkt.getDestinationMAC();

                IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
                Ip4Address src_ip = Ip4Address.valueOf(ipv4Packet.getSourceAddress());
                Ip4Address dst_ip = Ip4Address.valueOf(ipv4Packet.getDestinationAddress());

                byte protocol = ipv4Packet.getProtocol();

                String src_port = "";
                String dst_port = "";

                DeviceId in_sw = InPkt.receivedFrom().deviceId();
                PortNumber in_port = InPkt.receivedFrom().port();

                DeviceId src_access_sw = hostService.getHost(srcId).location().deviceId();
                PortNumber src_access_port = hostService.getHost(srcId).location().port();

                String dst_access_sw = "";
                String dst_access_port = "";

                if(hostService.getHost(dstId) != null) {
                    dst_access_sw = hostService.getHost(dstId).location().deviceId().toString();
                    dst_access_port = hostService.getHost(dstId).location().port().toString();
                }

                if(protocol == IPv4.PROTOCOL_TCP) {
                    TCP tcpPacket = (TCP) ipv4Packet.getPayload();  
                    src_port = PortNumber.portNumber(Integer.toString(tcpPacket.getSourcePort())).toString();   
                    dst_port = PortNumber.portNumber(Integer.toString(tcpPacket.getDestinationPort())).toString();  
                }
                else if(protocol == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipv4Packet.getPayload();
                    src_port = PortNumber.portNumber(Integer.toString(udpPacket.getSourcePort())).toString();   
                    dst_port = PortNumber.portNumber(Integer.toString(udpPacket.getDestinationPort())).toString();              
                } 

                auth = new Authentication(src_mac.toString(), dst_mac.toString(), 
                            src_ip.toString(), dst_ip.toString(), src_port, dst_port, 
                            protocol, src_access_sw.toString(), src_access_port.toString(), 
                            dst_access_sw, dst_access_port, in_sw.toString(), in_port.toString(), 
                            String.valueOf(context.time()));

                String resultAction = auth.accessCheck();

                if(resultAction.equals("Drop")) {
                    droppedPacket(macMetrics);
                    return;
                }
                else if(resultAction.equals("Pass")) {
                    normalPkt(context, macMetrics);
                    return;
                }
                else if(resultAction.equals("PktFromPortal")) {
                    pktFromPortal(context);
                    return;
                }
                else if(resultAction.equals("RedirectToPortal")) {
                    redirectToPortal(context);
                    return;
                }
            }
        }
    }

    private void normalPkt(PacketContext context, ReactiveForwardMetrics macMetrics) {
        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());
        Host dst = hostService.getHost(dstId);

        if (dst == null) {
            flood(context, macMetrics);
            return;
        }

        if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
            if (!context.inPacket().receivedFrom().port().equals(dst.location().port())) {
                installRule(context, dst.location().port(), macMetrics);
            }
            return;
        }

        Path path = calculatePath(context);

        if(path == null) {
            flood(context, macMetrics);
            return;
        }

        installRule(context, path.src().port(), macMetrics);
    }

    private void pktFromPortal(PacketContext context) {
        InboundPacket InPkt = context.inPacket();
        Ethernet ethPkt = InPkt.parsed();

        IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
        TCP tcpPacket = (TCP) ipv4Packet.getPayload();

        MacAddress dst_mac = ethPkt.getDestinationMAC();
        PortNumber dst_port = PortNumber.portNumber(Integer.toString(tcpPacket.getDestinationPort()));
        DeviceId in_sw = InPkt.receivedFrom().deviceId();

        MacAddress old_src_mac = null;
        Ip4Address old_src_ip = null;

        if(macMapping.get(dst_mac) != null && ipMapping.get(dst_mac) != null) {
            old_src_mac = macMapping.get(dst_mac).get(dst_port);
            old_src_ip = ipMapping.get(dst_mac).get(dst_port);
        }

        HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());
        Host dst = hostService.getHost(dstId);

        TrafficTreatment treatment = null;

        if(dst == null)
            treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.FLOOD).build();
        else {
            if (InPkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                if (!context.inPacket().receivedFrom().port().equals(dst.location().port()))
                    treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
            }
            else {
                Path path = calculatePath(context);

                if(path == null)
                    treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.FLOOD).build();
                else
                    treatment = DefaultTrafficTreatment.builder().setOutput(path.src().port()).build();
            }
        }

        if(old_src_mac != null && old_src_ip != null) {
            ipv4Packet.setSourceAddress(old_src_ip.toString());
            tcpPacket.resetChecksum();
            tcpPacket.serialize();
            ipv4Packet.resetChecksum();
            ipv4Packet.serialize();
        }

        OutboundPacket OutPkt = new DefaultOutboundPacket(in_sw, treatment, ByteBuffer.wrap(ethPkt.serialize()));
        packetService.emit(OutPkt);
    }

    private void redirectToPortal(PacketContext context) {
        InboundPacket InPkt = context.inPacket();
        Ethernet ethPkt = InPkt.parsed();

        IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
        TCP tcpPacket = (TCP) ipv4Packet.getPayload();

        MacAddress src_mac = ethPkt.getSourceMAC();
        MacAddress dst_mac = ethPkt.getDestinationMAC();
        Ip4Address dst_ip = Ip4Address.valueOf(ipv4Packet.getDestinationAddress());
        PortNumber src_port = PortNumber.portNumber(Integer.toString(tcpPacket.getSourcePort()));
        DeviceId in_sw = InPkt.receivedFrom().deviceId();

        if(macMapping.get(src_mac) == null) {
            tempMac = new HashMap<>();
            tempMac.put(src_port, dst_mac);
            macMapping.put(src_mac, tempMac);
        }
        else
            macMapping.get(src_mac).put(src_port, dst_mac);

        if(ipMapping.get(src_mac) == null) {
            tempIp = new HashMap<>();
            tempIp.put(src_port, dst_ip);
            ipMapping.put(src_mac, tempIp);
        }
        else
            ipMapping.get(src_mac).put(src_port, dst_ip);

        ethPkt.setDestinationMACAddress(portal_mac);
        ipv4Packet.setDestinationAddress(portal_ip);
        tcpPacket.resetChecksum();
        tcpPacket.serialize();
        ipv4Packet.resetChecksum();
        ipv4Packet.serialize();

        HostId dstId = HostId.hostId(ethPkt.getDestinationMAC()); //Portal MAC
        Host dst = hostService.getHost(dstId);

        TrafficTreatment treatment = null;

        if(dst == null)
            treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.FLOOD).build();
        else {
            if (InPkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                if (!context.inPacket().receivedFrom().port().equals(dst.location().port()))
                    treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
            }
            else {
                Path path = calculatePath(context);

                if(path == null)
                    treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.FLOOD).build();
                else
                    treatment = DefaultTrafficTreatment.builder().setOutput(path.src().port()).build();
            }
        }

        OutboundPacket OutPkt = new DefaultOutboundPacket(in_sw, treatment, ByteBuffer.wrap(ethPkt.serialize()));
        packetService.emit(OutPkt);
    }

    private Path calculatePath(PacketContext context) {
        InboundPacket InPkt = context.inPacket();
        Ethernet ethPkt = InPkt.parsed();

        HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());
        Host dst = hostService.getHost(dstId);

        Set<Path> paths =
                topologyService.getPaths(topologyService.currentTopology(),
                                         InPkt.receivedFrom().deviceId(),
                                         dst.location().deviceId());
        if (paths.isEmpty()) {
            return null;
        }

        Path path = pickForwardPathIfPossible(paths, InPkt.receivedFrom().port());
        if (path == null) {
            log.warn("Don't know where to go from here {} for {} -> {}",
                     InPkt.receivedFrom(), ethPkt.getSourceMAC(), ethPkt.getDestinationMAC());
            return null;
        }
        else
            return path;
    }

    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    // Indicated whether this is an IPv6 multicast packet.
    private boolean isIpv6Multicast(Ethernet eth) {
        return eth.getEtherType() == Ethernet.TYPE_IPV6 && eth.isMulticast();
    }

    // Selects a path from the given set that does not lead back to the
    // specified port if possible.
    private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
        Path lastPath = null;
        for (Path path : paths) {
            lastPath = path;
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return lastPath;
    }

    // Floods the specified packet if permissible.
    private void flood(PacketContext context, ReactiveForwardMetrics macMetrics) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                             context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD, macMetrics);
        } else {
            context.block();
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber, ReactiveForwardMetrics macMetrics) {
        replyPacket(macMetrics);
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    // Install a rule forwarding the packet to the specified port.
    private void installRule(PacketContext context, PortNumber portNumber, ReactiveForwardMetrics macMetrics) {
        //
        // We don't support (yet) buffer IDs in the Flow Service so
        // packet out first.
        //
        Ethernet inPkt = context.inPacket().parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        
        selectorBuilder.matchInPort(context.inPacket().receivedFrom().port())
                .matchEthSrc(inPkt.getSourceMAC())
                .matchEthDst(inPkt.getDestinationMAC());

        if (inPkt.getEtherType() == Ethernet.TYPE_IPV4) {
            IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
            byte ipv4Protocol = ipv4Packet.getProtocol();
            Ip4Prefix matchIp4SrcPrefix =
                    Ip4Prefix.valueOf(ipv4Packet.getSourceAddress(),
                                      Ip4Prefix.MAX_MASK_LENGTH);
            Ip4Prefix matchIp4DstPrefix =
                    Ip4Prefix.valueOf(ipv4Packet.getDestinationAddress(),
                                      Ip4Prefix.MAX_MASK_LENGTH);
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPSrc(matchIp4SrcPrefix)
                    .matchIPDst(matchIp4DstPrefix);

            if (ipv4Protocol == IPv4.PROTOCOL_TCP) {
                TCP tcpPacket = (TCP) ipv4Packet.getPayload();
                selectorBuilder.matchIPProtocol(ipv4Protocol)
                        .matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                        .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
            }
            if (ipv4Protocol == IPv4.PROTOCOL_UDP) {
                UDP udpPacket = (UDP) ipv4Packet.getPayload();
                selectorBuilder.matchIPProtocol(ipv4Protocol)
                        .matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                        .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
            }
            if (ipv4Protocol == IPv4.PROTOCOL_ICMP) {
                ICMP icmpPacket = (ICMP) ipv4Packet.getPayload();
                selectorBuilder.matchIPProtocol(ipv4Protocol)
                        .matchIcmpType(icmpPacket.getIcmpType())
                        .matchIcmpCode(icmpPacket.getIcmpCode());
            }
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
                .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(flowPriority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(flowTimeout)
                .add();

        flowObjectiveService.forward(context.inPacket().receivedFrom().deviceId(),
                                     forwardingObjective);
        forwardPacket(macMetrics);
        
        packetOut(context, portNumber, macMetrics);
    }

    public void addDefaultRule(DeviceId switchId) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.NORMAL).build();

        //DHCP
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_UDP).matchUdpSrc(TpPort.tpPort(67)).matchUdpDst(TpPort.tpPort(68));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_UDP).matchUdpSrc(TpPort.tpPort(68)).matchUdpDst(TpPort.tpPort(67));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);


        //DNS
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_UDP).matchUdpSrc(TpPort.tpPort(53));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchTcpSrc(TpPort.tpPort(53));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_UDP).matchUdpDst(TpPort.tpPort(53));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchTcpDst(TpPort.tpPort(53));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);


        //ARP
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_ARP);
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);


        //MySQL
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(db_ip), Ip4Prefix.MAX_MASK_LENGTH)).matchTcpSrc(TpPort.tpPort(3306));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);

        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(db_ip), Ip4Prefix.MAX_MASK_LENGTH)).matchTcpDst(TpPort.tpPort(3306));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);


        //To portal
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchEthDst(MacAddress.valueOf(portal_mac)).matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(portal_ip), Ip4Prefix.MAX_MASK_LENGTH));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);

        if(!switchId.toString().equalsIgnoreCase("of:0000000000000001")) {
            //From portal
            selectorBuilder = DefaultTrafficSelector.builder();
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchEthSrc(MacAddress.valueOf(portal_mac)).matchTcpSrc(TpPort.tpPort(80));
            installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);

            selectorBuilder = DefaultTrafficSelector.builder();
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchEthSrc(MacAddress.valueOf(portal_mac)).matchTcpSrc(TpPort.tpPort(443));
            installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);
        }

        //From portal port == 3000
        selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_TCP).matchEthSrc(MacAddress.valueOf(portal_mac)).matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(portal_ip), Ip4Prefix.MAX_MASK_LENGTH)).matchTcpSrc(TpPort.tpPort(3000));
        installDefaultRule(selectorBuilder, treatment, switchId, DEFAULT_PRIORITY);
    }

    public void installDefaultRule(TrafficSelector.Builder selectorBuilder, TrafficTreatment treatment, DeviceId switchId, int priority) {
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(priority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent()
                .add();

        flowObjectiveService.forward(switchId, forwardingObjective);
    }


    private class InternalFlowRuleListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            switch(event.type()) {
                case RULE_REMOVED:
                    FlowRule flowRule = event.subject();
                    FlowEntry flowEntry = (FlowEntry)flowRule;

                    DeviceId switchId = flowRule.deviceId();
                    TrafficSelector selector = flowRule.selector();
                    Long bytes = flowEntry.bytes();

                    String in_port = ((PortCriterion)selector.getCriterion(Criterion.Type.IN_PORT)).port().toString();
                    String src_mac = ((EthCriterion)selector.getCriterion(Criterion.Type.ETH_SRC)).mac().toString();
                    String dst_mac = ((EthCriterion)selector.getCriterion(Criterion.Type.ETH_DST)).mac().toString();
                    String src_ip = ((IPCriterion)selector.getCriterion(Criterion.Type.IPV4_SRC)).ip().address().toString();
                    String dst_ip = ((IPCriterion)selector.getCriterion(Criterion.Type.IPV4_DST)).ip().address().toString();
                    String src_port = "";
                    String dst_port = "";
                    short protocol = ((IPProtocolCriterion)selector.getCriterion(Criterion.Type.IP_PROTO)).protocol();
                    if(protocol == IPv4.PROTOCOL_TCP) {
                        src_port = ((TcpPortCriterion)selector.getCriterion(Criterion.Type.TCP_SRC)).tcpPort().toString();
                        dst_port = ((TcpPortCriterion)selector.getCriterion(Criterion.Type.TCP_DST)).tcpPort().toString();
                    }
                    else if(protocol == IPv4.PROTOCOL_UDP) {
                        src_port = ((UdpPortCriterion)selector.getCriterion(Criterion.Type.UDP_SRC)).udpPort().toString();
                        dst_port = ((UdpPortCriterion)selector.getCriterion(Criterion.Type.UDP_DST)).udpPort().toString();
                    }
                    
                    log.info("updateBytes");
                    log.info("switchId: {}", switchId);
                    log.info("src_mac: {}", src_mac);
                    log.info("src_ip: {}", src_ip);
                    log.info("src_port: {}", src_port);
                    log.info("protocol: {}", protocol);
                    log.info("in_port: {}", in_port);
                    log.info("bytes: {}", bytes);

                    auth.updateBytes(switchId.toString(), in_port, src_mac, dst_mac, src_ip, dst_ip, src_port, dst_port, protocol, bytes);
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            //log.info("DeviceEvent: {}", event.type());
            //log.info("Subject: {}", event.subject());
            
            DeviceId switchId = event.subject().id();

            switch(event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if(deviceService.isAvailable(switchId))
                        addDefaultRule(switchId);
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            List<Event> reasons = event.reasons();
            if (reasons != null) {
                reasons.forEach(re -> {
                    if (re instanceof LinkEvent) {
                        LinkEvent le = (LinkEvent) re;
                        if (le.type() == LinkEvent.Type.LINK_REMOVED) {
                            fixBlackhole(le.subject().src());
                        }
                    }
                });
            }
        }
    }

    private void fixBlackhole(ConnectPoint egress) {
        Set<FlowEntry> rules = getFlowRulesFrom(egress);
        Set<SrcDstPair> pairs = findSrcDstPairs(rules);

        Map<DeviceId, Set<Path>> srcPaths = new HashMap<>();

        for (SrcDstPair sd : pairs) {
            // get the edge deviceID for the src host
            Host srcHost = hostService.getHost(HostId.hostId(sd.src));
            Host dstHost = hostService.getHost(HostId.hostId(sd.dst));
            if (srcHost != null && dstHost != null) {
                DeviceId srcId = srcHost.location().deviceId();
                DeviceId dstId = dstHost.location().deviceId();
                log.trace("SRC ID is {}, DST ID is {}", srcId, dstId);

                cleanFlowRules(sd, egress.deviceId());

                Set<Path> shortestPaths = srcPaths.get(srcId);
                if (shortestPaths == null) {
                    shortestPaths = topologyService.getPaths(topologyService.currentTopology(),
                            egress.deviceId(), srcId);
                    srcPaths.put(srcId, shortestPaths);
                }
                backTrackBadNodes(shortestPaths, dstId, sd);
            }
        }
    }

    // Backtracks from link down event to remove flows that lead to blackhole
    private void backTrackBadNodes(Set<Path> shortestPaths, DeviceId dstId, SrcDstPair sd) {
        for (Path p : shortestPaths) {
            List<Link> pathLinks = p.links();
            for (int i = 0; i < pathLinks.size(); i = i + 1) {
                Link curLink = pathLinks.get(i);
                DeviceId curDevice = curLink.src().deviceId();

                // skipping the first link because this link's src has already been pruned beforehand
                if (i != 0) {
                    cleanFlowRules(sd, curDevice);
                }

                Set<Path> pathsFromCurDevice =
                        topologyService.getPaths(topologyService.currentTopology(),
                                                 curDevice, dstId);
                if (pickForwardPathIfPossible(pathsFromCurDevice, curLink.src().port()) != null) {
                    break;
                } else {
                    if (i + 1 == pathLinks.size()) {
                        cleanFlowRules(sd, curLink.dst().deviceId());
                    }
                }
            }
        }
    }

    // Removes flow rules off specified device with specific SrcDstPair
    private void cleanFlowRules(SrcDstPair pair, DeviceId id) {
        log.trace("Searching for flow rules to remove from: {}", id);
        log.trace("Removing flows w/ SRC={}, DST={}", pair.src, pair.dst);
        for (FlowEntry r : flowRuleService.getFlowEntries(id)) {
            boolean matchesSrc = false, matchesDst = false;
            for (Instruction i : r.treatment().allInstructions()) {
                if (i.type() == Instruction.Type.OUTPUT) {
                    // if the flow has matching src and dst
                    for (Criterion cr : r.selector().criteria()) {
                        if (cr.type() == Criterion.Type.ETH_DST) {
                            if (((EthCriterion) cr).mac().equals(pair.dst)) {
                                matchesDst = true;
                            }
                        } else if (cr.type() == Criterion.Type.ETH_SRC) {
                            if (((EthCriterion) cr).mac().equals(pair.src)) {
                                matchesSrc = true;
                            }
                        }
                    }
                }
            }
            if (matchesDst && matchesSrc) {
                log.trace("Removed flow rule from device: {}", id);
                flowRuleService.removeFlowRules((FlowRule) r);
            }
        }

    }

    // Returns a set of src/dst MAC pairs extracted from the specified set of flow entries
    private Set<SrcDstPair> findSrcDstPairs(Set<FlowEntry> rules) {
        ImmutableSet.Builder<SrcDstPair> builder = ImmutableSet.builder();
        for (FlowEntry r : rules) {
            MacAddress src = null, dst = null;
            for (Criterion cr : r.selector().criteria()) {
                if (cr.type() == Criterion.Type.ETH_DST) {
                    dst = ((EthCriterion) cr).mac();
                } else if (cr.type() == Criterion.Type.ETH_SRC) {
                    src = ((EthCriterion) cr).mac();
                }
            }
            builder.add(new SrcDstPair(src, dst));
        }
        return builder.build();
    }

    private ReactiveForwardMetrics createCounter(MacAddress macAddress) {
        ReactiveForwardMetrics macMetrics = null;
        if (recordMetrics) {
            macMetrics = metrics.compute(macAddress, (key, existingValue) -> {
                if (existingValue == null) {
                    return new ReactiveForwardMetrics(0L, 0L, 0L, 0L, macAddress);
                } else {
                    return existingValue;
                }
            });
        }
        return macMetrics;
    }

    private void  forwardPacket(ReactiveForwardMetrics macmetrics) {
        if (recordMetrics) {
            macmetrics.incrementForwardedPacket();
            metrics.put(macmetrics.getMacAddress(), macmetrics);
        }
    }

    private void inPacket(ReactiveForwardMetrics macmetrics) {
        if (recordMetrics) {
            macmetrics.incrementInPacket();
            metrics.put(macmetrics.getMacAddress(), macmetrics);
        }
    }

    private void replyPacket(ReactiveForwardMetrics macmetrics) {
        if (recordMetrics) {
            macmetrics.incremnetReplyPacket();
            metrics.put(macmetrics.getMacAddress(), macmetrics);
        }
    }

    private void droppedPacket(ReactiveForwardMetrics macmetrics) {
        if (recordMetrics) {
            macmetrics.incrementDroppedPacket();
            metrics.put(macmetrics.getMacAddress(), macmetrics);
        }
    }

    public EventuallyConsistentMap<MacAddress, ReactiveForwardMetrics> getMacAddress() {
        return metrics;
    }

    public void printMetric(MacAddress mac) {
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println(" MACADDRESS \t\t\t\t\t\t Metrics");
        if (mac != null) {
            System.out.println(" " + mac + " \t\t\t " + metrics.get(mac));
        } else {
            for (MacAddress key : metrics.keySet()) {
                System.out.println(" " + key + " \t\t\t " + metrics.get(key));
            }
        }
    }

    private Set<FlowEntry> getFlowRulesFrom(ConnectPoint egress) {
        ImmutableSet.Builder<FlowEntry> builder = ImmutableSet.builder();
        flowRuleService.getFlowEntries(egress.deviceId()).forEach(r -> {
            if (r.appId() == appId.id()) {
                r.treatment().allInstructions().forEach(i -> {
                    if (i.type() == Instruction.Type.OUTPUT) {
                        if (((Instructions.OutputInstruction) i).port().equals(egress.port())) {
                            builder.add(r);
                        }
                    }
                });
            }
        });

        return builder.build();
    }

    // Wrapper class for a source and destination pair of MAC addresses
    private final class SrcDstPair {
        final MacAddress src;
        final MacAddress dst;

        private SrcDstPair(MacAddress src, MacAddress dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SrcDstPair that = (SrcDstPair) o;
            return Objects.equals(src, that.src) &&
                    Objects.equals(dst, that.dst);
        }

        @Override
        public int hashCode() {
            return Objects.hash(src, dst);
        }
    }
}
