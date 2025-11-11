/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class AppendContentFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "append_content";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("file, content [, encoding=UTF-8 ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				final File file       = n.as(File.class);
				final String encoding = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : "UTF-8";

				if (sources[1] instanceof byte[]) {

					try (final OutputStream fos = file.getOutputStream(true, true)) {

						fos.write((byte[]) sources[1]);

					} catch (IOException ioex) {
						logger.warn("append_content(): Unable to append binary data to file '{}'", file.getPath(), ioex);
					}

				} else if (sources[1] instanceof String) {

					final String content = (String)sources[1];

					try (final OutputStream fos = file.getOutputStream(true, true)) {

						fos.write(content.getBytes(encoding));

					} catch (IOException ioex) {
						logger.warn("append_content(): Unable to append to file '{}'", file.getPath(), ioex);
					}

				} else {

					throw new FrameworkException(422, getName() + "(): Content must be of type String or byte[]. Found: " + sources[1].getClass().getSimpleName());
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${append_content(file, content[, encoding = 'UTF-8'])}. Example: ${append_content(first(find('File', 'name', 'test.txt')), 'additional content')}"),
			Usage.javaScript("Usage: ${{ $.appendContent(file, content[, encoding = 'UTF-8']) }}. Example: ${{ $.appendContent(fileNode, 'additional content') }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Appends the content to the given file. Content can either be of type String or byte[].";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
