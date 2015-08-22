package org.structr.schema.action;

/**
 *
 * @author Christian Morgner
 */
public abstract class Hint {

	private boolean dontModify = false;

	public abstract String shortDescription();
	public abstract String getSignature();
	public abstract String getName();

	public String getReplacement() {
		return getName();
	}

	public void preventModification() {
		this.dontModify = true;
	}

	public boolean mayModify() {
		return !dontModify;
	}
}
