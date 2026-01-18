/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AddToIndex;
import org.structr.core.traits.operations.graphobject.IndexPassiveProperties;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.propertycontainer.GetVisibilityFlags;

import java.util.*;

public final class GraphObjectTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String ID_PROPERTY                             = "id";
	public static final String TYPE_PROPERTY                           = "type";
	public static final String CREATED_DATE_PROPERTY                   = "createdDate";
	public static final String CREATED_BY_PROPERTY                     = "createdBy";
	public static final String LAST_MODIFIED_DATE_PROPERTY             = "lastModifiedDate";
	public static final String LAST_MODIFIED_BY_PROPERTY               = "lastModifiedBy";
	public static final String VISIBLE_TO_PUBLIC_USERS_PROPERTY        = "visibleToPublicUsers";
	public static final String VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY = "visibleToAuthenticatedUsers";

	public GraphObjectTraitDefinition() {
		super(StructrTraits.GRAPH_OBJECT);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject graphObject, final ErrorBuffer errorBuffer) {

					final SecurityContext securityContext = graphObject.getSecurityContext();
					final Traits traits                   = graphObject.getTraits();
					final PropertyKey idProperty          = traits.key(ID_PROPERTY);
					final PropertyKey typeProperty        = traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY);
					boolean valid = true;

					valid &= ValidationHelper.isValidStringNotBlank(graphObject, idProperty, errorBuffer);

					if (securityContext != null && securityContext.uuidWasSetManually() && graphObject.isNode()) {
						valid &= ValidationHelper.isValidGloballyUniqueProperty(graphObject, idProperty, errorBuffer);
					}

					valid &= ValidationHelper.isValidUuid(graphObject, idProperty, errorBuffer);
					valid &= ValidationHelper.isValidStringNotBlank(graphObject, typeProperty, errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			AddToIndex.class,
			new AddToIndex() {

				@Override
				public void addToIndex(final GraphObject graphObject) {

					final Set<PropertyKey> indexKeys = new LinkedHashSet<>();

					for (PropertyKey key : graphObject.getPropertyKeys(PropertyView.All)) {

						if (key.isIndexed()) {

							indexKeys.add(key);
						}
					}

					GraphObjectTraitDefinition.this.addToIndex(graphObject, indexKeys);
				}
			},

			IndexPassiveProperties.class,
			new IndexPassiveProperties() {

				@Override
				public void indexPassiveProperties(final org.structr.core.GraphObject graphObject) {

					final Set<PropertyKey> passiveIndexingKeys = new LinkedHashSet<>();

					for (PropertyKey key : graphObject.getPropertyKeys(PropertyView.All)) {

						if (key.isIndexed() && (key.isPassivelyIndexed() || key.isIndexedWhenEmpty())) {

							passiveIndexingKeys.add(key);
						}
					}

					GraphObjectTraitDefinition.this.addToIndex(graphObject, passiveIndexingKeys);
				}
			},

			GetVisibilityFlags.class,
			new GetVisibilityFlags() {

				@Override
				public boolean isVisibleToPublicUsers(final GraphObject obj) {

					final Traits traits = obj.getTraits();

					return obj.getProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY));
				}

				@Override
				public boolean isVisibleToAuthenticatedUsers(final GraphObject obj) {

					final Traits traits = obj.getTraits();

					return obj.getProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY));
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> typeProperty                  = new TypeProperty().category(GraphObject.SYSTEM_CATEGORY).description("type of this entity");
		final PropertyKey<String> idProperty                    = new UuidProperty().category(GraphObject.SYSTEM_CATEGORY).description("UUID of this entity");
		final PropertyKey<Date> createdDateProperty             = new ISO8601DateProperty(CREATED_DATE_PROPERTY).readOnly().systemInternal().indexed().unvalidated().writeOnce().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly().description("when this entity was created");
		final PropertyKey<String> createdByProperty             = new StringProperty(CREATED_BY_PROPERTY).readOnly().writeOnce().unvalidated().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();
		final PropertyKey<Date> lastModifiedDateProperty        = new ISO8601DateProperty(LAST_MODIFIED_DATE_PROPERTY).readOnly().systemInternal().passivelyIndexed().unvalidated().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly().description("when this entity was last modified");
		final PropertyKey<String> lastModifiedByProperty        = new StringProperty(LAST_MODIFIED_BY_PROPERTY).readOnly().systemInternal().unvalidated().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();
		final PropertyKey<Boolean> visibleToPublicUsersProperty = new BooleanProperty(VISIBLE_TO_PUBLIC_USERS_PROPERTY).passivelyIndexed().category(GraphObject.VISIBILITY_CATEGORY).category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly().description("whether this entity is visible to public users");
		final Property<Boolean> visibleToAuthenticatedUsers     = new BooleanProperty(VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY).passivelyIndexed().category(GraphObject.VISIBILITY_CATEGORY).category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly().description("whether this entity is visible to authenticated users");

		return newSet(
			idProperty,
			typeProperty,
			createdDateProperty,
			createdByProperty,
			lastModifiedDateProperty,
			lastModifiedByProperty,
			visibleToPublicUsersProperty,
			visibleToAuthenticatedUsers
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
					ID_PROPERTY, TYPE_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					ID_PROPERTY, TYPE_PROPERTY, CREATED_BY_PROPERTY,
					CREATED_DATE_PROPERTY, LAST_MODIFIED_DATE_PROPERTY,
					VISIBLE_TO_PUBLIC_USERS_PROPERTY, VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private methods -----
	private void addToIndex(final GraphObject obj, final Set<PropertyKey> indexKeys) {

		final Map<String, Object> values = new LinkedHashMap<>();

		for (PropertyKey key : indexKeys) {

			final PropertyConverter converter = key.databaseConverter(obj.getSecurityContext(), obj);

			if (converter != null) {

				try {

					final Object value = converter.convert(obj.getProperty(key));
					if (key.isPropertyValueIndexable(value)) {

						values.put(key.dbName(), value);
					}

				} catch (FrameworkException ex) {

					final Logger logger = LoggerFactory.getLogger(org.structr.core.GraphObject.class);
					logger.warn("Unable to convert property {} of type {}: {}", key, getClass().getSimpleName(), ex.getMessage());
					logger.warn("Exception", ex);
				}


			} else {

				final Object value = obj.getProperty(key);
				if (key.isPropertyValueIndexable(value)) {

					// index unconverted value
					values.put(key.dbName(), value);
				}
			}
		}

		try {

			// use "internal" setProperty for "indexing"
			obj.getPropertyContainer().setProperties(values);

		} catch (UnknownClientException | UnknownDatabaseException e) {

			final Logger logger = LoggerFactory.getLogger(org.structr.core.GraphObject.class);
			logger.warn("Unable to index properties of {} with UUID {}: {}", obj.getType(), obj.getUuid(), e.getMessage());
			logger.warn("Properties: {}", values);
		}
	}
}
