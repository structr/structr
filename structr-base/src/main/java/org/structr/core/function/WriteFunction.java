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
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("fileName, text");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.write(filename, value) }}. Example: ${{ $.write('text.txt', $.this.name) }}"),
			Usage.structrScript("Usage: ${write(filename, value)}. Example: ${write(\"text.txt\", this.name)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Writes to the given file in the exchange directoy";
	}
}
