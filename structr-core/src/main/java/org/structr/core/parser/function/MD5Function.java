package org.structr.core.parser.function;

import org.apache.commons.codec.digest.DigestUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class MD5Function extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MD5 = "Usage: ${md5(string)}. Example: ${md5(this.email)}";

	@Override
	public String getName() {
		return "md5()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? DigestUtils.md5Hex(sources[0].toString())
			: "";

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MD5;
	}

	@Override
	public String shortDescription() {
		return "Returns the MD5 hash of its parameter";
	}

}
