package org.structr.rest.transform;

import org.structr.rest.transform.relationship.ExtractorToPropertySource;
import org.structr.rest.transform.relationship.NodeSourceToExtractor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;

/**
 *
 */
public class StructrPropertyExtractor extends AbstractNode {

	public static final Property<StructrGraphObjectSource> input      = new StartNode<>("input", NodeSourceToExtractor.class);
	public static final Property<List<StructrPropertySource>> outputs = new EndNodes<>("outputs", ExtractorToPropertySource.class);

	public static final View defaultView = new View(StructrPropertyExtractor.class, PropertyView.Public,
		input, outputs
	);

	public static final View uiView = new View(StructrPropertyExtractor.class, PropertyView.Ui,
		input, outputs
	);

	public Iterable<NamedValue> createOutput(final TransformationContext context, final String name) throws FrameworkException {

		final StructrGraphObjectSource source = getProperty(input);
		if (source != null) {

			final Class<GraphObject> type = source.type();
			final PropertyKey key         = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, name);

			return Iterables.map(new Extractor(key), source.createOutput(context));

		} else {

			throw new FrameworkException(500, "Property extractor " + getUuid() + " has no source");
		}
	}

	public GraphObject processInput(final SecurityContext securityContext, final Map<String, Object> value, final boolean commit) throws FrameworkException {

		// this method will be called as many times as there are input streams
		if (commit) {

			final StructrGraphObjectSource source = getProperty(input);
			if (source != null) {

				return source.processInput(securityContext, value);

			} else {

				throw new FrameworkException(500, "Property extractor " + getUuid() + " has no source");
			}
		}

		return null;
	}

	private static class Extractor implements Function<GraphObject, NamedValue> {

		private PropertyKey key = null;

		public Extractor(final PropertyKey key) {
			this.key = key;
		}

		@Override
		public NamedValue apply(final GraphObject t) {
			return new NamedValue(key.jsonName(), t.getProperty(key));
		}
	}
}
