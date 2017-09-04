package org.iacl.app;

import java.util.*;

public interface AclService {

    List<AclRule> getAclRules();

    boolean addAclRule(AclRule rule) throws Exception;

    void removeAclRule(RuleId ruleId) throws Exception;
}
