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

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.rels.FlowDataSourceForEach;
import org.structr.flow.rels.FlowDataSourceTransformation;
import org.structr.flow.rels.FlowDecisionCondition;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class FlowDataSource extends FlowBaseNode implements DataSource {

	public static final Property<FlowDecision> isInputFor             = new StartNode<>("isInputFor", FlowDecisionCondition.class);
	public static final Property<List<FlowForEach>> isForEachSource   = new EndNodes<>("isForEachSource", FlowDataSourceForEach.class);
	public static final Property<FlowDataSource> transformationSource = new StartNode<>("transformationSource", FlowDataSourceTransformation.class);
	public static final Property<FlowDataSource> transformationTarget = new EndNode<>("transformationTarget", FlowDataSourceTransformation.class);
	public static final Property<String> query                        = new StringProperty("query");

	public static final View defaultView = new View(FlowDataSource.class, PropertyView.Public, query, isForEachSource, transformationSource, transformationTarget, isInputFor);
	public static final View uiView      = new View(FlowDataSource.class, PropertyView.Ui,     query, isForEachSource, transformationSource, transformationTarget, isInputFor);

	@Override
	public Object get(final Context context) {

		final String _script = getProperty(query);
		if (_script != null) {

			try {
				return Scripting.evaluate(new ActionContext(securityContext), this, "${" + _script + "}", "FlowDataSource(" + getUuid() + ")");

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return null;
	}
}
