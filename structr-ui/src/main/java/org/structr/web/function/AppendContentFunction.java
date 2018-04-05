/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.io.FileOutputStream;
import java.io.IOException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.FileBase;

public class AppendContentFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_APPEND_CONTENT    = "Usage: ${append_content(file, content[, encoding = \"UTF-8\"])}. Example: ${append_content(first(find('File', 'name', 'test.txt')), 'additional content')}";
	public static final String ERROR_MESSAGE_APPEND_CONTENT_JS = "Usage: ${{Structr.appendContent(file, content[, encoding = \"UTF-8\"])}}. Example: ${{Structr.appendContent(fileNode, 'additional content')}}";

	@Override
	public String getName() {
		return "append_content()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			if (!(arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof FileBase)) {
				return null;
			}

			final FileBase file   = (FileBase)sources[0];
			final String content  = (String)sources[1];
			final String encoding = (sources.length == 3 && sources[2] != null) ? sources[2].toString() : "UTF-8";

			try (final FileOutputStream fos = file.getOutputStream(true, true)) {

				fos.write(content.getBytes(encoding));

			} catch (IOException ioex) {
				logger.warn("append_content(): Unable to append to file '{}'", file.getProperty(FileBase.path), ioex);
			}

		} catch (IllegalArgumentException iae) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_APPEND_CONTENT_JS : ERROR_MESSAGE_APPEND_CONTENT);
	}

	@Override
	public String shortDescription() {
		return "Appends the content to the given file";
	}
}
