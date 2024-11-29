package org.structr.core.traits;

import java.util.LinkedList;
import java.util.List;

public class Hierarchy<T> {

	private final List<T> hierarchy = new LinkedList<>();

	public T get() {

		return hierarchy.get(0);
	}

	public void add(final T t) {
		hierarchy.add(t);
	}


}
