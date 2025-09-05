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
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Localization;
import org.structr.core.entity.Relation;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.LocalizationTraitWrapper;

import java.util.Map;
import java.util.Set;

public final class LocalizationTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String LOCALIZED_NAME_PROPERTY = "localizedName";
	public static final String DOMAIN_PROPERTY         = "domain";
	public static final String LOCALE_PROPERTY         = "locale";
	public static final String IMPORTED_PROPERTY       = "imported";

	public LocalizationTraitDefinition() {
		super(StructrTraits.LOCALIZATION);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {
				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits = obj.getTraits();

					return ValidationHelper.isValidPropertyNotNull(obj, traits.key(LOCALE_PROPERTY), errorBuffer);
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final Traits traits = graphObject.getTraits();

					graphObject.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
					graphObject.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

					LocalizeFunction.invalidateCache();
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					LocalizeFunction.invalidateCache();
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					LocalizeFunction.invalidateCache();
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Localization.class, (traits, node) -> new LocalizationTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> localizedNameProperty = new StringProperty(LOCALIZED_NAME_PROPERTY).indexed();
		final Property<String> domainProperty        = new StringProperty(DOMAIN_PROPERTY).indexed();
		final Property<String> localeProperty        = new StringProperty(LOCALE_PROPERTY).notNull().indexed();
		final Property<Boolean> importedProperty     = new BooleanProperty(IMPORTED_PROPERTY);

		return newSet(
			localizedNameProperty,
			domainProperty,
			localeProperty,
			importedProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					LOCALIZED_NAME_PROPERTY, DOMAIN_PROPERTY, LOCALE_PROPERTY, IMPORTED_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					LOCALIZED_NAME_PROPERTY, DOMAIN_PROPERTY, LOCALE_PROPERTY, IMPORTED_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
