package org.structr.rest.transform;

import org.structr.rest.transform.relationship.SourceToSink;
import org.structr.rest.transform.relationship.PropertySourceToJoiner;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.EndNode;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;

/**
 *
 */
public class StructrGraphObjectJoiner extends AbstractTransformation implements StructrGraphObjectSource {

	public static final Property<List<StructrPropertySource>> inputs = new StartNodes<>("inputs", PropertySourceToJoiner.class);
	public static final Property<StructrNodeSink> output             = new EndNode<>("output", SourceToSink.class);

	public static final View defaultView = new View(StructrGraphObjectJoiner.class, PropertyView.Public,
		inputs, output
	);

	public static final View uiView = new View(StructrGraphObjectJoiner.class, PropertyView.Ui,
		inputs, output
	);

	static {
		name.unique();
	}

	@Override
	public Iterable<GraphObject> createOutput(final TransformationContext context) throws FrameworkException {

		final List<Iterator<NamedValue>> sources = new LinkedList<>();
		final Map<String, Object> object         = new HashMap<>();
		final List<GraphObject> output           = new LinkedList<>();

		// setup: load iterators from all inputs
		for (final StructrPropertySource source : sort(getProperty(inputs))) {

			final Iterable<NamedValue> msg = source.createOutput(context);
			sources.add(msg.iterator());
		}

		// execution: consume iterators sequentially
		boolean hasData = true;

		while (hasData) {

			final GraphObjectMap entity = new GraphObjectMap();
			object.clear();
			hasData = false;

			for (final Iterator<NamedValue> source : sources) {

				if (source.hasNext()) {

					final NamedValue value = source.next();

					entity.put(new GenericProperty(value.name()), value.value());

					hasData = true;
				}
			}

			if (hasData) {
				output.add(entity);
			}
		}

		return output;
	}

	@Override
	public GraphObject processInput(final SecurityContext securityContext, final Map<String, Object> propertyMap) throws FrameworkException {

		final Iterator<StructrPropertySource> iterator = getProperty(inputs).iterator();
		GraphObject result                      = null;

		while (iterator.hasNext()) {

			final StructrPropertySource source = iterator.next();

			// commit on last iteration
			result = source.processInput(securityContext, propertyMap, !iterator.hasNext());
		}

		return result;
	}

	@Override
	public Class<GraphObject> type() {
		return GraphObject.class;
	}
}
