/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.service;

import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.auth.Authenticator;
import org.structr.core.property.Property;
import org.structr.rest.ResourceProvider;

/**
 *
 * @author Christian Morgner
 */
public class HttpServiceServlet extends HttpServlet {

	private static final Logger logger            = Logger.getLogger(HttpServiceServlet.class.getName());

	protected Property<String> defaultIdProperty  = GraphObject.id;
	protected String defaultPropertyView          = PropertyView.Public;
	protected ResourceProvider resourceProvider   = null;
	protected Class authenticatorClass            = null;
	protected Class userClass                     = null;
	protected boolean userAutoCreate              = false;
	protected int outputNestingDepth              = 3;

	public void initializeFromProperties(final Properties properties, final String servletName, final Set<ResourceProvider> resourceProviders) {

		final String resourceProviderKeyName = servletName.concat(".resourceprovider");
		final String authenticatorKeyName    = servletName.concat(".authenticator");
		final String userClassKeyName        = servletName.concat(".user.class");
		final String userAutoCreateKeyName   = servletName.concat(".user.autocreate");
		final String defaultPropertyKeyName  = servletName.concat(".defaultview");
		final String nestingDepthKeyName     = servletName.concat(".outputdepth");
		
		final String resourceProviderValue   = properties.getProperty(resourceProviderKeyName);
		final String authenticatorValue      = properties.getProperty(authenticatorKeyName);
		final String userClassValue          = properties.getProperty(userClassKeyName);
		final String userAutoCreateValue     = properties.getProperty(userAutoCreateKeyName);
		final String defaultPropertyKeyValue = properties.getProperty(defaultPropertyKeyName);
		final String outputDepthValue        = properties.getProperty(nestingDepthKeyName);
		
		if (resourceProviderValue == null) {

			logger.log(Level.SEVERE, "Missing resource provider key {0}.resourceprovider in configuration file.", servletName);
			
		} else {

			try {
				resourceProvider = (ResourceProvider)Class.forName(resourceProviderValue).newInstance();
				resourceProviders.add(resourceProvider);

			} catch (Throwable t) {

				logger.log(Level.SEVERE, "Unable to instantiate resource provider {0}: {1}", new Object[] { resourceProviderValue, t.getMessage() } );
			}
			
		}
		
		if (authenticatorValue == null) {
			
			logger.log(Level.SEVERE, "Missing authenticator key {0}.authenticator in configuration file.", servletName);
			
		} else {

			try {
				authenticatorClass = Class.forName(authenticatorValue);

			} catch (Throwable t) {

				logger.log(Level.SEVERE, "Unable to instantiate authenticator {0}: {1}", new Object[] { authenticatorValue, t.getMessage() } );
			}
			
		}
		
		if (userClassValue != null) {

			try {
				userClass = Class.forName(userClassValue);

			} catch (Throwable t) {

				logger.log(Level.SEVERE, "Unable to instantiate user class for authenticator {0}: {1}", new Object[] { userClassValue, t.getMessage() } );
			}
		}
		
		if (userAutoCreateValue != null) {
			userAutoCreate = parseBoolean(userAutoCreateValue, false);
		}
		
		if (defaultPropertyKeyValue != null) {
			this.defaultPropertyView = defaultPropertyKeyValue;
		}
		
		if (outputDepthValue != null) {
			this.outputNestingDepth = parseInt(outputDepthValue, 3);
		}
	}

	protected Authenticator getAuthenticator() {

		Authenticator authenticator = null;

		try {
			authenticator = (Authenticator)authenticatorClass.newInstance();
			authenticator.setUserAutoCreate(userAutoCreate, userClass);

		} catch (Throwable t) {

			logger.log(Level.SEVERE, "Unable to instantiate authenticator {0}: {1}", new Object[] { authenticatorClass, t.getMessage() } );
		}
		
		return authenticator;
	}

	/**
	 * Tries to parse the given String to an int value, returning
	 * defaultValue on error.
	 *
	 * @param value the source String to parse
	 * @param defaultValue the default value that will be returned when parsing fails
	 * @return the parsed value or the given default value when parsing fails
	 */
	public static int parseInt(String value, int defaultValue) {

		if (value == null) {

			return defaultValue;

		}

		try {
			return Integer.parseInt(value);
		} catch (Throwable ignore) {}

		return defaultValue;
	}

	public static boolean parseBoolean(Object source, boolean defaultValue) {
		
		try { return Boolean.parseBoolean(source.toString()); } catch(Throwable ignore) {}
		
		return defaultValue;
	}
}
