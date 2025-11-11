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
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.io.*;
import java.util.List;

public class AppendFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "append";
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

					try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file, true))) {

						for (int i = 1; i < sources.length; i++) {
							IOUtils.write(sources[i].toString(), writer);
						}

						writer.flush();
					}
				}

			} catch (IOException ioex) {

				logException(caller, ioex, sources);
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.append(filename, value)}}. Example: ${{$.append(\"test.txt\", $.this.name)}}"),
			Usage.structrScript("Usage: ${append(filename, value)}. Example: ${append(\"test.txt\", this.name)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Appends to the given file in the exchange directory.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
