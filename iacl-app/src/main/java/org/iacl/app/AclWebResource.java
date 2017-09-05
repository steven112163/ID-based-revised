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
package org.iacl.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.onlab.packet.MacAddress;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Prefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.onlab.util.Tools.nullIsNotFound;

import java.util.*;

/**
 * Sample web resource.
 */
@Path("acl")
public class AclWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
    @GET
    public Response queryAclRule() {
        List<AclRule> rules = get(AclService.class).getAclRules();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (AclRule rule : rules) {
            ObjectNode node = mapper.createObjectNode();
            node.put("aclId", rule.aclId().toString());

            if (rule.srcAttr() != null) {
                node.put("srcAttr", rule.srcAttr().toString());
            }
            if (rule.srcId() != null) {
                node.put("srcId", rule.srcId().toString());
            }
            if (rule.dstIp() != null) {
                node.put("dstIp", rule.dstIp().toString());
            }
            if (rule.protocol() != 0) {
                switch (rule.protocol()) {
                    case IPv4.PROTOCOL_ICMP:
                        node.put("protocol", "ICMP");
                        break;
                    case IPv4.PROTOCOL_TCP:
                        node.put("protocol", "TCP");
                        break;
                    case IPv4.PROTOCOL_UDP:
                        node.put("protocol", "UDP");
                        break;
                    default:
                        break;
                }
            }
            if (rule.dstPort() != 0) {
                node.put("dstPort", rule.dstPort());
            }
            if (rule.priority() >= 0 && rule.priority() <= 9) {
                node.put("priority", rule.priority());
            }
            node.put("action", rule.action().toString());
            arrayNode.add(node);
        }

        root.set("aclRules", arrayNode);
        return Response.ok(root.toString(), MediaType.APPLICATION_JSON_TYPE).build();
        //ObjectNode node = mapper().createObjectNode().put("hello", "world");
        //return ok(node).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAclRule(InputStream stream) throws URISyntaxException {
        AclRule newRule = jsonToRule(stream);
        get(AclService.class).addAclRule(newRule);
        return Response.created(new URI(newRule.aclId().toString())).build();
        /*
        return get(AclService.class).addAclRule(newRule) ?
                Response.created(new URI(newRule.aclId().toString())).build() :
                Response.serverError().build();
        */
    }

    @DELETE
    @Path("{aclId}")
    public Response removeAclRule(@PathParam("aclId") String aclId) {
        RuleId ruleId = new RuleId(Long.parseLong(aclId.substring(2), 16));
        get(AclService.class).removeAclRule(ruleId);
        return Response.noContent().build();
    }

    @POST
    @Path("authSuccess")
    @Consumes(MediaType.APPLICATION_JSON)
     public Response userAuthSuccess(InputStream stream) throws URISyntaxException {
        log.info("userAuthSuccess");
        JsonNode node;

        try {
            node = mapper().readTree(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("userAuthSuccess exception", e);
        }

        MacAddress mac = MacAddress.valueOf(node.path("mac").asText(null));
        String userId = node.path("userId").asText(null);
        String groupId = node.path("groupId").asText(null);

        log.info("mac = {}", mac);
        log.info("userId = {}", userId);
        log.info("groupId = {}", groupId);

        get(AclService.class).checkAclRule(mac, userId, groupId);
        return Response.ok().build();
    }

    private AclRule jsonToRule(InputStream stream) {
        JsonNode node;

        try {
            node = mapper().readTree(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse ACL request", e);
        }

        AclRule.Builder rule = AclRule.builder();

        String s = node.path("srcAttr").asText(null);
        if (s != null) {
            rule.srcAttr(s);
        }

        s = node.path("srcId").asText(null);
        if (s != null) {
            rule.srcId(s);
        }

        s = node.path("dstIp").asText(null);
        if (s != null) {
            rule.dstIp(Ip4Prefix.valueOf(s));
        }

        s = node.path("protocol").asText(null);
        if (s != null) {
            if ("TCP".equalsIgnoreCase(s)) {
                rule.protocol(IPv4.PROTOCOL_TCP);
            } else if ("UDP".equalsIgnoreCase(s)) {
                rule.protocol(IPv4.PROTOCOL_UDP);
            } else if ("ICMP".equalsIgnoreCase(s)) {
                rule.protocol(IPv4.PROTOCOL_ICMP);
            } else {
                throw new IllegalArgumentException("ipProto must be assigned to TCP, UDP, or ICMP");
            }
        }

        int port = node.path("dstPort").asInt(0);
        if (port > 0) {
            rule.dstPort((short) port);
        }

        int priority = node.path("priority").asInt(0);
        if (priority >= 0 && priority <= 9) {
            rule.priority(priority);
        }

        s = node.path("action").asText(null);
        if (s != null) {
            if ("allow".equalsIgnoreCase(s)) {
                rule.action(AclRule.Action.ALLOW);
            } else if ("deny".equalsIgnoreCase(s)) {
                rule.action(AclRule.Action.DENY);
            } else {
                throw new IllegalArgumentException("action must be ALLOW or DENY");
            }
        }

        return rule.build();
    }
}
