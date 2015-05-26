package org.structr.core.graph;

import org.neo4j.helpers.Predicate;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */

public class TypePredicate<T extends GraphObject> implements Predicate<T> {

	private String desiredType = null;

	public TypePredicate(final String desiredType) {
		this.desiredType = desiredType;
	}

	@Override
	public boolean accept(final T arg) {
		return desiredType == null || (arg.getType().equals(desiredType));
	}
}
