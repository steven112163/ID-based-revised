package org.iacl.app;

import org.onlab.util.Identifier;

public final class RuleId extends Identifier<Long> {

    public static RuleId valueOf(long value) {
        return new RuleId(value);
    }

    RuleId() {
        super(0L);
    }

    RuleId(long value) {
        super(value);
    }

    public long fingerprint() {
        return identifier;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(identifier);
    }
}
