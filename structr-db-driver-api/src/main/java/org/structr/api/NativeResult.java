package org.structr.api;

import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public interface NativeResult<T> extends AutoCloseable {

	Iterator<T> columnAs(final String name);

	boolean hasNext();

	Map<String, Object> next();
}
