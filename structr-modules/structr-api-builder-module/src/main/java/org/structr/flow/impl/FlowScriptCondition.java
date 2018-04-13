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
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class FlowScriptCondition extends FlowCondition implements DataSource {

	public static final Property<String> script = new StringProperty("script");

	public static final View defaultView = new View(FlowScriptCondition.class, PropertyView.Public, script);
	public static final View uiView      = new View(FlowScriptCondition.class, PropertyView.Ui,     script);

	@Override
	public Object get(final Context context) {

		final String _script = getProperty(script);
		if (_script != null) {

			try {
				return Scripting.evaluate(context.getActionContext(securityContext), context.getThisObject(), "${" + _script + "}", "FlowDataSource(" + getUuid() + ")");

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return null;
	}
}
