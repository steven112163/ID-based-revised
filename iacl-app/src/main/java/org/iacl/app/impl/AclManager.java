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
package org.iacl.app.impl;

import org.iacl.app.RuleId;
import org.iacl.app.AclRule;
import org.iacl.app.AclService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;

import com.google.common.collect.Collections2;

import org.json.*;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class AclManager implements AclService {

    private static final int DEFAULT_PRIORITY = 10;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ApplicationId appId;
    private final HostListener hostListener = new InternalHostListener();
    private IdGenerator idGenerator;

    private Map<RuleId, AclRule> ruleMap;
    private Set<FlowRule> flowSet;
    private Map<RuleId, Set<FlowRule>> aclToFlow;

    private String accessDbUrl = "http://127.0.0.1:5000";

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.iacl.app");
        hostService.addListener(hostListener);
        idGenerator = coreService.getIdGenerator("acl-ids");
        AclRule.bindIdGenerator(idGenerator);
        log.info("org.iacl Started");
        
        ruleMap = new HashMap<>();
        aclToFlow = new HashMap<>();
        loadAcl();
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        flowRuleService.removeFlowRulesById(appId);
        log.info("org.iacl Stopped");

        aclToFlow.clear();
        ruleMap.clear();
    }

    private void loadAcl() {
        try {
            URL url = new URL(accessDbUrl + "/getAcl");	
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine = in.readLine();
            in.close();

            JSONArray j = new JSONArray(inputLine);

            for(int i=0; i<j.length(); i++) {
                JSONObject k = j.getJSONObject(i);

                AclRule.Builder rule = AclRule.builder();

                String s = k.getString("ACL_ID");
                if (s != null) {
                    rule.aclId(Long.decode(s));
                }

                s = k.getString("Src_attr");
                if (s != null) {
                    rule.srcAttr(s);
                }

                s = k.getString("Src_ID");
                if (s != null) {
                    rule.srcId(s);
                }

                s = k.getString("Dst_IP");
                if (s != null) {
                    rule.dstIp(Ip4Prefix.valueOf(s));
                }

                s = k.getString("Protocol");
                if (s != null) {
                    if ("TCP".equalsIgnoreCase(s)) {
                        rule.protocol(IPv4.PROTOCOL_TCP);
                    } else if ("UDP".equalsIgnoreCase(s)) {
                        rule.protocol(IPv4.PROTOCOL_UDP);
                    } else if ("ICMP".equalsIgnoreCase(s)) {
                        rule.protocol(IPv4.PROTOCOL_ICMP);
                    }
                }

                s = k.getString("Dst_port");
                if (s != null) {
                    rule.dstPort(Short.valueOf(s));
                }

                s = k.getInt("Permission") == 1 ? "Allow" : "Deny";
                if (s != null) {
                    if ("allow".equalsIgnoreCase(s)) {
                        rule.action(AclRule.Action.ALLOW);
                    } else if ("deny".equalsIgnoreCase(s)) {
                        rule.action(AclRule.Action.DENY);
                    }
                }

                int priority = k.getInt("Priority");
                rule.priority(priority);

                AclRule aclRule = rule.build();

                ruleMap.put(aclRule.aclId(), aclRule);
                enforceRuleAdding(aclRule);
            }
        }
        catch(Exception e) {
            return;
        }
    }

    
    @Override
    public List<AclRule> getAclRules() {
        List<AclRule> aclRules = new ArrayList<>();
        for(RuleId aclId : ruleMap.keySet()) {
            aclRules.add(ruleMap.get(aclId));
        }

        return aclRules;
    }

    private boolean matchCheck(AclRule newRule) {
        for (AclRule existingRule : getAclRules()) {
            if (newRule.checkMatch(existingRule))
                return true;
        }

        return false;
    }
    
    @Override
    public void addAclRule(AclRule rule) {
        if (matchCheck(rule)) {
            return;
        }


        ruleMap.put(rule.aclId(), rule);
        log.info("ACL rule(id:{}) is added.", rule.aclId());

        //if (rule.action() != AclRule.Action.ALLOW) {
        enforceRuleAdding(rule);
        insertACL(rule);
        //}
    }

    private void enforceRuleAdding(AclRule rule) {
        Map<MacAddress, DeviceId> dpidSet = new HashMap<>();

        try {
            URL url = null;

            if(rule.srcAttr().equalsIgnoreCase("User"))
                url = new URL(accessDbUrl + "/userToMac?user_id=" + rule.srcId());	
            else if(rule.srcAttr().equalsIgnoreCase("Group"))
                url = new URL(accessDbUrl + "/groupToMac?group_id=" + rule.srcId());

            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            String inputLine = in.readLine();
            in.close();

            JSONArray j = new JSONArray(inputLine);

            for(int i=0; i<j.length(); i++) {
                JSONObject k = j.getJSONObject(i);
                MacAddress mac = MacAddress.valueOf(k.getString("MAC"));

                HostId srcId = HostId.hostId(mac);
                Host src = hostService.getHost(srcId);
                if(src != null) {
                    dpidSet.put(mac, src.location().deviceId());
                }
            }

            flowSet = new HashSet<>();

            for(MacAddress mac : dpidSet.keySet()) {
                generateAclFlow(rule, mac, dpidSet.get(mac));
            }

            aclToFlow.put(rule.aclId(), flowSet);

            //insertACL(rule);
        }
        catch(Exception e){
            log.info("enforceRuleAdding exception: ", e);
            return;
        }   
    }

    private void generateAclFlow(AclRule rule, MacAddress mac, DeviceId deviceId) {
        if (rule == null)
            return;

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        FlowEntry.Builder flowEntry = DefaultFlowEntry.builder();

        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);
        selectorBuilder.matchEthSrc(mac);
        selectorBuilder.matchIPDst(rule.dstIp());

        if(rule.protocol() != 0) {
            selectorBuilder.matchIPProtocol(Integer.valueOf(rule.protocol()).byteValue());
        }
        if(rule.dstPort() != 0) {
            switch (rule.protocol()) {
                case IPv4.PROTOCOL_TCP:
                    selectorBuilder.matchTcpDst(TpPort.tpPort(rule.dstPort()));
                    break;
                case IPv4.PROTOCOL_UDP:
                    selectorBuilder.matchUdpDst(TpPort.tpPort(rule.dstPort()));
                    break;
                default:
                    break;
            }
        }

        if (rule.action() == AclRule.Action.ALLOW) {
            treatment.add(Instructions.createOutput(PortNumber.CONTROLLER));
        }
        else if (rule.action() == AclRule.Action.DENY) {
            treatment.drop();
        }

        flowEntry.forDevice(deviceId);
        flowEntry.withPriority(DEFAULT_PRIORITY + rule.priority());
        flowEntry.withSelector(selectorBuilder.build());
        flowEntry.withTreatment(treatment.build());
        flowEntry.fromApp(appId);
        flowEntry.makePermanent();
        // install flow rule
        flowRuleService.applyFlowRules(flowEntry.build());
        log.debug("ACL flow rule {} is installed in {}.", flowEntry.build(), deviceId);

        flowSet.add(flowEntry.build());
    }

    public void insertACL(AclRule rule) {
        String s_url = null;

        String protocol = null;

        if (rule.protocol() == IPv4.PROTOCOL_TCP) {
            protocol = "TCP";
        } else if (rule.protocol() == IPv4.PROTOCOL_UDP) {
            protocol = "UDP";
        } else if (rule.protocol() == IPv4.PROTOCOL_ICMP) {
            protocol = "ICMP";
        }

        s_url = accessDbUrl + "/insertACL?acl_id=" + rule.aclId() + "&src_attr=" + rule.srcAttr() + 
                "&src_id=" + rule.srcId() + "&ip=" + rule.dstIp() + "&port=" + rule.dstPort() + 
                "&proto_type=" + protocol + "&permission=" + rule.action() + "&priority=" + rule.priority();
            
        s_url = s_url.replace(" ","%20");
        
        URL url = null;
        try {
            url = new URL(s_url);

            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            in.close();
        }
        catch(Exception e) {
            log.info("insertACL exception: ", e);
            return;
        }

        
    }
    
    @Override
    public void removeAclRule(RuleId ruleId) {
        log.info("ACL rule(id:{}) is removed.", ruleId);
        enforceRuleRemoving(ruleId);
    }

    private void enforceRuleRemoving(RuleId aclId) {
        Set<FlowRule> flowSet = aclToFlow.get(aclId);

        if (flowSet != null) {
            for (FlowRule flowRule : flowSet) {
                flowRuleService.removeFlowRules(flowRule);
                log.debug("ACL flow rule {} is removed from {}.", flowRule.toString(), flowRule.deviceId().toString());
            }
        }

        aclToFlow.remove(aclId);
        ruleMap.remove(aclId);

        removeACL(aclId);
    }

    public void removeACL(RuleId aclId) {       
        try {
            URL url = new URL(accessDbUrl + "/removeACL?acl_id=" + aclId);	
            URLConnection yc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

        in.close();
        }
        catch(Exception e) {
            log.info("removeACL exception: ", e);
            return;
        }   
    }

    public void checkAclRule(MacAddress mac, String userId, String groupId) {
        for (AclRule rule : getAclRules()) {
            if((rule.srcAttr().equalsIgnoreCase("User") && rule.srcId().equalsIgnoreCase(userId)) || 
               (rule.srcAttr().equalsIgnoreCase("Group") && rule.srcId().equalsIgnoreCase(groupId))) {
                flowSet = aclToFlow.get(rule.aclId());
                HostId srcId = HostId.hostId(mac);
                Host src = hostService.getHost(srcId);
                if(src != null) {
                    generateAclFlow(rule, mac, src.location().deviceId());
                }
                aclToFlow.put(rule.aclId(), flowSet);
            }
        }
    }
    
    private class InternalHostListener implements HostListener {
        public JSONObject macToUG(MacAddress mac) {
            try {
                URL url = new URL(accessDbUrl + "/macToUG?mac=" + mac.toString());

                URLConnection yc = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

                String inputLine = in.readLine();
                in.close();

                if(!inputLine.equals("empty"))
                    return new JSONObject(inputLine);
            }
            catch(Exception e){
                log.info("macToUG exception: ", e);
            }
      
            return null;
        }

        private void processHostAddedEvent(AclRule rule, DeviceId deviceId, MacAddress mac, String userId, String groupId) {
            if((rule.srcAttr().equalsIgnoreCase("User") && rule.srcId().equalsIgnoreCase(userId)) || 
               (rule.srcAttr().equalsIgnoreCase("Group") && rule.srcId().equalsIgnoreCase(groupId))) {
                flowSet = aclToFlow.get(rule.aclId());
                generateAclFlow(rule, mac, deviceId);
                aclToFlow.put(rule.aclId(), flowSet);
            }
        }

        private void processHostRemovedEvent(DeviceId deviceId, MacAddress mac) {
            log.info("processHostRemovedEvent");
            Iterable<FlowEntry> flowSet = flowRuleService.getFlowEntries(deviceId);
            Iterator<FlowEntry> flowIt = flowSet.iterator();

            while(flowIt.hasNext()) {
                FlowRule flowRule = flowIt.next();
                TrafficSelector selector = flowRule.selector();
                MacAddress srcMac;
                try{
                    srcMac = ((EthCriterion)selector.getCriterion(Criterion.Type.ETH_SRC)).mac();
                }
                catch(NullPointerException e) {
                    log.info("processHostRemovedEvent NullPointerException: ", e);
                    continue;
                }

                if(flowRule.isPermanent() && srcMac.toString().equalsIgnoreCase(mac.toString())) {
                    flowRuleService.removeFlowRules(flowRule);
                }
            }
        }

        private void processHostMovedEvent(DeviceId oldDeviceId, DeviceId newDeviceId, MacAddress mac) {
            Iterable<FlowEntry> flowSet = flowRuleService.getFlowEntries(oldDeviceId);
            FlowEntry.Builder flowEntry = DefaultFlowEntry.builder();

            Iterator<FlowEntry> flowIt = flowSet.iterator();
            while(flowIt.hasNext()) {
                FlowRule flowRule = flowIt.next();
 
                TrafficSelector selector = flowRule.selector();
                MacAddress srcMac;
                try{
                    srcMac = ((EthCriterion)selector.getCriterion(Criterion.Type.ETH_SRC)).mac();
                }
                catch(NullPointerException e) {
                    log.info("processHostMovedEvent NullPointerException: ", e);
                    continue;
                }

                if(flowRule.isPermanent() && srcMac.toString().equalsIgnoreCase(mac.toString())) {
                    flowRuleService.removeFlowRules(flowRule);

                    flowEntry.forDevice(newDeviceId);
                    flowEntry.withPriority(flowRule.priority());
                    flowEntry.withSelector(selector);
                    flowEntry.withTreatment(flowRule.treatment());
                    flowEntry.fromApp(appId);
                    flowEntry.makePermanent();
                    // install flow rule
                    flowRuleService.applyFlowRules(flowEntry.build());

                    for(RuleId aclId : aclToFlow.keySet()) {
                        if(aclToFlow.get(aclId).contains(flowRule)) {
                            aclToFlow.get(aclId).remove(flowRule);
                            aclToFlow.get(aclId).add(flowEntry.build());
                        }
                    }
                }
            }
        }

        @Override
        public void event(HostEvent event) {
            DeviceId deviceId;
            MacAddress mac;

            switch(event.type()) {
                case HOST_ADDED:
                    deviceId = event.subject().location().deviceId();
                    mac = event.subject().mac();

                    JSONObject j = macToUG(mac);
                    String userId = null;
                    String groupId = null;

                    try {
                        userId = j.getString("User_ID");
                        groupId = j.getString("Group_ID");
                    }
                    catch(Exception e){
                        log.info("HOST_ADDED exception: ", e);
                    }

                    for (AclRule rule : getAclRules()) {
                        processHostAddedEvent(rule, deviceId, mac, userId, groupId);
                    }
                    break;
                case HOST_REMOVED:
                    deviceId = event.subject().location().deviceId();
                    mac = event.subject().mac();
                    processHostRemovedEvent(deviceId, mac);
                    break;
                case HOST_MOVED:
                    DeviceId newDeviceId = event.subject().location().deviceId();
                    DeviceId oldDeviceId = event.prevSubject().location().deviceId();
                    mac = event.subject().mac();
                    processHostMovedEvent(oldDeviceId, newDeviceId, mac);
                    break;
                default:
                    break;
            }
        }
    }
}
