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
