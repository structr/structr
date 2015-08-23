package org.structr.schema.action;

/**
 *
 * @author Christian Morgner
 */
public abstract class Hint {

	private boolean dontModify     = false;
	private boolean isDynamic      = false;

	public abstract String shortDescription();
	public abstract String getSignature();
	public abstract String getName();

	public String getReplacement() {
		return getName();
	}

	public void allowNameModification(final boolean allowModification) {
		this.dontModify = !allowModification;
	}

	public boolean mayModify() {
		return !dontModify;
	}

	public void setIsDynamic(final boolean isDynamic) {
		this.isDynamic = isDynamic;
	}

	public boolean isDynamic() {
		return isDynamic;
	}
}
