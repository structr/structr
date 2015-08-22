package org.structr.autocomplete;

import org.structr.schema.action.Hint;

/**
 *
 * @author Christian Morgner
 */
public abstract class NonFunctionHint extends Hint {

	private String replacement = null;

	@Override
	public String getReplacement() {

		if (replacement != null) {
			return replacement;
		}

		return getName();
	}

	public void setReplacement(final String replacement) {
		this.replacement = replacement;
	}

	public boolean hasComplexReplacement() {
		return !getName().equals(getReplacement());
	}
}
