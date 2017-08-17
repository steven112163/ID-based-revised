package org.ibwd.app;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onlab.packet.MacAddress;

import java.util.*;

public interface BandwidthAllocationService {

    public Long getQueue(DeviceId swId, PortNumber outPort, MacAddress srcMac, MacAddress dstMac);
}