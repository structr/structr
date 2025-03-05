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

import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowDataInputs;
import org.structr.module.api.DeployableEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowCollectionDataSource extends FlowDataSource implements DeployableEntity {

	public static final Property<Iterable<DataSource>> dataSources = new StartNodes<>("dataSources", FlowDataInputs.class);

	public static final View defaultView = new View(FlowObjectDataSource.class, PropertyView.Public, dataSources);
	public static final View uiView      = new View(FlowObjectDataSource.class, PropertyView.Ui, dataSources);

	@Override
	public Object get(final Context context) throws FlowException {

		List<DataSource> sources = Iterables.toList(getProperty(dataSources));
		List<Object> result      = new ArrayList<>();

		if (sources != null && sources.size() > 0) {

			for (DataSource source : sources) {

				result.add(source.get(context));

			}

		}

		return result;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

}
