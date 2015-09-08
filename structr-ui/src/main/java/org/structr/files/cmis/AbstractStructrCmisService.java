package org.structr.files.cmis;

import java.math.BigInteger;
import java.util.List;
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

	protected <T> List<T> applyPaging(final List<T> source, final BigInteger maxItems, final BigInteger skipCount) {

		final int size = source.size();
		int skip       = 0;
		int from       = 0;
		int to         = size;

		if (skipCount != null) {
			skip = skipCount.intValue();
		}

		if (maxItems != null) {
			to = Math.min(maxItems.intValue(), size);
		}

		from = Math.min(skip, size);
		to   = Math.min(to+skip, size);

		return source.subList(from, to);
	}
}
