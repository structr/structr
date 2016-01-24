package org.structr.rest.transform;

import org.structr.rest.transform.relationship.PropertySourceManyToOne;
import org.structr.rest.transform.relationship.PropertySourceOneToOne;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class StructrMergePropertySource extends AbstractTransformation implements StructrPropertySource {

	public static final Property<List<StructrPropertySource>> inputs = new StartNodes<>("inputs", PropertySourceManyToOne.class);
	public static final Property<StructrPropertySource> output       = new EndNode<>("output", PropertySourceOneToOne.class);
	public static final Property<String> mergeFunction        = new StringProperty("mergeFunction");
	public static final Property<String> splitFunction        = new StringProperty("splitFunction");
	public static final Property<String> targetName           = new StringProperty("targetName");

	public static final View defaultView = new View(StructrGraphObjectJoiner.class, PropertyView.Public,
		inputs, output, targetName, mergeFunction, splitFunction
	);

	public static final View uiView = new View(StructrGraphObjectJoiner.class, PropertyView.Ui,
		inputs, output, targetName, mergeFunction, splitFunction
	);

	@Override
	public Iterable<NamedValue> createOutput(final TransformationContext context) throws FrameworkException {

		final String _targetName = getProperty(targetName);
		if (_targetName != null) {

			final String _mergeFunction = getProperty(mergeFunction);
			if (_mergeFunction != null) {

				final ActionContext ctx                  = new ActionContext(context.getSecurityContext());
				final List<Iterator<NamedValue>> sources = new LinkedList<>();
				final List<Object> values                = new LinkedList<>();
				final List<NamedValue> _output           = new LinkedList<>();

				// setup: load iterators from all inputs
				for (final StructrPropertySource source : sort(getProperty(inputs))) {

					final Iterable<NamedValue> msg = source.createOutput(context);
					sources.add(msg.iterator());
				}

				// execution: consume iterators sequentially
				boolean hasData = true;

				while (hasData) {

					values.clear();
					hasData = false;

					for (final Iterator<NamedValue> source : sources) {

						if (source.hasNext()) {

							final Object value = source.next().value();
							if (value != null) {

								values.add(value);
							}

							hasData = true;
						}
					}

					if (hasData) {

						ctx.setConstant("inputs", values);

						_output.add(new NamedValue(_targetName, Scripting.evaluate(ctx, null, "${" + _mergeFunction + "}")));
					}
				}

				return _output;

			} else {

				throw new FrameworkException(500, "Merge property source with ID " + getUuid() + " needs merge function");
			}

		} else {

			throw new FrameworkException(500, "Merge property source with ID " + getUuid() + " needs target property name");
		}
	}

	@Override
	public GraphObject processInput(final SecurityContext securityContext, final Map<String, Object> propertyMap, final boolean commit) throws FrameworkException {


		final String _targetName = getProperty(targetName);
		if (_targetName != null) {

			final String _splitFunction = getProperty(splitFunction);
			if (_splitFunction != null) {

				final ActionContext ctx = new ActionContext(securityContext);
				GraphObject result      = null;
				Iterator iterator       = null;

				ctx.setConstant("input", propertyMap.get(_targetName));
				final Object splitResult = Scripting.evaluate(ctx, null, "${" + _splitFunction + "}");

				// split result already contains named values
				if (splitResult instanceof Map) {
					iterator = ((Map)splitResult).values().iterator();
				}

				if (splitResult instanceof Collection) {
					iterator = ((Collection)splitResult).iterator();
				}

				if (iterator != null) {

					for (final StructrPropertySource source : sort(getProperty(inputs))) {

						if (iterator.hasNext()) {

							// store split value in map
							final String _sourceName = source.getSourceName();
							propertyMap.put(_sourceName, iterator.next());

							// commit flag needs to be extended because we are addressing multiple outputs here
							result = source.processInput(securityContext, propertyMap, commit && !iterator.hasNext());

						} else {

							throw new FrameworkException(500, "Merge property source with ID " + getUuid() + ": property count mismatch");
						}
					}

					return result;

				} else {

					throw new FrameworkException(500, "Merge property source with ID " + getUuid() + ": invalid split function result.");
				}

			} else {

				throw new FrameworkException(500, "Merge property source with ID " + getUuid() + " needs split function");
			}

		} else {

			throw new FrameworkException(500, "Merge property source with ID " + getUuid() + " needs target property name");
		}
	}

	@Override
	public String getSourceName() {
		return null;
	}

	@Override
	public String getTargetName() {
		return getProperty(targetName);
	}
}
