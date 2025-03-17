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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.EqualFunction;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.operations.RenderManagedAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Option extends GenericHtmlElementTraitDefinition {

	public static final String VALUE_PROPERTY          = getPrefixedHTMLAttributeName("value");
	public static final String DISABLED_PROPERTY       = getPrefixedHTMLAttributeName("disabled");
	public static final String SELECTED_PROPERTY       = getPrefixedHTMLAttributeName("selected");
	public static final String LABEL_PROPERTY          = getPrefixedHTMLAttributeName("label");
	public static final String SELECTEDVALUES_PROPERTY = "selectedValues";

	public Option() {
		super(StructrTraits.OPTION);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> valueProperty          = new StringProperty(VALUE_PROPERTY);
		final PropertyKey<String> disabledProperty       = new StringProperty(DISABLED_PROPERTY);
		final PropertyKey<String> selectedProperty       = new StringProperty(SELECTED_PROPERTY);
		final PropertyKey<String> labelProperty          = new StringProperty(LABEL_PROPERTY);

		final PropertyKey<String> selectedValuesProperty = new StringProperty(SELECTEDVALUES_PROPERTY);

		return newSet(
			valueProperty, disabledProperty, selectedProperty, labelProperty, selectedValuesProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
					SELECTEDVALUES_PROPERTY
			),
			PropertyView.Html,
			newSet(
					VALUE_PROPERTY, DISABLED_PROPERTY, SELECTED_PROPERTY, LABEL_PROPERTY, SELECTEDVALUES_PROPERTY
			)
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		final Map<Class, FrameworkMethod> frameworkMethods = super.getFrameworkMethods();

		frameworkMethods.put(
			RenderManagedAttributes.class,
			new RenderManagedAttributes() {

				@Override
				public void renderManagedAttributes(final NodeInterface node, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {
					Option.renderManagedAttributes(node.as(DOMNode.class), out, securityContext, renderContext);
				}
			}
		);

		return frameworkMethods;
	}

	/*
	static final GenericProperty valueKey = new GenericProperty("value");

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Option");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Option"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");


		type.addPropertyGetter(SELECTEDVALUES_PROPERTY, String.class);
	}}

	String getSelectedValues();
	*/


	static final EqualFunction EqualFunction = new EqualFunction();

	static void renderManagedAttributes(final DOMNode node, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

		try {

			// make sure we are inside a repeater
			final String dataKey = node.getDataKey();
			if (dataKey != null) {

				// make sure the unmanaged "selected" attribute is not set
				final Traits traits                = node.getTraits();
				final PropertyKey<String> valueKey = new StringProperty("value");
				final String originalSelected      = node.getProperty(traits.key(SELECTED_PROPERTY));

				if (StringUtils.isEmpty(originalSelected)) {

					// fetch selectedValues expression
					final String selectedValuesExpression = node.getProperty(traits.key(SELECTEDVALUES_PROPERTY));
					if (selectedValuesExpression != null) {

						// evaluate selectedValues expression
						final java.lang.Object selectedValues = Scripting.evaluate(renderContext, node, "${" + selectedValuesExpression.trim() + "}", selectedValuesExpression, node.getUuid());
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

			final Logger logger = LoggerFactory.getLogger(Option.class);
			DOMNode.logScriptingError(logger, t, "Error while evaluating script in Option[{}]", node.getUuid());
		}
	}
}
