package org.structr.rest.transform;

import org.structr.rest.transform.relationship.SourceToSink;
import java.util.List;
import java.util.Map;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;

/**
 *
 * @author Christian Morgner
 */
public class StructrNodeSink extends AbstractNode {

	public static final Property<StructrGraphObjectSource> input = new StartNode<>("input", SourceToSink.class);
	public static final Property<Integer> position               = new IntProperty("position").indexed();

	public static final View defaultView = new View(StructrNodeSink.class, PropertyView.Public,
		name, input, position
	);

	public static final View uiView = new View(StructrNodeSink.class, PropertyView.Ui,
		name, input, position
	);

	static {

		name.unique();
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		return validate(securityContext, errorBuffer) && super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		return validate(securityContext, errorBuffer) && super.onModification(securityContext, errorBuffer);
	}

	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		final StructrGraphObjectSource source = getProperty(input);
		if (source != null) {

			final TransformationContext context = new TransformationContext(securityContext, sortKey, pageSize, page, sortDescending, offsetId);
			final Iterable<GraphObject> nodes  = source.createOutput(context);
			final List<GraphObject> resultList = Iterables.toList(nodes);

			return new Result(resultList, context.getRawResultCount(), true, isPrimitiveArray());
		}

		throw new FrameworkException(500, "Node sink with name " + getName() + " has no sources");
	}

	public GraphObject doPost(final Map<String, Object> propertySet) throws FrameworkException {

		final StructrGraphObjectSource source = getProperty(input);
		if (source != null) {

			return source.processInput(securityContext, propertySet);
		}

		return null;
	}

	public boolean isPrimitiveArray() {
		return false;
	}

	// ----- private methods -----
	private boolean validate(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = true;

		valid &= !ValidationHelper.checkStringNotBlank(this, name, errorBuffer);
		valid &= !ValidationHelper.checkPropertyUniquenessError(this, name, errorBuffer);

		return valid;
	}
}
