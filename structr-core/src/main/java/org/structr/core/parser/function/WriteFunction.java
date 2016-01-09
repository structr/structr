/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class WriteFunction extends Function<Object, Object> {

	private static final Logger logger = Logger.getLogger(WriteFunction.class.getName());

	public static final String ERROR_MESSAGE_WRITE = "Usage: ${write(filename, value)}. Example: ${write(\"text.txt\", this.name)}";

	@Override
	public String getName() {
		return "write()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

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

						logger.log(Level.SEVERE, "Trying to overwrite an existing file, please use append() for that purpose.");
					}
				}

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_WRITE;
	}

	@Override
	public String shortDescription() {
		return "Writes to the given file in the exchange directoy";
	}

}
