package org.structr.core.entity;

import java.util.List;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.relationship.Transform;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class Transformation extends AbstractNode {

	public static final Property<List<Transformation>> inputs  = new StartNodes<>("inputs", Transform.class);
	public static final Property<List<Transformation>> outputs = new EndNodes<>("outputs", Transform.class);
	public static final Property<String> out                   = new StringProperty("out");
	public static final Property<String> in                    = new StringProperty("in");
	public static final Property<Object> result                = new TransformProperty("result");

	public static final View defaultView = new View(Transformation.class, PropertyView.Public,
		inputs, outputs, in, out, result
	);

	public static final View uiView = new View(Transformation.class, PropertyView.Ui,
		inputs, outputs, in, out, result
	);

	private static class TransformProperty extends AbstractPrimitiveProperty<Object> {

		public TransformProperty(final String name) {
			super(name);
		}

		@Override
		public Object getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

			try {

				final String source = obj.getProperty(out);
				if (source != null) {

					return Scripting.evaluate(new ActionContext(securityContext), obj, "${" + source + "}");
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}

			return null;
		}

		@Override
		public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final Object value) throws FrameworkException {

			try {

				final String source = obj.getProperty(in);
				if (source != null) {

					return Scripting.evaluate(new ActionContext(securityContext), obj, "${" + source + "}");
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}

			return null;
		}

		@Override
		public Object fixDatabaseProperty(Object value) {
			return value;
		}

		@Override
		public String typeName() {
			return "Object";
		}

		@Override
		public Class valueType() {
			return Object.class;
		}

		@Override
		public PropertyConverter<Object, ?> databaseConverter(SecurityContext securityContext) {
			return null;
		}

		@Override
		public PropertyConverter<Object, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
			return null;
		}

		@Override
		public PropertyConverter<?, Object> inputConverter(SecurityContext securityContext) {
			return null;
		}

		@Override
		public SortType getSortType() {
			return SortType.Default;
		}
	}
}
