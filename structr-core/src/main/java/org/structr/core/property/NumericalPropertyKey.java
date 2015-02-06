package org.structr.core.property;

/**
 * Marker interface to support conversion to and from Double which
 * is the default exchange type in ECMA script.
 *
 * @author Christian Morgner
 */
public interface NumericalPropertyKey<T> {
	public T convertToNumber(final Double source);
}
