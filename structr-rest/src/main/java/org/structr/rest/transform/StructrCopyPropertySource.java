package org.structr.rest.transform;

import org.structr.rest.transform.relationship.PropertySourceToJoiner;
import org.structr.rest.transform.relationship.ExtractorToPropertySource;
import java.util.Map;
import java.util.function.Function;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class StructrCopyPropertySource extends AbstractNode implements StructrPropertySource {

	public static final Property<StructrPropertyExtractor> input = new StartNode<>("input", ExtractorToPropertySource.class);
	public static final Property<StructrGraphObjectJoiner> output   = new EndNode<>("output", PropertySourceToJoiner.class);
	public static final Property<String> outputFunction   = new StringProperty("outputFunction");
	public static final Property<String> inputFunction    = new StringProperty("inputFunction");
	public static final Property<String> sourceName       = new StringProperty("sourceName");
	public static final Property<String> targetName       = new StringProperty("targetName");

	public static final View defaultView = new View(StructrCopyPropertySource.class, PropertyView.Public,
		input, output, sourceName, targetName, inputFunction, outputFunction
	);

	public static final View uiView = new View(StructrCopyPropertySource.class, PropertyView.Ui,
		input, output, sourceName, targetName, inputFunction, outputFunction
	);

	@Override
	public Iterable<NamedValue> createOutput(final TransformationContext context) throws FrameworkException {

		final StructrPropertyExtractor extractor = getProperty(input);
		if (extractor != null) {

			final String _sourceName = getProperty(sourceName);
			if (_sourceName != null) {

				String _targetName = getProperty(targetName);
				if (_targetName == null) {

					// no need to transform input, NamedValue can be re-used
					return extractor.createOutput(context, _sourceName);
				}

				// return mapper to transform from source name to target name
				return Iterables.map(new Mapper(context.getSecurityContext(), getProperty(outputFunction), _targetName), extractor.createOutput(context, _sourceName));

			} else {

				throw new FrameworkException(500, "Property extractor with ID " + getUuid() + " needs source name");
			}
		}

		throw new FrameworkException(500, "Property extractor with ID " + getUuid() + " has no source");
	}

	@Override
	public GraphObject processInput(final SecurityContext securityContext, final Map<String, Object> propertyMap, final boolean commit) throws FrameworkException {

		final StructrPropertyExtractor extractor = getProperty(input);
		if (extractor != null) {

			final String _sourceName = getProperty(sourceName);
			if (_sourceName != null) {

				String _targetName = getProperty(targetName);
				if (_targetName != null) {

					final Object value = transformInput(securityContext, propertyMap.remove(_targetName));
					if (value != null) {

						propertyMap.put(_sourceName, value);
					}
				}

				return extractor.processInput(securityContext, propertyMap, commit);

			} else {

				throw new FrameworkException(500, "Property extractor with ID " + getUuid() + " needs source name");
			}
		}

		return null;
	}

	@Override
	public String getSourceName() {
		return getProperty(sourceName);
	}

	@Override
	public String getTargetName() {
		return getProperty(targetName);
	}

	private Object transformInput(final SecurityContext securityContext, final Object input) throws FrameworkException {

		if (input != null) {

			final String _inputFunction = getProperty(inputFunction);
			if (_inputFunction != null) {

				final ActionContext ctx = new ActionContext(securityContext);
				ctx.setConstant("input", input);

				return Scripting.evaluate(ctx, null, "${" + _inputFunction + "}");
			}
		}

		return input;
	}

	private static class Mapper implements Function<NamedValue, NamedValue> {

		private SecurityContext securityContext = null;
		private String outputFunction           = null;
		private String targetName               = null;

		public Mapper(final SecurityContext securityContext, final String outputFunction, final String targetName) {

			this.securityContext = securityContext;
			this.outputFunction  = outputFunction;
			this.targetName      = targetName;
		}

		@Override
		public NamedValue apply(final NamedValue t) {
			return new NamedValue(targetName, transformOutput(t.value()));
		}

		private Object transformOutput(final Object output) {

			if (output != null) {

				if (outputFunction != null) {

					final ActionContext ctx = new ActionContext(securityContext);
					ctx.setConstant("input", output);

					try {
						return Scripting.evaluate(ctx, null, "${" + outputFunction + "}");

					} catch (FrameworkException fex) {
						fex.printStackTrace();
					}
				}
			}

			return output;
		}
	}
}
