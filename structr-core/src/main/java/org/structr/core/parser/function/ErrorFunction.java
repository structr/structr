package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ErrorFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ERROR = "Usage: ${error(...)}. Example: ${error(\"base\", \"must_equal\", int(5))}";

	@Override
	public String getName() {
		return "error()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final Class entityType;
		final String type;

		if (entity != null) {

			entityType = entity.getClass();
			type = entity.getType();

		} else {

			entityType = GraphObject.class;
			type = "Base";

		}

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, sources[0].toString());
			ctx.raiseError(422, new SemanticErrorToken(type, key, sources[1].toString()));

		} else if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

			final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, sources[0].toString());
			ctx.raiseError(422, new SemanticErrorToken(type, key, sources[1].toString(), sources[2]));
		}

		return null;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ERROR;
	}

	@Override
	public String shortDescription() {
		return "Signals an error to the caller";
	}

}
