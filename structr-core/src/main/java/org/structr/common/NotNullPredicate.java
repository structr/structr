package org.structr.common;

import org.neo4j.helpers.Predicate;

/**
 *
 * @author Christian Morgner
 */
public class NotNullPredicate implements Predicate {

	@Override
	public boolean accept(Object item) {
		return item != null;
	}
}
