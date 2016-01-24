package org.structr.rest.transform;

/**
 *
 * @author Christian Morgner
 */
public class NamedValue {

	private String name  = null;
	private Object value = null;

	public NamedValue(final String name, final Object value) {
		this.name  = name;
		this.value = value;
	}

	@Override
	public String toString() {

		if (value != null) {
			return value.toString();
		}

		return null;
	}

	public String name() {
		return name;
	}

	public Object value() {
		return value;
	}
}
