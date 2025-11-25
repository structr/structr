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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class GetContentFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "get_content";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("file [, encoding ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				final File file = n.as(File.class);

				if (StorageProviderFactory.getStorageProvider(file).size() == 0) {
					return "";
				}

				final String encoding = (sources.length == 2 && sources[1] != null) ? sources[1].toString() : null;

				try (final InputStream is = file.getInputStream()) {

					if (encoding != null) {

						return new Scanner(is, encoding).useDelimiter("\\A").next();
					} else {

						return is.readAllBytes();
					}

				} catch (IOException e) {

					logParameterError(caller, sources, ctx.isJavaScriptContext());
					return usage(ctx.isJavaScriptContext());
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
			Usage.structrScript("Usage: ${get_content(file[, encoding ])}. Example: ${get_content(first(find('File', 'name', 'test.txt')))}"),
			Usage.javaScript("Usage: ${{Structr.getContent(file[, encoding ])}}. Example: ${{Structr.getContent(fileNode)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the content of the given file.";
	}

	@Override
	public String getLongDescription() {
		return """
		Retrieves the content of the given file from the Structr filesystem. This method can be used to access the binary content of a file stored in Structr.
		
		The `encoding` parameter controls the type of the returned data. Without an encoding, the raw data is returned as an array of bytes.
		
		To get the content as a string, you must provide an encoding, e.g. 'UTF-8'.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${get_content(first(find('File', 'name', 'test.txt')))}"),
			Example.javaScript("${{ let bytes = $.getContent($.first($.find('File', 'name', 'test.txt'))) }}"),
			Example.javaScript("${{ let content = $.getContent($.first($.find('File', 'name', 'test.txt')), 'UTF-8') }}")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The parameter `encoding` is available from version 2.3.9+",
			"If you want to access the raw binary content of the file, omit the `encoding` argument.",
			"If you don't provide the `encoding` argument, this function returns a byte array."
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("file", "source file to extract content"),
			Parameter.optional("encoding", "encoding of source data, see notes and description")
		);
	}
}
