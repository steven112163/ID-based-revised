package org.ibwd.app.config;

import com.google.common.collect.SetMultimap;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;

import java.util.Map;
import java.util.Set;

public interface ConfigService {
    Class<AppConfig> CONFIG_CLASS = AppConfig.class;

    Set<String> getCoreSw();
}