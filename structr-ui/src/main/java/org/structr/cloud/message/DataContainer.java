/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.cloud.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.core.graph.SyncCommand;

/**
 * Abstract superclass of {@link NodeDataContainer} and {@link RelationshipDataContainer}
 *
 *
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
	 * @return properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	// ----- protected methods -----
	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {

		this.sequenceNumber      = (Integer)SyncCommand.deserialize(inputStream);
		final int num            = (Integer)SyncCommand.deserialize(inputStream);

		for (int i=0; i<num; i++) {

			final String key   = (String)SyncCommand.deserialize(inputStream);
			final Object value = SyncCommand.deserialize(inputStream);

			properties.put(key, value);
		}
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {

		SyncCommand.serialize(outputStream, sequenceNumber);
		SyncCommand.serialize(outputStream, properties.size());

		for (final Entry<String, Object> entry : properties.entrySet()) {

			SyncCommand.serialize(outputStream, entry.getKey());
			SyncCommand.serialize(outputStream, entry.getValue());
		}
	}

	protected void collectProperties(final PropertyContainer propertyContainer, final Iterable<String> propertyKeys) {

		for (String key : propertyKeys) {

			Object value = propertyContainer.getProperty(key);
			if (value != null) {

				properties.put(key, value);
			}
		}
	}

}
