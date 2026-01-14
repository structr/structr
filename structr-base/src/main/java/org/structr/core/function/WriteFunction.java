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
package org.structr.core.function;

import org.apache.commons.io.IOUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.io.*;
import java.util.List;

public class WriteFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "write";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			try {

				final String sandboxFilename = getSandboxFileName(sources[0].toString());
				if (sandboxFilename != null) {

					final File file = new File(sandboxFilename);
					if (!file.exists()) {

						try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file, false))) {

							for (int i = 1; i < sources.length; i++) {
								if (sources[i] != null) {
									IOUtils.write(sources[i].toString(), writer);
								}
							}

							writer.flush();
						}

					} else {

						logger.error("Trying to overwrite an existing file, please use append() for that purpose.");
					}
				}

			} catch (IOException ioex) {

				logException(caller, ioex, sources);
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String getShortDescription() {
		return "Writes text to a new file in the `exchange/` folder.";
	}

	@Override
	public String getLongDescription() {
		return """
			This function writes the given text to the file with the given file name in the exchange/ folder. If the file already exist, an error will be thrown.
			
			To prevent data leaks, Structr allows very limited access to the underlying file system. The only way to read or write files on the harddisk is to use files in the exchange/ folder of the Structr runtime directory. All calls to `read()`, `write()` and `append()` will check that before reading from or writing to the disk.";
			""";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("filename, text");
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("filename", "name of the file to write to"),
			Parameter.optional("text", "text to write")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${write('test.txt', 'hello world')}", "Write 'hello world' to a new file named 'test.txt' in the exchange/ folder."),
			Example.javaScript("${{ $.write('test.txt', 'hello world'); }}", "Write 'hello world' to a new file named 'test.txt' in the exchange/ folder.")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The `exchange/` folder itself may be a symbolic link.",
			"The canonical path of a file has to be identical to the provided filepath in order to prevent directory traversal attacks. This means that symbolic links inside the `exchange/` folder are forbidden",
			"Absolute paths and relative paths that traverse out of the exchange/ folder are forbidden.",
			"Allowed 'sub/dir/file.txt'",
			"Forbidden '../../../../etc/passwd'",
			"Forbidden '/etc/passwd'"
		);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${write(filename, text)}. Example: ${write('test.txt', this.name)}"),
			Usage.javaScript("Usage: ${{$.write(filename, text)}}. Example: ${{ $.write('test.txt', $.this.name); }}")
		);
	}
}
