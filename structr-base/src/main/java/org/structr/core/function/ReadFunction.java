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
package org.structr.core.function;

import org.apache.commons.io.IOUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class ReadFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_READ = "Usage: ${read(filename)}. Example: ${read(\"text.xml\")}";

	@Override
	public String getName() {
		return "read";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("filename");
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

			logException(ioex, "{}: IOException in element \"{}\" for parameters: {}", new Object[] { getReplacement(), caller, getParametersAsString(sources) });

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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_READ;
	}

	@Override
	public String getShortDescription() {
		return "Reads and returns the contents of the given file from the exchange directoy";
	}
}
