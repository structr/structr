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
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.flow.api.DataSource;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.schema.action.ActionContext;
import org.structr.flow.api.Return;
import org.structr.flow.engine.Context;

/**
 *
 */
public class FlowReturn extends FlowNode implements Return {

	public static final Property<DataSource> dataSource = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<String> result = new StringProperty("result");

	public static final View defaultView = new View(FlowReturn.class, PropertyView.Public, result, dataSource);
	public static final View uiView      = new View(FlowReturn.class, PropertyView.Ui,     result, dataSource);

	@Override
	public Object getResult(final Context context) {

		final DataSource ds = getProperty(dataSource);
		final String _script = getProperty(result);

		String script = _script;
		if (script == null) {
			script = "data";
		}

		if (ds != null) {
			context.setData(getUuid(), ds.get(context));
		}

		try {
			return Scripting.evaluate(context.getActionContext(securityContext, this), context.getThisObject(), "${" + script + "}", "FlowReturn(" + getUuid() + ")");

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;

	}
}
