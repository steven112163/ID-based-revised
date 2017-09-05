package org.iacl.app;

import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.core.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.*;

public final class AclRule {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RuleId aclId;
    private final String srcAttr;
    private final String srcId;
    private final Ip4Prefix dstIp;
    private final short dstPort;
    private final byte protocol;
    private final Action action;
    private final int priority;

    private static IdGenerator idGenerator;

    public enum Action {
        DENY, ALLOW
    }

    private AclRule() {
        this.aclId = null;
        this.srcAttr = null;
        this.srcId = null;
        this.dstIp = null;
        this.dstPort = 0;
        this.protocol = 0;
        this.action = null;
        this.priority = 0;
    }

    private AclRule(Long aclId, String srcAttr, String srcId, Ip4Prefix dstIp, short dstPort, byte protocol, Action action, int priority) {

        //checkState(idGenerator != null, "Id generator is not bound.");
        //this.aclId = RuleId.valueOf(idGenerator.getNewId());
        this.aclId = RuleId.valueOf(aclId);
        this.srcAttr = srcAttr;
        this.srcId = srcId;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.action = action;
        this.priority = priority;
    }

    private boolean checkCidrInCidr(Ip4Prefix cidrAddr1, Ip4Prefix cidrAddr2) {
        if (cidrAddr2 == null) {
            return true;
        } else if (cidrAddr1 == null) {
            return false;
        }
        if (cidrAddr1.prefixLength() < cidrAddr2.prefixLength()) {
            return false;
        }
        int offset = 32 - cidrAddr2.prefixLength();

        int cidr1Prefix = cidrAddr1.address().toInt();
        int cidr2Prefix = cidrAddr2.address().toInt();
        cidr1Prefix = cidr1Prefix >> offset;
        cidr2Prefix = cidr2Prefix >> offset;
        cidr1Prefix = cidr1Prefix << offset;
        cidr2Prefix = cidr2Prefix << offset;

        return (cidr1Prefix == cidr2Prefix);
    }

    public boolean checkMatch(AclRule r) {
        return (this.dstPort == r.dstPort || r.dstPort == 0)
                && (this.protocol == r.protocol || r.protocol == 0)
                && (checkCidrInCidr(this.dstIp(), r.dstIp()))
                && (this.srcAttr.equalsIgnoreCase(r.srcAttr) || r.srcAttr == null)
                && (this.srcId.equalsIgnoreCase(r.srcId) || r.srcId == null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        
        private Long aclId = null;
        private String srcAttr = null;
        private String srcId = null;
        private Ip4Prefix dstIp = null;
        private short dstPort = 0;
        private byte protocol = 0;
        private Action action = Action.DENY;
        private int priority = 0;
        
        private Builder() {
            // Hide constructor
        }

        public Builder aclId(Long aclId) {
            this.aclId = aclId;

            return this;
        }

        public Builder srcAttr(String srcAttr) {
            if ((srcAttr.equalsIgnoreCase("User") || srcAttr.equalsIgnoreCase("Group")))
                this.srcAttr = srcAttr;

            return this;
        }

        public Builder srcId(String srcId) {
            this.srcId = srcId;
            return this;
        }

        public Builder dstIp(Ip4Prefix dstIp) {
            this.dstIp = dstIp;
            return this;
        }

        public Builder protocol(byte protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder dstPort(short dstPort) {
            if ((protocol == IPv4.PROTOCOL_TCP || protocol == IPv4.PROTOCOL_UDP)) {
                this.dstPort = dstPort;
            }
            return this;
        }
        
        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public AclRule build() {
            checkState(srcAttr != null && srcId != null, "Either srcAttr or srcId must be assigned.");
            checkState(dstIp != null, "dstIp must be assigned.");
            checkState(protocol == 0 || protocol == IPv4.PROTOCOL_ICMP || protocol == IPv4.PROTOCOL_TCP || protocol == IPv4.PROTOCOL_UDP,
                       "protocol must be assigned to TCP, UDP, or ICMP.");
            checkState(priority >= 0 && priority <=9, "Range of priority is 0~9.");
            if(aclId == null) {
                checkState(idGenerator != null, "Id generator is not bound.");
                aclId = idGenerator.getNewId();
            }

            return new AclRule(aclId, srcAttr, srcId, dstIp, dstPort, protocol, action, priority);
        }
    }

    public static void bindIdGenerator(IdGenerator newIdGenerator) {
        checkState(idGenerator == null, "Id generator is already bound.");
        idGenerator = checkNotNull(newIdGenerator);
    }

    public RuleId aclId() {
        return aclId;
    }
    
    public String srcAttr() {
        return srcAttr;
    }

    public String srcId() {
        return srcId;
    }

    public Ip4Prefix dstIp() {
        return dstIp;
    }

    public short dstPort() {
        return dstPort;
    }

    public byte protocol() {
        return protocol;
    }

    public Action action() {
        return action;
    }

    public int priority() {
        return priority;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AclRule) {
            AclRule that = (AclRule) obj;
            return Objects.equals(aclId, that.aclId) &&
                    Objects.equals(srcAttr, that.srcAttr) &&
                    Objects.equals(srcId, that.srcId) &&
                    Objects.equals(dstIp, that.dstIp) &&
                    Objects.equals(dstPort, that.dstPort) &&
                    Objects.equals(protocol, that.protocol) &&
                    Objects.equals(action, that.action) &&
                    Objects.equals(priority, that.priority);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("aclId", aclId)
                .add("srcAttr", srcAttr)
                .add("srcId", srcId)
                .add("dstIp", dstIp)
                .add("dstPort", dstPort)
                .add("protocol", protocol)
                .add("action", action)
                .add("priority", priority)
                .toString();
    }
}
