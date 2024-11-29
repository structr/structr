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
package org.structr.core.traits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.UnknownClientException;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.*;

import java.util.*;

public final class GraphObjectTraitImplementation extends AbstractTraitImplementation implements GraphObjectTrait {

	private static final Logger logger = LoggerFactory.getLogger(GraphObjectTrait.class);

	private static final String SYSTEM_CATEGORY     = "System";
	private static final String VISIBILITY_CATEGORY = "Visibility";

	static {

		/*
		final Trait trait = Trait.create(GraphObjectTrait.class, n -> new GraphTraitImpl(n) {});

		trait.registerProperty(new StringProperty("base").partOfBuiltInSchema());
		trait.registerProperty();
		trait.registerProperty(new ISO8601DateProperty("createdDate").readOnly().systemInternal().indexed().unvalidated().writeOnce().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new StringProperty("createdBy").readOnly().writeOnce().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new ISO8601DateProperty("lastModifiedDate").readOnly().systemInternal().passivelyIndexed().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new StringProperty("lastModifiedBy").readOnly().systemInternal().unvalidated().partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());
		trait.registerProperty(new BooleanProperty("visibleToPublicUsers").passivelyIndexed().category(VISIBILITY_CATEGORY).partOfBuiltInSchema().category(SYSTEM_CATEGORY).nodeIndexOnly());

		 */
	}

	protected GraphObjectTraitImplementation(final Traits traits) {

		super(traits);

		registerProperty(new TypeProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY));
		registerProperty(new UuidProperty().partOfBuiltInSchema().category(SYSTEM_CATEGORY));
	}

	@Override
	public boolean isValid(final GraphObject graphObject, final ErrorBuffer errorBuffer) {

		final SecurityContext securityContext = graphObject.getSecurityContext();
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

	@Override
	public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
	}

	@Override
	public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
	}

	@Override
	public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
	}

	@Override
	public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterModification(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {
	}

	@Override
	public void afterDeletion(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties) {
	}

	@Override
	public void ownerModified(final GraphObject graphObject, final SecurityContext securityContext) {
	}

	@Override
	public void securityModified(final GraphObject graphObject, final SecurityContext securityContext) {
	}

	@Override
	public void locationModified(final GraphObject graphObject, final SecurityContext securityContext) {
	}

	@Override
	public void propagatedModification(final GraphObject graphObject, final SecurityContext securityContext) {
	}

	@Override
	public void indexPassiveProperties(final GraphObject obj) {

		final Set<PropertyKey> passiveIndexingKeys = new LinkedHashSet<>();

		for (PropertyKey key : obj.getPropertyKeys(PropertyView.All)) {

			if (key.isIndexed() && (key.isPassivelyIndexed() || key.isIndexedWhenEmpty())) {

				passiveIndexingKeys.add(key);
			}
		}

		addToIndex(obj, passiveIndexingKeys);
	}

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

					final Logger logger = LoggerFactory.getLogger(GraphObject.class);
					logger.warn("Unable to convert property {} of type {}: {}", key.dbName(), getClass().getSimpleName(), ex.getMessage());
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

			final Logger logger = LoggerFactory.getLogger(GraphObject.class);
			logger.warn("Unable to index properties of {} with UUID {}: {}", obj.getType(), obj.getUuid(), e.getMessage());
			logger.warn("Properties: {}", values);
		}
	}
}
