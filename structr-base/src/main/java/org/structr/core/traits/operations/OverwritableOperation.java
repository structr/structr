package org.structr.core.traits.operations;

public abstract class OverwritableOperation<T> {

	private T superImplementation = null;

	public final T getSuper() {
		return superImplementation;
	}

	public final void setSuper(final T superImplementation) {
		this.superImplementation = superImplementation;
	}
}
