package org.ibwd.app.impl;

import org.ibwd.app.config.AppConfig;
import org.ibwd.app.config.ConfigService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Component(immediate = true)
@Service
public class ConfigImpl implements ConfigService {
    private static final String CONFIG_APP = "org.ibwd.app";
    private static final String CONFIG = "sw-type";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    private AppConfig appConfig = null;

    private ApplicationId appId;

    private ConfigFactory<ApplicationId, AppConfig> configFactory =
            new ConfigFactory<ApplicationId, AppConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, AppConfig.class, CONFIG) {
                @Override
                public AppConfig createConfig() {
                    return new AppConfig();
                }
            };

    private final InternalNetworkConfigListener configListener = new InternalNetworkConfigListener();

    @Activate
    protected void active() {
        networkConfigService.addListener(configListener);
        networkRegistry.registerConfigFactory(configFactory);
        loadConfiguration();
        log.info("ibwd Impl Started");
    }

    @Deactivate
    protected  void deactive() {
        networkRegistry.unregisterConfigFactory(configFactory);
        networkConfigService.removeListener(configListener);
        log.info("ibwd Impl Stopped");
    }

    private void loadConfiguration() {
        loadAppId();

        appConfig = networkConfigService.getConfig(appId, AppConfig.class);

        if (appConfig == null) {
            appConfig = networkConfigService.addConfig(appId, AppConfig.class);
        }
    }

    private void loadAppId() {
        appId = coreService.getAppId(CONFIG_APP);
    }

    public Set<String> getCoreSw() {
        return appConfig.getCoreSw();
    }

    public Set<String> getAggreSw() {
        return appConfig.getAggreSw();
    }

    public Set<String> getAccessSw() {
        return appConfig.getAccessSw();
    }

    public Set<String> getServerSw() {
        return appConfig.getServerSw();
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            log.info("NetworkConfigEvent1: {}",event.type());
            if (event.configClass() == ConfigService.CONFIG_CLASS) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED:
                    case CONFIG_REMOVED:
                        loadConfiguration();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}