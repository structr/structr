/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.CorsSetting;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
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
public final class CorsSettingTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String REQUEST_URI_PROPERTY        = "requestUri";
	public static final String ACCEPTED_ORIGINS_PROPERTY   = "acceptedOrigins";
	public static final String MAX_AGE_PROPERTY            = "maxAge";
	public static final String ALLOW_METHODS_PROPERTY      = "allowMethods";
	public static final String ALLOW_HEADERS_PROPERTY      = "allowHeaders";
	public static final String ALLOW_CREDENTIALS_PROPERTY  = "allowCredentials";
	public static final String EXPOSE_HEADERS_PROPERTY     = "exposeHeaders";
	public static final String IS_CORS_SETTING_PROPERTY    = "isCorsSetting";

	public CorsSettingTraitDefinition() {
		super(StructrTraits.CORS_SETTING);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits = obj.getTraits();

					return ValidationHelper.isValidStringNotBlank(obj, traits.key(REQUEST_URI_PROPERTY), errorBuffer);
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
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String>               requestUri        = new StringProperty(REQUEST_URI_PROPERTY).indexed();
		final Property<String>               acceptedOrigins   = new StringProperty(ACCEPTED_ORIGINS_PROPERTY).indexed();
		final Property<Integer>              maxAge            = new IntProperty(MAX_AGE_PROPERTY).indexed();
		final Property<String>               allowMethods      = new StringProperty(ALLOW_METHODS_PROPERTY).indexed();
		final Property<String>               allowHeaders      = new StringProperty(ALLOW_HEADERS_PROPERTY).indexed();
		final Property<String>               allowCredentials  = new StringProperty(ALLOW_CREDENTIALS_PROPERTY).indexed();
		final Property<String>               exposeHeaders     = new StringProperty(EXPOSE_HEADERS_PROPERTY).indexed();
		final Property<Boolean>              isCorsSetting     = new ConstantBooleanProperty(IS_CORS_SETTING_PROPERTY, true);

		return newSet(
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


	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

				PropertyView.Public,
				newSet(
						REQUEST_URI_PROPERTY, ACCEPTED_ORIGINS_PROPERTY, MAX_AGE_PROPERTY, ALLOW_METHODS_PROPERTY,
						ALLOW_HEADERS_PROPERTY, ALLOW_CREDENTIALS_PROPERTY, EXPOSE_HEADERS_PROPERTY, IS_CORS_SETTING_PROPERTY
				),

				PropertyView.Ui,
				newSet(
						REQUEST_URI_PROPERTY, ACCEPTED_ORIGINS_PROPERTY, MAX_AGE_PROPERTY, ALLOW_METHODS_PROPERTY,
						ALLOW_HEADERS_PROPERTY, ALLOW_CREDENTIALS_PROPERTY, EXPOSE_HEADERS_PROPERTY, IS_CORS_SETTING_PROPERTY
				)
		);
	}
}
