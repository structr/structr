/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.flow.api.KeyValue;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowKeySource;
import org.structr.flow.impl.rels.FlowValueSource;

/**
 *
 */
public class FlowKeyValue extends FlowDataSource {

	private static final Logger logger = LoggerFactory.getLogger(FlowKeyValue.class);

	public static final Property<FlowDataSource> keySource   = new StartNode<>("keySource",   FlowKeySource.class);
	public static final Property<FlowDataSource> valueSource = new StartNode<>("valueSource", FlowValueSource.class);

	public static final View defaultView = new View(FlowKeyValue.class, PropertyView.Public, keySource, valueSource);
	public static final View uiView      = new View(FlowKeyValue.class, PropertyView.Ui,     keySource, valueSource);

	@Override
	public Object get(final Context context) {

		final FlowDataSource _keySource   = getProperty(keySource);
		final FlowDataSource _valueSource = getProperty(valueSource);

		if (_keySource != null && _valueSource != null) {

			final Object key = _keySource.get(context);
			if (key != null) {

				final Object value = _valueSource.get(context);
				if (value != null) {

					return new KeyValue(key.toString(), value);

				} else {

					logger.warn("Value source of {} returned no data", getUuid());
				}

			} else {

				logger.warn("Key source of {} returned no data", getUuid());
			}

		} else {

			logger.warn("Unable to evaluate FlowKeyValue {}, missing at least one source.", getUuid());
		}

		return null;
	}
}
