/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.util.Iterables;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.rest.common.ResultTransformer;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class VirtualType extends AbstractNode implements ResultTransformer {

	private static final Logger logger = Logger.getLogger(VirtualType.class.getName());

	public static final Property<List<VirtualProperty>> properties = new EndNodes<>("properties", VirtualTypeProperty.class);
	public static final Property<Integer> position                 = new IntProperty("position").indexed();
	public static final Property<String> sourceType                = new StringProperty("sourceType");
	public static final Property<String> filterExpression          = new StringProperty("filterExpression");

	public static final View defaultView = new View(VirtualType.class, PropertyView.Public,
		name, sourceType, position, properties, filterExpression
	);

	public static final View uiView = new View(VirtualType.class, PropertyView.Ui,
		name, sourceType, position, properties, filterExpression
	);

	// ----- interface ResultTransformer -----
	@Override
	public String getSourceType() {
		return getProperty(sourceType);
	}

	@Override
	public Result transformOutput(final SecurityContext securityContext, final Class sourceType, final Result result) throws FrameworkException {

		final List<VirtualProperty> props         = sort(getProperty(properties));
		final Mapper mapper                       = new Mapper(securityContext, props, entityType);
		final Filter filter                       = new Filter(securityContext, getProperty(filterExpression));
		final Iterable<GraphObject> iterable      = Iterables.map(mapper, Iterables.filter(filter, result.getResults()));
		final List<GraphObject> transformedResult = Iterables.toList(iterable);

		return new Result(transformedResult,transformedResult.size(), result.isCollection(), result.isPrimitiveArray());
	}

	@Override
	public void transformInput(final SecurityContext securityContext, final Class type, final Map<String, Object> propertySet) throws FrameworkException {

		final ActionContext actionContext = new ActionContext(securityContext);
		final List<VirtualProperty> props = sort(getProperty(properties));

		for (final VirtualProperty property : props) {

			final Transformation transformation = property.getTransformation(type);
			transformation.transformInput(actionContext, propertySet);
		}
	}

	@Override
	public boolean isPrimitiveArray() {
		return false;
	}

	// ----- private methods -----
	private List<VirtualProperty> sort(final List<VirtualProperty> source) {

		Collections.sort(source, new GraphObjectComparator(VirtualProperty.position, false));

		return source;
	}

	// ----- nested classes -----
	private static class Filter implements Predicate<GraphObject> {

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

					return Boolean.TRUE.equals(Scripting.evaluate(ctx, value, "${" + expression + "}"));

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "", fex);
				}
			}

			return true;
		}
	}

	private static class Mapper implements Function<GraphObject, GraphObject> {

		private final List<Transformation> transformations = new LinkedList<>();
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
				Logger.getLogger(VirtualType.class.getName()).log(Level.SEVERE, null, ex);
			}

			return obj;
		}

	}
}
