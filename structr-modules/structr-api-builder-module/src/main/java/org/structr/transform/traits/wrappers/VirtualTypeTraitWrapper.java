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
package org.structr.transform.traits.wrappers;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.transform.Transformation;
import org.structr.transform.VirtualProperty;
import org.structr.transform.VirtualType;

import java.util.*;
import java.util.function.Function;

public class VirtualTypeTraitWrapper extends AbstractNodeTraitWrapper implements VirtualType {

	public VirtualTypeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public String getSourceType() {
		return wrappedObject.getProperty(traits.key("sourceType"));
	}

	@Override
	public Integer getPosition() {
		return wrappedObject.getProperty(traits.key("position"));
	}

	@Override
	public String getFilterExpression() {
		return wrappedObject.getProperty(traits.key("filterExpression"));
	}

	@Override
	public Iterable<NodeInterface> getVirtualProperties() {
		return wrappedObject.getProperty(traits.key("properties"));
	}

	@Override
	public boolean isPrimitiveArray() {
		return false;
	}

	@Override
	public ResultStream transformOutput(final SecurityContext securityContext, final String sourceType, final ResultStream result) throws FrameworkException {

		final List<NodeInterface> props      = sort(Iterables.toList(getVirtualProperties()));
		final Mapper mapper                  = new Mapper(securityContext, props, getSourceType());
		final Filter filter                  = new Filter(securityContext, getFilterExpression());
		final Iterable<GraphObject> iterable = Iterables.map(mapper, Iterables.filter(filter, result));

		return new PagingIterable("VirtualType.transformOutput()", iterable);
	}

	@Override
	public void transformInput(final SecurityContext securityContext, final String type, final Map<String, Object> propertySet) throws FrameworkException {

		final ActionContext actionContext = new ActionContext(securityContext);
		final List<NodeInterface> props   = sort(Iterables.toList(getVirtualProperties()));
		final Set<String> targetNames     = extractTargetNames(props);

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

		for (final NodeInterface node : props) {

			final VirtualProperty property      = node.as(VirtualProperty.class);

			final Transformation transformation = property.getTransformation(type);
			transformation.transformInput(actionContext, propertySet);
		}
	}

	// ----- private methods -----
	private List<NodeInterface> sort(final List<NodeInterface> source) {

		final Traits traits = Traits.of("VirtualProperty");

		Collections.sort(source, traits.key("position").sorted(false));

		return source;
	}

	private Set<String> extractTargetNames(final List<NodeInterface> source) {

		final Set<String> result = new LinkedHashSet<>();

		for (final NodeInterface node : source) {

			final VirtualProperty prop = node.as(VirtualProperty.class);

			result.add(prop.getTargetName());
		}

		return result;
	}

	// ----- nested classes -----
	private class Filter implements Predicate<GraphObject> {

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

	private class Mapper implements Function<GraphObject, GraphObject> {

		private final List<Transformation> transformations = new LinkedList<>();
		private ActionContext actionContext                = null;

		public Mapper(final SecurityContext securityContext, final List<NodeInterface> properties, final String type) throws FrameworkException {

			this.actionContext = new ActionContext(securityContext);

			for (final NodeInterface node : properties) {

				final VirtualProperty property      = node.as(VirtualProperty.class);
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
