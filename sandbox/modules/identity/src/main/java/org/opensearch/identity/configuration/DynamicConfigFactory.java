/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.configuration;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opensearch.identity.User;
import org.opensearch.identity.configuration.model.InternalUsersModel;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.opensearch.client.Client;
import org.opensearch.common.settings.Settings;
import org.opensearch.identity.extensions.ExtensionSecurityConfig;
import org.opensearch.identity.extensions.ExtensionSecurityConfigStore;
import org.opensearch.identity.extensions.ExtensionSecurityConfigModel;
import org.opensearch.identity.realm.InternalUsersStore;
import org.opensearch.threadpool.ThreadPool;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusBuilder;

public class DynamicConfigFactory implements ConfigurationChangeListener {

    public static final EventBusBuilder EVENT_BUS_BUILDER = EventBus.builder();

    protected final Logger log = LogManager.getLogger(this.getClass());
    private final ConfigurationRepository cr;
    private final AtomicBoolean initialized = new AtomicBoolean();
    private final EventBus eventBus = EVENT_BUS_BUILDER.build();
    private final Settings opensearchSettings;
    private final Path configPath;

    SecurityDynamicConfiguration<?> config;

    public DynamicConfigFactory(
        ConfigurationRepository cr,
        final Settings opensearchSettings,
        final Path configPath,
        Client client,
        ThreadPool threadPool,
        ClusterInfoHolder cih
    ) {
        super();
        this.cr = cr;
        this.opensearchSettings = opensearchSettings;
        this.configPath = configPath;

        registerDCFListener(InternalUsersStore.getInstance());
        registerDCFListener(ExtensionSecurityConfigStore.getInstance());
        this.cr.subscribeOnChange(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onChange(Map<CType, SecurityDynamicConfiguration<?>> typeToConfig) {

        SecurityDynamicConfiguration<?> internalusers = cr.getConfiguration(CType.INTERNALUSERS);

        if (log.isDebugEnabled()) {
            String logmsg = "current config (because of "
                + typeToConfig.keySet()
                + ")\n"
                + " internalusers: "
                + internalusers.getImplementingClass()
                + " with "
                + internalusers.getCEntries().size()
                + " entries\n";
            log.debug(logmsg);

        }

        final InternalUsersModel ium;
        ium = new InternalUsersModelV1((SecurityDynamicConfiguration<User>) internalusers);

        eventBus.post(ium);
        eventBus.post(cr.getConfiguration(CType.EXTENSIONSECURITY));

        if (!isInitialized()) {
            initialized.set(true);
            cr.initializeExtensionsSecurity();
        }

    }

    public final boolean isInitialized() {
        return initialized.get();
    }

    public void registerDCFListener(Object listener) {
        eventBus.register(listener);
    }

    public void unregisterDCFListener(Object listener) {
        eventBus.unregister(listener);
    }

    private static class InternalUsersModelV1 extends InternalUsersModel {

        private final SecurityDynamicConfiguration<User> internalUserSecurityDC;

        public InternalUsersModelV1(SecurityDynamicConfiguration<User> internalUserSecurityDC) {
            super();
            this.internalUserSecurityDC = internalUserSecurityDC;
        }

        @Override
        public User getUser(String username) {
            return internalUserSecurityDC.getCEntry(username);
        }

        @Override
        public boolean exists(String username) {
            return internalUserSecurityDC.exists(username);
        }

        @Override
        public Map<String, String> getAttributes(String username) {
            User tmp = internalUserSecurityDC.getCEntry(username);
            return tmp == null ? null : tmp.getAttributes();
        }

        @Override
        public String getHash(String username) {
            User tmp = internalUserSecurityDC.getCEntry(username);
            return tmp == null ? null : tmp.getBcryptHash();
        }
    }

    private static class ExtensionsSecurityConfigModelV1 extends ExtensionSecurityConfigModel {

        private final SecurityDynamicConfiguration<ExtensionSecurityConfig> extensionsSecurityConfigDC;

        public ExtensionsSecurityConfigModelV1(SecurityDynamicConfiguration<ExtensionSecurityConfig> extensionsSecurityConfigDC) {
            super();
            this.extensionsSecurityConfigDC = extensionsSecurityConfigDC;
        }

        @Override
        public ExtensionSecurityConfig getExtensionSecurityConfig(String extensionId) {
            return extensionsSecurityConfigDC.getCEntry(extensionId);
        }

        @Override
        public boolean exists(String extensionId) {
            return extensionsSecurityConfigDC.exists(extensionId);
        }
    }
}
