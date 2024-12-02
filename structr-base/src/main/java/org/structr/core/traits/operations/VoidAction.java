package org.structr.core.traits.operations;

public abstract class VoidAction {

	protected final Runnable function;

	public VoidAction(Runnable function) {
		this.function = function;
	}
}
