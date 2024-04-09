/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.core.auth.Authenticator;
import org.structr.rest.ResourceProvider;
import org.structr.schema.compiler.NodeExtender;

import java.util.Set;

/**
 *
 *
 */
public class StructrHttpServiceConfig {

	private static final Logger logger            = LoggerFactory.getLogger(StructrHttpServiceConfig.class.getName());

	private String defaultPropertyView                = PropertyView.Public;
	private ResourceProvider resourceProvider         = null;
	private Class authenticatorClass                  = null;
	private boolean userAutoCreate                    = false;
	private boolean userAutoLogin                     = false;
	private int outputNestingDepth                    = 3;

	public ResourceProvider getResourceProvider() {
		return resourceProvider;
	}

	public void setResourceProvider(final ResourceProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
	}

	public String getDefaultPropertyView() {
		return defaultPropertyView;
	}

	public int getOutputNestingDepth() {
		return outputNestingDepth;
	}

	public void initializeFromSettings(final String servletName, final Set<ResourceProvider> resourceProviders) throws InstantiationException, IllegalAccessException {

		final String resourceProviderKeyName = "resourceprovider";
		final String authenticatorKeyName    = "authenticator";
		final String defaultPropertyKeyName  = "defaultview";
		final String nestingDepthKeyName     = "outputdepth";

		final String resourceProviderValue   = Settings.getOrCreateStringSetting(servletName, resourceProviderKeyName).getValue();
		final String authenticatorValue      = Settings.getOrCreateStringSetting(servletName, authenticatorKeyName).getValue();

		if (StringUtils.isBlank(resourceProviderValue)) {

			logger.error("Missing resource provider key {}.resourceprovider in configuration file.", servletName);

			throw new IllegalStateException("No resource provider set for servlet " + servletName);

		} else {

			final Class<ResourceProvider> providerClass = loadClass(resourceProviderValue);
			if (providerClass != null) {

				resourceProvider = providerClass.newInstance();
				resourceProviders.add(resourceProvider);

			} else {

				logger.error("Unable to initialize resource provider for servlet {}, no resource provider found. Please check structr.conf for a valid resource provider class.", servletName);

				throw new IllegalStateException("No resource provider available for servlet " + servletName);
			}
		}

		if (StringUtils.isBlank(authenticatorValue)) {

			logger.error("Missing authenticator key {}.authenticator in configuration file.", servletName);

		} else {

			authenticatorClass = loadClass(authenticatorValue);
			if (authenticatorClass == null) {

				logger.error("Unable to instantiate authenticator {}", authenticatorValue );
			}

		}

		this.defaultPropertyView = Settings.getOrCreateStringSetting(servletName, defaultPropertyKeyName).getValue("public");
		this.outputNestingDepth  = Settings.getOrCreateIntegerSetting(servletName, nestingDepthKeyName).getValue(0);
	}

	public Authenticator getAuthenticator() {

		Authenticator authenticator = null;

		if (authenticatorClass == null) {

			logger.error("No authenticator class loaded. Check log for 'Missing authenticator key'." );

			return null;
		}

		try {
			authenticator = (Authenticator) authenticatorClass.newInstance();

		} catch (InstantiationException | IllegalAccessException t) {

			logger.error("Unable to instantiate authenticator {}: {}", new Object[] { authenticatorClass, t.getMessage() } );
		}

		return authenticator;
	}

	// ----- private methods -----
	private Class loadClass(final String name) {

		ClassLoader loader = NodeExtender.getClassLoader();
		Class loadedClass  = null;

		if (loader == null) {
			loader = getClass().getClassLoader();
		}

		try {

			loadedClass = Class.forName(name, true, loader);

		} catch (Throwable ignore) {}

		if (loadedClass == null) {

			try {

				loadedClass = Class.forName(name);

			} catch (Throwable ignore) {}
		}

		return loadedClass;
	}
}
