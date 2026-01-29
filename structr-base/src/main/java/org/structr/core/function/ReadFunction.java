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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class ReadFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "read";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String sandboxFilename = getSandboxFileName(sources[0].toString());
			if (sandboxFilename != null) {

				final File file = new File(sandboxFilename);
				if (file.exists()) {

					try (final FileInputStream fis = new FileInputStream(file)) {

						return IOUtils.toString(fis, "utf-8");
					}
				}
			}

		} catch (IOException ioex) {

			logException(ioex, "{}: IOException in element \"{}\" for parameters: {}", new Object[] { getDisplayName(), caller, getParametersAsString(sources) });

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String getShortDescription() {
		return "Reads text from a file in the `exchange/` folder.";
	}

	@Override
	public String getLongDescription() {
		return """
			This function reads text from the file with the given file name in the exchange/ folder. If the file does not exist, nothing will be returned, but no error will be thrown.
			
			To prevent data leaks, Structr allows very limited access to the underlying file system. The only way to read or write files on the harddisk is to use files in the exchange/ folder of the Structr runtime directory. All calls to `read()`, `write()` and `append()` will check that before reading from or writing to the disk.";
			""";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("filename");
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("filename", "name of the file to read from")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${read('test.txt')}", "Read from a file named 'test.txt' in the exchange/ folder."),
			Example.javaScript("${{ $.read('test.txt'); }}", "Read from a file named 'test.txt' in the exchange/ folder.")
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
			Usage.structrScript("Usage: ${read(filename)}. Example: ${read('test.txt')}"),
			Usage.javaScript("Usage: ${{ $.read(filename); }}. Example: ${{ $.read('test.txt'); }}")
		);
	}
}
