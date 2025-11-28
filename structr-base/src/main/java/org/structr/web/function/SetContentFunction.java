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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class SetContentFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "set_content";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("file, content[, encoding ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				final File file       = n.as(File.class);
				final String encoding = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : null;

				if (sources[1] instanceof byte[]) {

					try (final OutputStream fos = file.getOutputStream(true, false)) {

						fos.write((byte[]) sources[1]);

					} catch (IOException ioex) {
						logger.warn("set_content(): Unable to write binary data to file '{}'", file.getPath(), ioex);
					}

				} else if (sources[1] instanceof String) {

					final String content = (String)sources[1];

					try (final OutputStream fos = file.getOutputStream(true, false)) {

						if (encoding != null) {
							fos.write(content.getBytes(encoding));
						} else {
							fos.write(content.getBytes());
						}

					} catch (IOException ioex) {
						logger.warn("set_content(): Unable to write content to file '{}'", file.getPath(), ioex);
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
			Usage.structrScript("Usage: ${set_content(file, content[, encoding ])}."),
			Usage.javaScript("Usage: ${{Structr.setContent(file, content[, encoding ])}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the content of the given file. Content can either be of type String or byte[].";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${set_content(first(find('File', 'name', 'test.txt')), 'New Content Of File test.txt')}", "Simply overwrite file with static content"),
				Example.structrScript("${set_content(create('File', 'name', 'new_document.xlsx'), to_excel(find('User'), 'public'), 'ISO-8859-1')}", "Create new file with Excel content"),
				Example.structrScript("${set_content(create('File', 'name', 'web-data.json'), GET('https://api.example.com/data.json'))}", "Create a new file and retrieve content from URL"),
				Example.structrScript("${set_content(create('File', 'name', 'logo.png'), GET('https://example.com/img/logo.png', 'application/octet-stream'))}", "Download binary data (an image) and store it in a local file"),
				Example.javaScript("${{ $.set_content($.create('File', 'name', 'new_document.xlsx'), $.to_excel($.find('User'), 'public'), 'ISO-8859-1') }}", "Create new file with Excel content (JS version)")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("file", "file node"),
				Parameter.mandatory("content", "content to set"),
				Parameter.optional("encoding", "encoding, default: UTF-8")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The `encoding` parameter is used when writing the data to the file. The default (`UTF-8`) rarely needs to be changed but can be very useful when working with binary strings. For example when using the `to_excel()` function."
		);
	}
}
