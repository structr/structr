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
package org.structr.core.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.CorsSetting;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.wrappers.CorsSettingTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Controls CORS settings
 *
 * These settings overwrite the system default and the settings defined in {@see Settings.java}.
 */
public final class CorsSettingTraitDefinition extends AbstractTraitDefinition {

	private static final Logger logger                                    = LoggerFactory.getLogger(CorsSettingTraitDefinition.class.getName());
	private static final Property<String>               requestUri        = new StringProperty("requestUri").indexed();
	private static final Property<String>               acceptedOrigins   = new StringProperty("acceptedOrigins").indexed();
	private static final Property<Integer>              maxAge            = new IntProperty("maxAge").indexed();
	private static final Property<String>               allowMethods      = new StringProperty("allowMethods").indexed();
	private static final Property<String>               allowHeaders      = new StringProperty("allowHeaders").indexed();
	private static final Property<String>               allowCredentials  = new StringProperty("allowCredentials").indexed();
	private static final Property<String>               exposeHeaders     = new StringProperty("exposeHeaders").indexed();
	private static final Property<Boolean>              isCorsSetting     = new ConstantBooleanProperty("isCorsSetting", true);

	public CorsSettingTraitDefinition() {
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

					return ValidationHelper.isValidStringNotBlank(obj,  CorsSettingTraitDefinition.requestUri, errorBuffer);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			CorsSetting.class, (traits, node) -> new CorsSettingTraitWrapper(traits, node)
		);
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
}
