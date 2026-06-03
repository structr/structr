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
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SetContentFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "setContent";
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
						logger.warn("setContent(): Unable to write binary data to file '{}'", file.getPath(), ioex);
					}

				} else if (sources[1] instanceof InputStream is) {

					try (final OutputStream fos = file.getOutputStream(true, false)) {

						IOUtils.copy(is, fos);

					} catch (IOException ioex) {
						logger.warn("setContent(): Unable to stream content to file '{}'", file.getPath(), ioex);
					} finally {
						try { is.close(); } catch (IOException ignore) {}
					}

				} else if (sources[1] instanceof String content) {

					try (final OutputStream fos = file.getOutputStream(true, false)) {

						if (encoding != null) {
							fos.write(content.getBytes(encoding));
						} else {
							fos.write(content.getBytes());
						}

					} catch (IOException ioex) {
						logger.warn("setContent(): Unable to write content to file '{}'", file.getPath(), ioex);
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${setContent(file, content[, encoding ])}."),
			Usage.javaScript("Usage: ${{$.setContent(file, content[, encoding ])}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the content of the given file. Content can be of type String, byte[] or InputStream.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("""
					${
						setContent(
							first(find('File', 'name', 'test.txt')),
							'New Content Of File test.txt'
						)
					}
					""", "Simply overwrite file with static content"),
				Example.structrScript("""
					${
						setContent(
							create('File', 'name', 'new_document.xlsx'),
							toExcel(find('User'), 'public'),
							'ISO-8859-1'
						)
					}
					""", "Create new file with Excel content"),
				Example.structrScript("""
					${
						setContent(
							create('File', 'name', 'web-data.json'),
							GET('https://api.example.com/data.json').body
						)
					}
					""", "Create a new file and retrieve content from URL"),
				Example.structrScript("""
					${
						setContent(
							create('Image', 'name', 'logo.png'),
							GET('https://example.com/img/logo.png', 'application/octet-stream').body
						)
					}
					""", "Download binary data (an image) and store it in a local file"),
				Example.javaScript("""
					${{
						$.setContent(
							$.create('File', 'name', 'new_document.xlsx'),
							$.toExcel($.find('User'), 'public'),
							'ISO-8859-1'
						);
					}}
					""", "Create new file with Excel content (JS version)")
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
				"If `content` is an InputStream (via $.GET), the stream is consumed and can not be used again afterwards",
				"The `encoding` parameter is only used when writing **string** data to the file and ignored otherwise. The default (`UTF-8`) rarely needs to be changed but can be very useful when working with binary strings. For example when using the `toExcel()` function."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}
}
