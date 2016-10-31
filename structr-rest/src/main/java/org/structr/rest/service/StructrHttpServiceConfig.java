/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.core.Services;
import org.structr.core.auth.Authenticator;
import org.structr.rest.ResourceProvider;
import org.structr.schema.compiler.NodeExtender;

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

	public void initializeFromProperties(final Properties properties, final String servletName, final Set<ResourceProvider> resourceProviders) throws InstantiationException, IllegalAccessException {

		final String resourceProviderKeyName = servletName.concat(".resourceprovider");
		final String authenticatorKeyName    = servletName.concat(".authenticator");
		final String userAutoCreateKeyName   = servletName.concat(".user.autocreate");
		final String userAutoLoginKeyName    = servletName.concat(".user.autologin");
		final String defaultPropertyKeyName  = servletName.concat(".defaultview");
		final String nestingDepthKeyName     = servletName.concat(".outputdepth");

		final String resourceProviderValue   = properties.getProperty(resourceProviderKeyName);
		final String authenticatorValue      = properties.getProperty(authenticatorKeyName);
		final String userAutoCreateValue     = properties.getProperty(userAutoCreateKeyName);
		final String userAutoLoginValue      = properties.getProperty(userAutoLoginKeyName);
		final String defaultPropertyKeyValue = properties.getProperty(defaultPropertyKeyName);
		final String outputDepthValue        = properties.getProperty(nestingDepthKeyName);

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

		if (StringUtils.isNotBlank(userAutoCreateValue)) {
			userAutoCreate = Services.parseBoolean(userAutoCreateValue, false);
		}

		if (StringUtils.isNotBlank(userAutoLoginValue)) {
			userAutoLogin = Services.parseBoolean(userAutoLoginValue, false);
		}

		if (StringUtils.isNotBlank(defaultPropertyKeyValue)) {
			this.defaultPropertyView = defaultPropertyKeyValue;
		}

		if (StringUtils.isNotBlank(outputDepthValue)) {
			this.outputNestingDepth = Services.parseInt(outputDepthValue, 3);
		}
	}

	public Authenticator getAuthenticator() {

		Authenticator authenticator = null;

		if (authenticatorClass == null) {
			
			logger.error("No authenticator class loaded. Check log for 'Missing authenticator key'." );
			
			return null;
		}

		try {
			authenticator = (Authenticator) authenticatorClass.newInstance();
			authenticator.setUserAutoCreate(userAutoCreate);
			authenticator.setUserAutoLogin(userAutoLogin);

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
