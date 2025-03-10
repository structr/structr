/*
 * Copyright (C) 2010-2024 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.ldap.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.ldap.LDAPGroup;
import org.structr.ldap.traits.wrappers.LDAPGroupTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 */
public class LDAPGroupTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DISTINGUISHED_NAME_PROPERTY = "distinguishedName";
	public static final String PATH_PROPERTY               = "path";
	public static final String FILTER_PROPERTY             = "filter";
	public static final String SCOPE_PROPERTY              = "scope";

	public LDAPGroupTraitDefinition() {
		super(StructrTraits.LDAP_GROUP);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {
				@Override
				public Boolean isValid(GraphObject obj, ErrorBuffer errorBuffer) {
					return ValidationHelper.isValidUniqueProperty(obj, obj.getTraits().key(DISTINGUISHED_NAME_PROPERTY), errorBuffer);
				}
			},

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					graphObject.as(LDAPGroup.class).update(securityContext);
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
					graphObject.as(LDAPGroup.class).update(securityContext);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(

			LDAPGroup.class, (traits, node) -> new LDAPGroupTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> distinguishedNameProperty = new StringProperty(DISTINGUISHED_NAME_PROPERTY).unique().indexed();
		final PropertyKey<String> pathProperty              = new StringProperty(PATH_PROPERTY);
		final PropertyKey<String> filterProperty            = new StringProperty(FILTER_PROPERTY);
		final PropertyKey<String> scopeProperty             = new StringProperty(SCOPE_PROPERTY);

		return newSet(
			distinguishedNameProperty,
			pathProperty,
			filterProperty,
			scopeProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				DISTINGUISHED_NAME_PROPERTY, PATH_PROPERTY, FILTER_PROPERTY, SCOPE_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				DISTINGUISHED_NAME_PROPERTY, PATH_PROPERTY, FILTER_PROPERTY, SCOPE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
