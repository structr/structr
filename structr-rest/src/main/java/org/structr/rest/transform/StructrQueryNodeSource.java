package org.structr.rest.transform;

import org.structr.rest.transform.relationship.NodeSourceToExtractor;
import java.util.Map;
import java.util.function.Function;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class StructrQueryNodeSource extends AbstractNode implements StructrGraphObjectSource, Function<NodeInterface, GraphObject> {

	public static final Property<StructrPropertyExtractor> output = new EndNode<>("output", NodeSourceToExtractor.class);
	public static final Property<String> sourceType               = new StringProperty("sourceType");

	public static final View defaultView = new View(StructrQueryNodeSource.class, PropertyView.Public,
		output, sourceType
	);

	public static final View uiView = new View(StructrQueryNodeSource.class, PropertyView.Ui,
		output, sourceType
	);

	static {
		name.unique();
	}

	@Override
	public Iterable<GraphObject> createOutput(final TransformationContext context) throws FrameworkException {

		final String _sourceType = getProperty(sourceType);
		if (_sourceType != null) {

			final Class<NodeInterface> type = StructrApp.getConfiguration().getNodeEntityClass(getProperty(sourceType));
			if (type != null) {

				final Result<NodeInterface> result = StructrApp.getInstance(context.getSecurityContext())
					.nodeQuery(type)
					.sort(context.getSortKey())
					.page(context.getPage())
					.pageSize(context.getPageSize())
					.order(context.isSortDescending())
					.offsetId(context.getOffsetId())
					.getResult();

				context.setRawResultCount(result.size());

				return Iterables.map(this, result.getResults());

			} else {

				throw new FrameworkException(500, "Unknown source type " + _sourceType + " for node source with ID " + getUuid());
			}

		} else {

			throw new FrameworkException(500, "Invalid source type null for node source with ID " + getUuid());
		}
	}

	@Override
	public GraphObject processInput(final SecurityContext securityContext, final Map<String, Object> propertyMap) throws FrameworkException {

		final String _sourceType = getProperty(sourceType);
		if (_sourceType != null) {

			final Class<NodeInterface> type = StructrApp.getConfiguration().getNodeEntityClass(getProperty(sourceType));
			if (type != null) {

				return StructrApp.getInstance(securityContext).create(type, PropertyMap.inputTypeToJavaType(securityContext, type, propertyMap));

			} else {

				throw new FrameworkException(500, "Unknown source type " + _sourceType + " for node source with ID " + getUuid());
			}

		} else {

			throw new FrameworkException(500, "Invalid source type null for node source with ID " + getUuid());
		}
	}

	@Override
	public Class<GraphObject> type() {
		return StructrApp.getConfiguration().getNodeEntityClass(getProperty(sourceType));
	}

	@Override
	public GraphObject apply(NodeInterface t) {
		return t;
	}
}
