/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.web.entity.model;

import java.net.URI;
import java.util.Set;
import org.structr.schema.SchemaService;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.ModelDataSource;

public interface SelectModel extends ListModel {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("SelectModel");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/SelectModel"));
		type.setExtends(schema.getType("ListModel"));
		type.setCategory("html");

		type.addPropertyGetter("selectedOptionsExpression", String.class);
		type.addPropertyGetter("currentOptionExpression", String.class);

		type.addStringProperty("selectedOptionsExpression");
		type.addStringProperty("currentOptionExpression");

		// override "evaluate" method and redirect it to isSelected if the isSelected keyword is evaluated
		type.overrideMethod("evaluate", false, "if (\"isSelected\".equals(arg1)) { return " + SelectModel.class.getName() + ".isSelected(this, arg0, arg1); } else { return super.evaluate(arg0, arg1, arg2, arg3, arg4, arg5); }");
	}}

	String getSelectedOptionsExpression();
	String getCurrentOptionExpression();

	default Object getSelectedData(final RenderContext renderContext, final NodeInterface referenceNode) throws FrameworkException {

		final String selectedExpression = getSelectedOptionsExpression();
		if (selectedExpression == null) {

			return null;
		}

		return Scripting.evaluate(renderContext, referenceNode, "${" + selectedExpression.trim() + "}", "listExpression", getUuid());
	}

	public static Object isSelected(final SelectModel thisSelectModel, final ActionContext actionContext, final String key) {

		try {

			final String currentOptionExpression = thisSelectModel.getCurrentOptionExpression();
			if (currentOptionExpression != null) {

				final Object current = actionContext.evaluate(thisSelectModel, currentOptionExpression, null, null, 0, new EvaluationHints(), 1, 1);
				if (current != null && current instanceof GraphObject) {

					final Object selectedData = thisSelectModel.getSelectedData((RenderContext)actionContext, thisSelectModel);
					if (selectedData != null) {

						final Set<GraphObject> selected = Iterables.toSet(ModelDataSource.wrap(selectedData));
						if (selected.contains((GraphObject)current)) {

							return "selected";
						}

					} else {

						// log missing list expression of ListModel
					}

				} else {

					// log missing current object
				}

			} else {

				// log missing selected option expression
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}
}