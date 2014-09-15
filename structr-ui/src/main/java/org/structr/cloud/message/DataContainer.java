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
package org.structr.cloud.message;

import java.util.LinkedHashMap;
import java.util.Map;
import org.neo4j.graphdb.PropertyContainer;

/**
 * Abstract superclass of {@link NodeDataContainer} and {@link RelationshipDataContainer}
 *
 * @author axel
 */
public abstract class DataContainer extends Message {

	protected Map<String, Object> properties = new LinkedHashMap<>();
	protected int sequenceNumber             = 0;

	public DataContainer() {}

	public DataContainer(final int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	/**
	 * Return the properties map
	 *
	 * @return
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public Object getPayload() {
		return null;
	}

	// ----- protected methods -----
	protected void collectProperties(final PropertyContainer propertyContainer) {

		for (String key : propertyContainer.getPropertyKeys()) {

			Object value = propertyContainer.getProperty(key);
			properties.put(key, value);
		}
	}
}
