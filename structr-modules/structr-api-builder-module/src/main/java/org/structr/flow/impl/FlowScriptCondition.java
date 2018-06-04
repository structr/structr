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
import org.structr.module.api.DeployableEntity;
import scala.reflect.internal.util.DeprecatedPosition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class FlowScriptCondition extends FlowCondition implements DataSource, DeployableEntity {

	public static final Property<DataSource> dataSource 		= new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<List<FlowBaseNode>> dataTarget = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<String> script 				= new StringProperty("script");

	public static final View defaultView = new View(FlowScriptCondition.class, PropertyView.Public, script);
	public static final View uiView      = new View(FlowScriptCondition.class, PropertyView.Ui,     script);

	@Override
	public Object get(final Context context) {

		Object currentData = context.getData(getUuid());

		if (currentData == null) {
			final DataSource _ds = getProperty(dataSource);
			final String _script = getProperty(script);

			if (_script != null) {

				if (_ds != null) {
					context.setData(getUuid(), _ds.get(context));
				}

				try {

					Object result =  Scripting.evaluate(context.getActionContext(securityContext, this), context.getThisObject(), "${" + _script + "}", "FlowDataSource(" + getUuid() + ")");
					context.setData(getUuid(), result);
					return result;
				} catch (FrameworkException fex) {

					fex.printStackTrace();
				}
			}
		} else {
			return currentData;
		}

		return null;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("script", this.getProperty(script));

		return result;
	}
}
