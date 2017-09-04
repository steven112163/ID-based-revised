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
        //loadAcl();
    }

    @Deactivate
    protected void deactivate() {
        hostService.removeListener(hostListener);
        flowRuleService.removeFlowRulesById(appId);
        log.info("org.iacl Stopped");

        aclToFlow.clear();
        ruleMap.clear();
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
    public boolean addAclRule(AclRule rule) throws Exception {
        if (matchCheck(rule)) {
            return false;
        }


        ruleMap.put(rule.aclId(), rule);
        log.info("ACL rule(id:{}) is added.", rule.aclId());

        //if (rule.action() != AclRule.Action.ALLOW) {
        enforceRuleAdding(rule);
        //}
        return true;
    }

    private void enforceRuleAdding(AclRule rule) throws Exception {
        Map<MacAddress, DeviceId> dpidSet = new HashMap<>();

        URL url = null;

        try {
            if(rule.srcAttr().equalsIgnoreCase("User"))
                url = new URL(accessDbUrl + "/userToMac?user_id=" + rule.srcId());	
            else if(rule.srcAttr().equalsIgnoreCase("Group"))
                url = new URL(accessDbUrl + "/groupToMac?group_id=" + rule.srcId());
        }
        catch(Exception e){
            throw new Exception("enforceRuleAdding exception");
        }

        URLConnection yc = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        String inputLine = in.readLine();
        in.close();

        JSONArray j = new JSONArray(inputLine);
        //String[] mac = new String[j.length()];

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

        insertACL(rule);
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

    public void insertACL(AclRule rule) throws Exception {
        String s_url = null;

        s_url = accessDbUrl + "/insertACL?acl_id=" + rule.aclId() + "&src_attr=" + rule.srcAttr() + 
                "&src_id=" + rule.srcId() + "&ip=" + rule.dstIp() + "&port=" + rule.dstPort() + 
                "&proto_type=" + rule.protocol() + "&permission=" + rule.action() + "&priority=" + rule.priority();
            
        s_url = s_url.replace(" ","%20");
        
        URL url = null;
        try {
            url = new URL(s_url);
        }
        catch(Exception e) {
            throw new Exception("insertACL exception");
        }

        URLConnection yc = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

        in.close();
    }
    
    @Override
    public void removeAclRule(RuleId ruleId) throws Exception {
        log.info("ACL rule(id:{}) is removed.", ruleId);
        enforceRuleRemoving(ruleId);
    }

    private void enforceRuleRemoving(RuleId aclId) throws Exception {
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

    public void removeACL(RuleId aclId) throws Exception {       
        URL url = null;
        try {
            url = new URL(accessDbUrl + "/removeACL?acl_id=" + aclId);	
        }
        catch(Exception e) {
            throw new Exception("removeACL exception");
        }

        URLConnection yc = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

        in.close();
    }
    
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            // if a new host appears and an existing rule denies
            // its traffic, a new ACL flow rule is generated.
            /*
            if (event.type() == HostEvent.Type.HOST_ADDED) {
                DeviceId deviceId = event.subject().location().deviceId();
                if (mastershipService.getLocalRole(deviceId) == MastershipRole.MASTER) {
                    for (AclRule rule : aclStore.getAclRules()) {
                        if (rule.action() != AclRule.Action.ALLOW) {
                            processHostAddedEvent(event, rule);
                        }
                    }
                }
            }
            */
        }
    }
}
