package org.ibwd.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.Config;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class AppConfig extends Config<ApplicationId> {
    private static final String CORE_SW = "coreSw";
    private static final String AGGRE_SW = "aggreSw";
    private static final String ACCESS_SW = "accessSw";
    private static final String SERVER_SW = "serverSw";

    public Set<String> getCoreSw() {
        Set<String> coreSw = Sets.newHashSet();
        JsonNode jsonNode = object.get(CORE_SW);

        if (jsonNode.toString().isEmpty()) {
            jsonNode = ((ObjectNode) jsonNode).putArray(CORE_SW);
        }
        jsonNode.forEach(swType -> coreSw.add(swType.asText()));

        return coreSw;
    }

    public Set<String> getAggreSw() {
        Set<String> aggreSw = Sets.newHashSet();
        JsonNode jsonNode = object.get(AGGRE_SW);

        if (jsonNode.toString().isEmpty()) {
            jsonNode = ((ObjectNode) jsonNode).putArray(AGGRE_SW);
        }
        jsonNode.forEach(swType -> aggreSw.add(swType.asText()));

        return aggreSw;
    }

    public Set<String> getAccessSw() {
        Set<String> accessSw = Sets.newHashSet();
        JsonNode jsonNode = object.get(ACCESS_SW);

        if (jsonNode.toString().isEmpty()) {
            jsonNode = ((ObjectNode) jsonNode).putArray(ACCESS_SW);
        }
        jsonNode.forEach(swType -> accessSw.add(swType.asText()));

        return accessSw;
    }

    public Set<String> getServerSw() {
        Set<String> serverSw = Sets.newHashSet();
        JsonNode jsonNode = object.get(SERVER_SW);

        if (jsonNode.toString().isEmpty()) {
            jsonNode = ((ObjectNode) jsonNode).putArray(SERVER_SW);
        }
        jsonNode.forEach(swType -> serverSw.add(swType.asText()));

        return serverSw;
    }
}