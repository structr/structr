package org.structr.core.parser.function;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class AppendFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_APPEND = "Usage: ${append(filename, value)}. Example: ${append(\"test.txt\", this.name)}";

	@Override
	public String getName() {
		return "append()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

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
				ioex.printStackTrace();
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_APPEND;
	}

	@Override
	public String shortDescription() {
		return "Appends to the given file in the exchange directoy";
	}

}
