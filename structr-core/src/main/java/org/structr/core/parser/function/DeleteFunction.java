package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class DeleteFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_DELETE = "Usage: ${delete(entity)}. Example: ${delete(this)}";

	@Override
	public String getName() {
		return "delete()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final App app = StructrApp.getInstance(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
			for (final Object obj : sources) {

				if (obj instanceof NodeInterface) {

					app.delete((NodeInterface)obj);
					continue;
				}

				if (obj instanceof RelationshipInterface) {

					app.delete((RelationshipInterface)obj);
					continue;
				}
			}
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_DELETE;
	}

	@Override
	public String shortDescription() {
		return "Deletes the given entity from the database";
	}
}
