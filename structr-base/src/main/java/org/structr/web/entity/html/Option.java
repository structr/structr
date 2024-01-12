/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.EqualFunction;
import org.structr.core.property.GenericProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.SchemaService;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;

import java.net.URI;
import java.util.List;

public interface Option extends DOMElement {

	static final GenericProperty valueKey = new GenericProperty("value");
	static final EqualFunction EqualFunction = new EqualFunction();

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Option");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Option"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");

		type.addStringProperty("selectedValues", PropertyView.Ui);

		type.addStringProperty("_html_value",	 PropertyView.Html);
		type.addStringProperty("_html_disabled", PropertyView.Html);
		type.addStringProperty("_html_selected", PropertyView.Html);
		type.addStringProperty("_html_label",    PropertyView.Html);

		type.addPropertyGetter("selectedValues", String.class);
	}}

	String getSelectedValues();

	@Override
	default void renderManagedAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

		try {

			// make sure we are inside a repeater
			final String dataKey = getDataKey();
			if (dataKey != null) {

				// make sure the unmanaged "selected" attribute is not set
				final String originalSelected = getProperty("_html_selected");
				if (StringUtils.isEmpty(originalSelected)) {

					// fetch selectedValues expression
					final String selectedValuesExpression = getSelectedValues();
					if (selectedValuesExpression != null) {

						// evaluate selectedValues expression
						final java.lang.Object selectedValues = Scripting.evaluate(renderContext, this, "${" + selectedValuesExpression.trim() + "}", selectedValuesExpression, this.getUuid());
						if (selectedValues != null) {

							// fetch value of current data key
							final GraphObject currentValue = renderContext.getDataNode(dataKey);
							boolean found                  = false;

							if (selectedValues instanceof Iterable) {

								// Iterable, Collection, List etc.
								final List list = Iterables.toList((Iterable)selectedValues);
								found = list.contains(currentValue);

							} else if (selectedValues.getClass().isArray()) {

								// Array
								final Object[] array = (Object[])selectedValues;
								for (final Object o : array) {

									if (o.equals(currentValue)) {

										found = true;
										break;
									}
								}

							} else {

								if (currentValue instanceof GraphObjectMap) {

									final GraphObjectMap map = (GraphObjectMap)currentValue;
									if (map.size() == 1 && map.containsKey(valueKey)) {

										final java.lang.Object value = map.get(valueKey);

										found = EqualFunction.valueEquals(selectedValues, value);
									}

								} else {

									// single object, compare directly
									found = EqualFunction.valueEquals(selectedValues, currentValue);
								}
							}

							if (found) {

								out.append(" selected");
							}
						}
					}
				}
			}
		} catch (final Throwable t) {

			final Logger logger = LoggerFactory.getLogger(Content.class);
			DOMNode.logScriptingError(logger, t, "Error while evaluating script in Option[{}]", this.getUuid());
		}
	}

}
