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
package org.structr.core.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.property.*;
import org.structr.core.traits.AbstractTraitDefinition;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;

import java.util.Map;
import java.util.Set;

/**
 * Controls CORS settings
 *
 * These settings overwrite the system default and the settings defined in {@see Settings.java}.
 */
public class CorsSettingDefinition extends AbstractTraitDefinition {

	private static final Logger logger                                = LoggerFactory.getLogger(CorsSettingDefinition.class.getName());

	/** Request URL that has to match */
	private static final Property<String>               requestUri          = new StringProperty("requestUri").indexed();

	/** Comma-separated list of accepted origins, sets the <code>Access-Control-Allow-Origin</code> header. Overwrites {@link Settings#AccessControlAcceptedOrigins} */
	private static final Property<String>               acceptedOrigins     = new StringProperty("acceptedOrigins").indexed();

	/** Sets the value of the <code>Access-Control-Max-Age</code> header. Unit is seconds. Overwrites @see Settings.AccessControlMaxAge */
	private static final Property<Integer>              maxAge              = new IntProperty("maxAge").indexed();

	/** Sets the value of the <code>Access-Control-Allow-Methods</code> header. Comma-delimited list of the allowed HTTP request methods. Overwrites @see Settings.AccessControlAllowMethods */
	private static final Property<String>               allowMethods        = new StringProperty("allowMethods").indexed();

	/** Sets the value of the <code>Access-Control-Allow-Headers</code> header. Overwrites @see Settings.AccessControlAllowHeaders */
	private static final Property<String>               allowHeaders        = new StringProperty("allowHeaders").indexed();

	/** Sets the value of the <code>Access-Control-Allow-Credentials</code> header. Overwrites @see Settings.AccessControlAllowCredentials */
	private static final Property<String>               allowCredentials    = new StringProperty("allowCredentials").indexed();

	/** Sets the value of the <code>Access-Control-Expose-Headers</code> header. Overwrites @see Settings.AccessControlExposeHeaders */
	private static final Property<String>               exposeHeaders       = new StringProperty("exposeHeaders").indexed();

	private static final Property<Boolean>              isCorsSetting   = new ConstantBooleanProperty("isCorsSetting", true);

	public CorsSettingDefinition() {
		super("CorsSetting");
	}

	/*
	public static final View uiView = new View(CorsSetting.class, PropertyView.Ui,
			requestUri, acceptedOrigins, maxAge, allowMethods, allowHeaders, allowCredentials, exposeHeaders, isCorsSetting
	);

	public static final View publicView = new View(CorsSetting.class, PropertyView.Public,
			requestUri, acceptedOrigins, maxAge, allowMethods, allowHeaders, allowCredentials, exposeHeaders, isCorsSetting
	);
	*/

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					return ValidationHelper.isValidStringNotBlank(obj,  CorsSettingDefinition.requestUri, errorBuffer);
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of(
			requestUri,
			acceptedOrigins,
			maxAge,
			allowMethods,
			allowHeaders,
			allowCredentials,
			exposeHeaders,
			isCorsSetting
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
