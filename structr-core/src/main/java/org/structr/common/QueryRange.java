package org.structr.common;

import java.util.logging.Logger;
import org.neo4j.helpers.Predicate;

/**
 *
 * @author Christian Morgner
 */
public class QueryRange implements Predicate {

	private static final Logger logger = Logger.getLogger(QueryRange.class.getName());

	private int start = 0;
	private int end   = 0;
	private int count = 0;

	public QueryRange(final int start, final int end) {

		this.start = start;
		this.end   = end;
	}

	// ----- interface Predicate -----
	@Override
	public boolean accept(final Object t) {

		final boolean result = count >= start && count < end;

		count++;

		return result;
	}
}
