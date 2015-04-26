package org.structr.schema.action;

/**
 *
 * @author Christian Morgner
 */
public class Notification<T> {

	private T payload  = null;
	private String key = null;

	public Notification(final String key) {
		this(key, null);
	}

	public Notification(final String key, final T payload) {

		this.payload = payload;
		this.key     = key;
	}

	public String getKey() {
		return key;
	}

	public T getPayload() {
		return payload;
	}
}
