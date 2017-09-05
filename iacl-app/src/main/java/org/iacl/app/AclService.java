package org.iacl.app;

import org.onlab.packet.MacAddress;
import java.util.*;

public interface AclService {

    List<AclRule> getAclRules();

    void addAclRule(AclRule rule);

    void removeAclRule(RuleId ruleId);

    void checkAclRule(MacAddress mac, String userId, String groupId);
}
