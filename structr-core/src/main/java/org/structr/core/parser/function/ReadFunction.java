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
