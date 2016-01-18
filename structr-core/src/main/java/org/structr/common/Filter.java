package org.structr.common;

import org.structr.api.Predicate;

/**
 *
 * @author Christian Morgner
 */
public interface Filter<T> extends Predicate<T> {

	public void setSecurityContext(final SecurityContext securityContext);
}
