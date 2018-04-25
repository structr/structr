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
import org.structr.common.error.FrameworkException;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowDataInput;

import java.util.List;

public class FlowAction extends FlowActionNode implements DataSource {

	public static final Property<DataSource> dataSource = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<List<FlowBaseNode>> dataTarget		= new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<String> script             		= new StringProperty("script");

	public static final View defaultView 							= new View(FlowAction.class, PropertyView.Public, script, dataSource, dataTarget);
	public static final View uiView      							= new View(FlowAction.class, PropertyView.Ui,     script, dataSource, dataTarget);

	@Override
	public void execute(final Context context) {
		final String _script = getProperty(script);
		if (_script != null) {

			try {

				final DataSource _dataSource = getProperty(FlowAction.dataSource);

				// make data available to action if present
				if (_dataSource != null) {
					context.setData(getUuid(), _dataSource.get(context));
				}

				// Evaluate script and write result to context
				Object result = Scripting.evaluate(context.getActionContext(securityContext, this), this, "${" + _script + "}", "FlowAction(" + getUuid() + ")");
				context.setData(getUuid(), result);

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

	}

	@Override
	public Object get(Context context) {
		if (!context.hasData(getUuid())) {
			this.execute(context);
		}
		return context.getData(getUuid());
	}
}
