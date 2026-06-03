/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.io.IOUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AppendContentFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "appendContent";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				final File file       = n.as(File.class);
				final String encoding = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : null;

				if (sources[1] instanceof byte[]) {

					try (final OutputStream fos = file.getOutputStream(true, true)) {

						fos.write((byte[]) sources[1]);

					} catch (IOException ioex) {
						logger.warn("appendContent(): Unable to append binary data to file '{}'", file.getPath(), ioex);
					}

				} else if (sources[1] instanceof InputStream is) {

					try (final OutputStream fos = file.getOutputStream(true, true)) {

						IOUtils.copy(is, fos);

					} catch (IOException ioex) {
						logger.warn("appendContent(): Unable to stream content to file '{}'", file.getPath(), ioex);
					} finally {
						try { is.close(); } catch (IOException ignore) {}
					}

				} else if (sources[1] instanceof String) {

					final String content = (String)sources[1];

					try (final OutputStream fos = file.getOutputStream(true, true)) {

						if (encoding != null) {
							fos.write(content.getBytes(encoding));
						} else {
							fos.write(content.getBytes());
						}

					} catch (IOException ioex) {
						logger.warn("appendContent(): Unable to append to file '{}'", file.getPath(), ioex);
					}

				} else {

					return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getName() + "(): Content must be of type String, byte[] or InputStream. Found: " + sources[1].getClass().getSimpleName());
				}

			} else {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getName() + "(): First parameter must be a File. Found: " + sources[0]);
			}

		} catch (ArgumentNullException ane) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getName() + "(): " + ane.getMessage() + " - Parameters: " + getParametersAsString(sources));

		} catch (ArgumentCountException ace) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getName() + "(): " + ace.getMessage() + " - Parameters: " + getParametersAsString(sources));
		}

		return null;
	}

	@Override
	public String getShortDescription() {
		return "Appends content to the given file. Content can be of type String, byte[] or InputStream.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("file, content [, encoding ]");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${appendContent(file, content[, encoding ])}. Example: ${appendContent(first(find('File', 'name', 'test.txt')), 'additional content')}"),
			Usage.javaScript("Usage: ${{ $.appendContent(file, content[, encoding ]) }}. Example: ${{ $.appendContent(fileNode, 'additional content') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("file", "Structr File entity to append the content to"),
			Parameter.mandatory("content", "content to append"),
			Parameter.optional("encoding", "encoding to use, e.g. 'UTF-8'")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("appendContent(first(find('File', 'name', 'test.txt')), '\\nAdditional Content')", "Append the string '\\nAdditional Content' to the file with the name 'test.txt'.")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"If `content` is an InputStream (via $.GET), the stream is consumed and can not be used again afterwards",
				"The `encoding` parameter is only used when writing string the data to the file. By default the input is not encoded, but when given an encoding such as `UTF-8` the content is transformed before being written to the file."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}
}
