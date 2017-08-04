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

    public Set<String> getCoreSw() {
        Set<String> cresSw = Sets.newHashSet();
        JsonNode jsonNode = object.get(CORE_SW);

        if (jsonNode.toString().isEmpty()) {
            jsonNode = ((ObjectNode) jsonNode).putArray(CORE_SW);
        }
        jsonNode.forEach(uplinkInface -> cresSw.add(uplinkInface.asText()));

        return cresSw;
    }
}