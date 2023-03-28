/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AppendContentFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_APPEND_CONTENT    = "Usage: ${append_content(file, content[, encoding = \"UTF-8\"])}. Example: ${append_content(first(find('File', 'name', 'test.txt')), 'additional content')}";
	public static final String ERROR_MESSAGE_APPEND_CONTENT_JS = "Usage: ${{Structr.appendContent(file, content[, encoding = \"UTF-8\"])}}. Example: ${{Structr.appendContent(fileNode, 'additional content')}}";

	@Override
	public String getName() {
		return "append_content";
	}

	@Override
	public String getSignature() {
		return "file, content [, encoding=UTF-8 ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof File) {

				final File file       = (File)sources[0];
				final String encoding = (sources.length == 3 && sources[2] != null) ? sources[2].toString() : "UTF-8";

				if (sources[1] instanceof byte[]) {

					try (final OutputStream fos = file.getOutputStream(true, true)) {

						fos.write((byte[]) sources[1]);

					} catch (IOException ioex) {
						logger.warn("append_content(): Unable to append binary data to file '{}'", file.getPath(), ioex);
					}

				} else {

					final String content = (String)sources[1];

					try (final OutputStream fos = file.getOutputStream(true, true)) {

						fos.write(content.getBytes(encoding));

					} catch (IOException ioex) {
						logger.warn("append_content(): Unable to append to file '{}'", file.getPath(), ioex);
					}
				}
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
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
