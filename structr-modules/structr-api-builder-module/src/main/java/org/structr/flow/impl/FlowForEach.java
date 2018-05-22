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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.ForEach;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowForEachBody;
import org.structr.flow.impl.rels.FlowForEachDataInput;

/**
 *
 */
public class FlowForEach extends FlowNode implements ForEach, DataSource {

	public static final Property<DataSource> dataSource   = new StartNode<>("dataSource", FlowForEachDataInput.class);
	public static final Property<FlowNode> loopBody           = new EndNode<>("loopBody", FlowForEachBody.class);

	public static final View defaultView = new View(FlowForEach.class, PropertyView.Public, dataSource, loopBody, isStartNodeOfContainer);
	public static final View uiView      = new View(FlowForEach.class, PropertyView.Ui,     dataSource, loopBody, isStartNodeOfContainer);


	@Override
	public DataSource getDataSource() {
		return getProperty(dataSource);
	}

	@Override
	public FlowElement getLoopBody() {
		return getProperty(loopBody);
	}

	@Override
	public Object get(Context context) {
		return context.getData(getUuid());
	}

}
