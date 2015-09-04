package org.structr.files.cmis;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christian Morgner
 */
public class AbstractStructrCmisService {

	protected void log(final Logger logger, final Object... objects) {

		final StringBuilder buf = new StringBuilder();

		for (int i=0; i<objects.length; i++) {
			buf.append("\n{").append(i).append("}");
		}

		logger.log(Level.INFO, buf.toString(), objects);
	}
}
