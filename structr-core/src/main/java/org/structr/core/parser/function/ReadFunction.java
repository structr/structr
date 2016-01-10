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
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ReadFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_READ = "Usage: ${read(filename)}. Example: ${read(\"text.xml\")}";

	@Override
	public String getName() {
		return "read()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			try {
				final String sandboxFilename = getSandboxFileName(sources[0].toString());
				if (sandboxFilename != null) {

					final File file = new File(sandboxFilename);
					if (file.exists() && file.length() < 10000000) {

						try (final FileInputStream fis = new FileInputStream(file)) {

							return IOUtils.toString(fis, "utf-8");
						}
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
		return ERROR_MESSAGE_READ;
	}

	@Override
	public String shortDescription() {
		return "Reads and returns the contents of the given file from the exchange directoy";
	}

}
