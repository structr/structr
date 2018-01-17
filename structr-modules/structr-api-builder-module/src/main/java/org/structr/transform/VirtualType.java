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
package org.structr.transform;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.util.Iterables;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.ResultTransformer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

/**
 *
 */
public interface VirtualType extends NodeInterface, ResultTransformer {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("VirtualType");
		final JsonObjectType prop = schema.addType("VirtualProperty");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/VirtualType"));

		type.addStringProperty("filterExpression", PropertyView.Public);
		type.addStringProperty("sourceType",       PropertyView.Public);
		type.addIntegerProperty("position",        PropertyView.Public).setIndexed(true);

		type.addPropertyGetter("filterExpression", String.class);
		type.addPropertyGetter("sourceType",       String.class);
		type.addPropertyGetter("position",         Integer.class);

		type.overrideMethod("isPrimitiveArray",     false, "return false;");
		type.overrideMethod("getVirtualProperties", false, "return getProperty(propertiesProperty);");
		type.overrideMethod("transformOutput",      false, "return " + VirtualType.class.getName() + ".transformOutput(this, arg0, arg1, arg2);");
		type.overrideMethod("transformInput",       false, VirtualType.class.getName() + ".transformInput(this, arg0, arg1, arg2);");

		type.relate(prop, "virtualProperty", Relation.Cardinality.OneToMany, "virtualType", "properties").setCascadingCreate(JsonSchema.Cascade.sourceToTarget);


		/*
	public Result transformOutput(final SecurityContext securityContext, final Class sourceType, final Result result) throws FrameworkException {
	public void transformInput(final SecurityContext securityContext, final Class type, final Map<String, Object> propertySet) throws FrameworkException {
		*/

	}}

	Integer getPosition();
	String getFilterExpression();
	List<VirtualProperty> getVirtualProperties();

	/*

	private static final Logger logger = LoggerFactory.getLogger(VirtualType.class.getName());

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
	*/

	public static Result transformOutput(final VirtualType thisType, final SecurityContext securityContext, final Class sourceType, final Result result) throws FrameworkException {

		final List<VirtualProperty> props         = VirtualType.sort(thisType.getVirtualProperties());
		final Mapper mapper                       = new Mapper(securityContext, props, thisType.getClass());
		final Filter filter                       = new Filter(securityContext, thisType.getFilterExpression());
		final Iterable<GraphObject> iterable      = Iterables.map(mapper, Iterables.filter(filter, result.getResults()));
		final List<GraphObject> transformedResult = Iterables.toList(iterable);

		return new Result(transformedResult,transformedResult.size(), result.isCollection(), result.isPrimitiveArray());
	}

	public static void transformInput(final VirtualType thisType, final SecurityContext securityContext, final Class type, final Map<String, Object> propertySet) throws FrameworkException {

		final ActionContext actionContext = new ActionContext(securityContext);
		final List<VirtualProperty> props = VirtualType.sort(thisType.getVirtualProperties());
		final Set<String> targetNames     = VirtualType.extractTargetNames(props);

		// remove all properties for which no VirtualProperty exists
		final Iterator<String> it = propertySet.keySet().iterator();
		while (it.hasNext()) {

			final String propertyName = it.next();

			if (!targetNames.contains(propertyName)) {
				logger.debug("Removing property '{}' with value '{}' from propertyset because no matching virtual property was found", propertyName, propertySet.get(propertyName));
				it.remove();
			}
		};

		for (final VirtualProperty property : props) {

			final Transformation transformation = property.getTransformation(type);
			transformation.transformInput(actionContext, propertySet);
		}
	}

	/*
	@Override
	public boolean isPrimitiveArray() {
		return false;
	}
	*/

	// ----- private methods -----
	static List<VirtualProperty> sort(final List<VirtualProperty> source) {

		Collections.sort(source, new GraphObjectComparator(StructrApp.key(VirtualProperty.class, "position"), false));

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

					return Boolean.TRUE.equals(Scripting.evaluate(ctx, value, "${" + expression + "}", "virtual type filter"));

				} catch (FrameworkException fex) {
					logger.warn("", fex);
				}
			}

			return true;
		}
	}

	static class Mapper implements Function<GraphObject, GraphObject> {

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
				logger.error("", ex);
			}

			return obj;
		}

	}
}
