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
package org.structr.web.traits.definitions.html;

import org.structr.common.PropertyView;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.web.common.HtmlProperty;

import java.util.Map;
import java.util.Set;

public class Option extends GenericHtmlElementTraitDefinition {

	public Option() {
		super("Option");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> valueProperty = new HtmlProperty("value");
		final PropertyKey<String> disabledProperty = new HtmlProperty("disabled");
		final PropertyKey<String> selectedProperty = new HtmlProperty("selected");
		final PropertyKey<String> labelProperty = new HtmlProperty("label");

		final PropertyKey<String> selectedValuesProperty = new StringProperty("selectedValues");

		return newSet(
			valueProperty, disabledProperty, selectedProperty, labelProperty, selectedValuesProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Html,
			newSet(
				"_html_value", "_html_disabled", "_html_selected", "_html_label", "_html_selectedValues"
			)
		);
	}

	/*
	static final GenericProperty valueKey = new GenericProperty("value");
	static final EqualFunction EqualFunction = new EqualFunction();

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Option");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Option"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");


		type.addPropertyGetter("selectedValues", String.class);
	}}

	String getSelectedValues();

	@Override
	void renderManagedAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

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
	*/
}
