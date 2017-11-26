/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.function;

import java.io.InputStream;
import org.asciidoctor.internal.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.File;

/**
 * Convenience method to render named nodes. If more than one node is found, an error message is returned that informs the user that this is not allowed and can result in unexpected
 * behavior (instead of including the node).
 */
public class GetContentFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INCLUDE    = "Usage: ${get_content(file)}. Example: ${get_content(first(find('File', 'name', 'test.txt')))}";
	public static final String ERROR_MESSAGE_INCLUDE_JS = "Usage: ${{Structr.getContent(file)}}. Example: ${{Structr.getContent(fileNode)}}";

	@Override
	public String getName() {
		return "include()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (!(arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof File)) {
				return null;
			}

			final File file  = (File)sources[0];
			final InputStream is = file.getInputStream();

			return IOUtils.readFull(is);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_INCLUDE_JS : ERROR_MESSAGE_INCLUDE);
	}

	@Override
	public String shortDescription() {
		return "Returns the content of the given file";
	}

}
