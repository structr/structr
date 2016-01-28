package org.structr.rest.transform;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class VirtualType extends AbstractNode {

	public static final Property<List<VirtualProperty>> properties = new EndNodes<>("properties", VirtualTypeProperty.class);
	public static final Property<Integer> position                 = new IntProperty("position").indexed();
	public static final Property<String> sourceType                = new StringProperty("sourceType");

	public static final View defaultView = new View(VirtualType.class, PropertyView.Public,
		name, sourceType, position, properties
	);

	public static final View uiView = new View(VirtualType.class, PropertyView.Ui,
		name, sourceType, position, properties
	);

	public Result transformOutput(final SecurityContext securityContext, final Class sourceType, final Result result) throws FrameworkException {

		final List<VirtualProperty> props    = sort(getProperty(properties));
		final Mapper mapper                  = new Mapper(securityContext, props, entityType);
		final Iterable<GraphObject> iterable = Iterables.map(mapper, result.getResults());

		return new Result(Iterables.toList(iterable), result.getRawResultCount(), result.isCollection(), result.isPrimitiveArray());
	}

	public void transformInput(final SecurityContext securityContext, final Class type, final Map<String, Object> propertySet) throws FrameworkException {

		final ActionContext actionContext = new ActionContext(securityContext);
		final List<VirtualProperty> props = sort(getProperty(properties));

		for (final VirtualProperty property : props) {

			final Transformation transformation = property.getTransformation(type);
			transformation.transformInput(actionContext, propertySet);
		}
	}

	public boolean isPrimitiveArray() {
		return false;
	}

	// ----- private methods -----
	private List<VirtualProperty> sort(final List<VirtualProperty> source) {

		Collections.sort(source, new GraphObjectComparator(VirtualProperty.position, false));

		return source;
	}

	// ----- nested classes -----
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
