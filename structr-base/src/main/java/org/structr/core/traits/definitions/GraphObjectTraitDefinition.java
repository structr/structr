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
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AddToIndex;
import org.structr.core.traits.operations.graphobject.IndexPassiveProperties;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.propertycontainer.GetVisibilityFlags;

import java.util.*;

public final class GraphObjectTraitDefinition extends AbstractNodeTraitDefinition {

	public GraphObjectTraitDefinition() {
		super(StructrTraits.GRAPH_OBJECT);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject graphObject, final ErrorBuffer errorBuffer) {

					final SecurityContext securityContext = graphObject.getSecurityContext();
					final Traits traits                   = graphObject.getTraits();
					final PropertyKey idProperty          = traits.key("id");
					final PropertyKey typeProperty        = traits.key("type");
					boolean valid = true;

					// the following two checks can be omitted in release 2.4 when Neo4j uniqueness constraints are live
					valid &= ValidationHelper.isValidStringNotBlank(graphObject, idProperty, errorBuffer);

					if (securityContext != null && securityContext.uuidWasSetManually()) {
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

					return obj.getProperty(traits.key("visibleToPublicUsers"));
				}

				@Override
				public boolean isVisibleToAuthenticatedUsers(final GraphObject obj) {

					final Traits traits = obj.getTraits();

					return obj.getProperty(traits.key("visibleToAuthenticatedUsers"));
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> baseProperty                  = new StringProperty("base");
		final PropertyKey<String> typeProperty                  = new TypeProperty().category(GraphObject.SYSTEM_CATEGORY);
		final PropertyKey<String> idProperty                    = new UuidProperty().category(GraphObject.SYSTEM_CATEGORY);
		final PropertyKey<Date> createdDateProperty             = new ISO8601DateProperty("createdDate").readOnly().systemInternal().indexed().unvalidated().writeOnce().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();
		final PropertyKey<String> createdByProperty             = new StringProperty("createdBy").readOnly().writeOnce().unvalidated().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();
		final PropertyKey<Date> lastModifiedDateProperty        = new ISO8601DateProperty("lastModifiedDate").readOnly().systemInternal().passivelyIndexed().unvalidated().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();
		final PropertyKey<String> lastModifiedByProperty        = new StringProperty("lastModifiedBy").readOnly().systemInternal().unvalidated().category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();
		final PropertyKey<Boolean> visibleToPublicUsersProperty = new BooleanProperty("visibleToPublicUsers").passivelyIndexed().category(GraphObject.VISIBILITY_CATEGORY).category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();
		final Property<Boolean> visibleToAuthenticatedUsers     = new BooleanProperty("visibleToAuthenticatedUsers").passivelyIndexed().category(GraphObject.VISIBILITY_CATEGORY).category(GraphObject.SYSTEM_CATEGORY).nodeIndexOnly();

		return newSet(
			baseProperty,
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
			newSet("id", "type"),

			PropertyView.Ui,
			newSet("id", "type", "createdBy", "createdDate", "lastModifiedDate", "visibleToPublicUsers", "visibleToAuthenticatedUsers")
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
