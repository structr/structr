package org.structr.files.cmis;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractStructrCmisService {

	protected SecurityContext securityContext = null;

	public AbstractStructrCmisService(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	protected void log(final Logger logger, final Object... objects) {

		final StringBuilder buf = new StringBuilder();

		for (int i=0; i<objects.length; i++) {
			buf.append("\n{").append(i).append("}");
		}

		logger.log(Level.INFO, buf.toString(), objects);
	}
}
