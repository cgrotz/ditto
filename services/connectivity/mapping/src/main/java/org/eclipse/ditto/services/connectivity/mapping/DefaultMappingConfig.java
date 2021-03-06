/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.connectivity.mapping;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.services.connectivity.mapping.javascript.DefaultJavaScriptConfig;
import org.eclipse.ditto.services.connectivity.mapping.javascript.JavaScriptConfig;
import org.eclipse.ditto.services.utils.config.ConfigWithFallback;
import org.eclipse.ditto.services.utils.config.ScopedConfig;

import com.typesafe.config.Config;

/**
 * This class is the default implementation of the mapping config.
 */
@Immutable
public final class DefaultMappingConfig implements MappingConfig {

    private static final String CONFIG_PATH = "mapping";

    private final JavaScriptConfig javaScriptConfig;
    private final MapperLimitsConfig mapperLimitsConfig;

    private DefaultMappingConfig(final ScopedConfig config) {
        mapperLimitsConfig = DefaultMapperLimitsConfig.of(config);
        javaScriptConfig = DefaultJavaScriptConfig.of(config);
    }

    private DefaultMappingConfig(final ScopedConfig config, final JavaScriptConfig theJavaScriptConfig) {
        javaScriptConfig = theJavaScriptConfig;
        mapperLimitsConfig = DefaultMapperLimitsConfig.of(config);
    }

    /**
     * Returns an instance of {@code DefaultMappingConfig} based on the settings of the specified Config.
     *
     * @param config is supposed to provide the settings of the JavaScript mapping config at {@value #CONFIG_PATH}.
     * @return the instance.
     * @throws org.eclipse.ditto.services.utils.config.DittoConfigError if {@code config} is invalid.
     */
    public static DefaultMappingConfig of(final Config config) {
        final ConfigWithFallback mappingScopedConfig =
                ConfigWithFallback.newInstance(config, CONFIG_PATH, MappingConfigValue.values());

        return new DefaultMappingConfig(mappingScopedConfig);
    }

    @Override
    public JavaScriptConfig getJavaScriptConfig() {
        return javaScriptConfig;
    }

    @Override
    public MapperLimitsConfig getMapperLimitsConfig() {
        return mapperLimitsConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultMappingConfig that = (DefaultMappingConfig) o;
        return Objects.equals(javaScriptConfig, that.javaScriptConfig) &&
                Objects.equals(mapperLimitsConfig, that.mapperLimitsConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaScriptConfig, mapperLimitsConfig);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" +
                "javaScriptConfig=" + javaScriptConfig +
                "mapperLimitsConfig=" + mapperLimitsConfig +
                "]";
    }

}
