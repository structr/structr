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
package org.structr.flow;

import org.structr.flow.rels.FlowInput;
import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.flow.engine.Context;

/**
 *
 */
public class FlowEquals extends FlowDataSource {

	public static final Property<List<FlowDataSource>> sources = new StartNodes<>("sources", FlowInput.class);

	public static final View defaultView = new View(FlowEquals.class, PropertyView.Public, sources);
	public static final View uiView      = new View(FlowEquals.class, PropertyView.Ui,     sources);

	@Override
	public Boolean get(final Context context) {

		Object previousValue = null;

		for (final FlowDataSource source : getProperty(sources)) {

			final Object value = source.get(context);

			// compare previous value and current value
			if (previousValue != null && !previousValue.equals(value)) {
				return false;
			}

			previousValue = value;
		}

		return true;
	}
}
