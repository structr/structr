package org.structr.api;

/**
 *
 */
public interface Predicate<T> {

	boolean accept(final T value);
}
