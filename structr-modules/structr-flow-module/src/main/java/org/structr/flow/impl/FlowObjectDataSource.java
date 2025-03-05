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
package org.structr.flow.impl;

/**
 *
 */
public interface FlowObjectDataSource extends FlowDataSource {

	/*

	private static final Logger logger = LoggerFactory.getLogger(FlowObjectDataSource.class);

	public static final Property<Iterable<FlowKeyValue>> keyValueSources = new StartNodes<>("keyValueSources", FlowKeyValueObjectInput.class);

	public static final View defaultView = new View(FlowObjectDataSource.class, PropertyView.Public, keyValueSources);
	public static final View uiView      = new View(FlowObjectDataSource.class, PropertyView.Ui,     keyValueSources);

	@Override
	public Object get(final Context context) throws FlowException {

		final Map<String, Object> result = new LinkedHashMap<>();

		for (final FlowKeyValue _keySource : getProperty(keyValueSources)) {

			final Object item = _keySource.get(context);
			if (item != null && item instanceof KeyValue) {

				final KeyValue keyValue = (KeyValue)item;

				result.put(keyValue.getKey(), keyValue.getValue());

			} else {

				logger.warn("KeyValue source {} of {} returned invalid value {}", _keySource.getUuid(), getUuid(), item);
			}
		}

		return result;
	}
	*/
}
