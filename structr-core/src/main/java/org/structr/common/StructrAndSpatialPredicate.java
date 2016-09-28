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
package org.structr.common;

import org.apache.commons.lang.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.schema.ConfigurationProvider;

/**
 *
 *
 */

public class StructrAndSpatialPredicate<T extends PropertyContainer> implements Predicate<T> {

	private static final String typeName = GraphObject.type.dbName();
	private static final String idName   = GraphObject.id.dbName();

	private ConfigurationProvider configuration = null;
	private boolean includeStructr              = false;
	private boolean includeSpatial              = false;
	private boolean includeOther                = false;

	public StructrAndSpatialPredicate(final boolean includeStructrEntities, final boolean includeSpatialEntities, final boolean includeOtherNodes) {

		this.configuration  = Services.getInstance().getConfigurationProvider();
		this.includeStructr = includeStructrEntities;
		this.includeSpatial = includeSpatialEntities;
		this.includeOther   = includeOtherNodes;
	}

	@Override
	public boolean accept(T container) {

		final boolean isStructrEntity = isStructrEntity(container);

		if (container instanceof Node) {

			final boolean isSpatialEntity = container.isSpatialEntity();

			if (isStructrEntity) {
				return includeStructr;
			}

			if (isSpatialEntity) {
				return includeSpatial;
			}

			return includeOther;

		} else if (container instanceof Relationship) {

			final boolean isSpatialEntity = container.isSpatialEntity();

			if (isStructrEntity) {
				return includeStructr;
			}

			if (isSpatialEntity) {
				return includeSpatial;
			}

			return includeOther;
		}

		return true;
	}

	private boolean isStructrEntity(final T container) {

		if (container.hasProperty(idName)) {

			final Object idObject = container.getProperty(idName);
			if (idObject instanceof String) {

				final String id = (String)idObject;

				if (id.length() == 32 && StringUtils.isAlphanumeric(id)) {

					// id is a Structr uuid
					if (container.hasProperty(typeName)) {

						final Object typeObject = container.getProperty(typeName);
						if (typeObject instanceof String) {

							final String type = (String)typeObject;

							// return true if type is an existing node entity
							if (configuration.getNodeEntities().containsKey(type)) {
								return true;
							}

							// return true if type is an existing relationship entity
							if (configuration.getRelationshipEntities().containsKey(type)) {
								return true;
							}
						}
					}
				}
			}
		}

		return false;
	}
}
