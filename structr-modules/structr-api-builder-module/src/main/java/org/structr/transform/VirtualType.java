/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.transform;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.LinkedList;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.PropertyView;
import org.structr.common.ResultTransformer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public interface VirtualType extends NodeInterface, ResultTransformer {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("VirtualType");
		final JsonObjectType prop = schema.addType("VirtualProperty");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/VirtualType"));

		type.addStringProperty("filterExpression", PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("sourceType",       PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("position",        PropertyView.Public, PropertyView.Ui).setIndexed(true);

		type.addPropertyGetter("filterExpression", String.class);
		type.addPropertyGetter("sourceType",       String.class);
		type.addPropertyGetter("position",         Integer.class);

		type.overrideMethod("isPrimitiveArray",     false, "return false;");
		type.overrideMethod("getVirtualProperties", false, "return getProperty(propertiesProperty);");
		type.overrideMethod("transformOutput",      false, "return " + VirtualType.class.getName() + ".transformOutput(this, arg0, arg1, arg2);");
		type.overrideMethod("transformInput",       false, VirtualType.class.getName() + ".transformInput(this, arg0, arg1, arg2);");

		type.relate(prop, "virtualProperty", Cardinality.OneToMany, "virtualType", "properties").setCascadingCreate(JsonSchema.Cascade.sourceToTarget);

		// view configuration
		type.addViewProperty(PropertyView.Public, "name");
		type.addViewProperty(PropertyView.Public, "properties");

		type.addViewProperty(PropertyView.Ui, "properties");
	}}

	Integer getPosition();
	String getFilterExpression();
	Iterable<VirtualProperty> getVirtualProperties();

	public static ResultStream transformOutput(final VirtualType thisType, final SecurityContext securityContext, final Class sourceType, final ResultStream result) throws FrameworkException {

		final List<VirtualProperty> props         = VirtualType.sort(Iterables.toList(thisType.getVirtualProperties()));
		final Mapper mapper                       = new Mapper(securityContext, props, StructrApp.getConfiguration().getNodeEntityClass(thisType.getSourceType()));
		final Filter filter                       = new Filter(securityContext, thisType.getFilterExpression());
		final Iterable<GraphObject> iterable      = Iterables.map(mapper, Iterables.filter(filter, result));

		return new PagingIterable("VirtualType.transformOutput()", iterable);
	}

	public static void transformInput(final VirtualType thisType, final SecurityContext securityContext, final Class type, final Map<String, Object> propertySet) throws FrameworkException {

		final ActionContext actionContext = new ActionContext(securityContext);
		final List<VirtualProperty> props = VirtualType.sort(Iterables.toList(thisType.getVirtualProperties()));
		final Set<String> targetNames     = VirtualType.extractTargetNames(props);

		// remove all properties for which no VirtualProperty exists
		final Iterator<String> it = propertySet.keySet().iterator();
		while (it.hasNext()) {

			final String propertyName = it.next();

			if (!targetNames.contains(propertyName)) {
				final Logger logger = LoggerFactory.getLogger(VirtualType.class);
				logger.debug("Removing property '{}' with value '{}' from propertyset because no matching virtual property was found", propertyName, propertySet.get(propertyName));
				it.remove();
			}
		};

		for (final VirtualProperty property : props) {

			final Transformation transformation = property.getTransformation(type);
			transformation.transformInput(actionContext, propertySet);
		}
	}

	// ----- private methods -----
	static List<VirtualProperty> sort(final List<VirtualProperty> source) {

		Collections.sort(source, StructrApp.key(VirtualProperty.class, "position").sorted(false));

		return source;
	}

	static Set<String> extractTargetNames(final List<VirtualProperty> source) {

		final Set<String> result = new LinkedHashSet<>();

		for (final VirtualProperty prop : source) {
			result.add(prop.getTargetName());
		}

		return result;
	}

	// ----- nested classes -----
	static class Filter implements Predicate<GraphObject> {

		private ActionContext ctx = null;
		private String expression = null;

		public Filter(final SecurityContext securityContext, final String expression) {

			this.ctx        = new ActionContext(securityContext);
			this.expression = expression;
		}

		@Override
		public boolean accept(final GraphObject value) {

			if (StringUtils.isNotBlank(expression)) {

				try {

					return Boolean.TRUE.equals(Scripting.evaluate(ctx, value, "${" + expression.trim() + "}", "virtual type filter", null));

				} catch (FrameworkException fex) {
					final Logger logger = LoggerFactory.getLogger(VirtualType.class);
					logger.warn("", fex);
				}
			}

			return true;
		}
	}

	static class Mapper implements Function<GraphObject, GraphObject> {

		private final List<Transformation> transformations = new ArrayList<>();
		private ActionContext actionContext                = null;

		public Mapper(final SecurityContext securityContext, final List<VirtualProperty> properties, final Class type) throws FrameworkException {

			this.actionContext = new ActionContext(securityContext);

			for (final VirtualProperty property : properties) {

				final Transformation transformation = property.getTransformation(type);
				if (transformation != null) {

					this.transformations.add(transformation);
				}
			}
		}

		@Override
		public GraphObject apply(final GraphObject source) {

			final GraphObjectMap obj = new GraphObjectMap();

			try {

				for (final Transformation transformation : transformations) {

					final PropertyKey targetProperty = transformation.getTargetProperty();

					obj.put(targetProperty, transformation.transformOutput(actionContext, source));
				}

			} catch (FrameworkException ex) {
				final Logger logger = LoggerFactory.getLogger(VirtualType.class);
				logger.error("", ex);
			}

			return obj;
		}

	}
}
