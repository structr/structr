package org.structr.rest.serialization.html;

import org.structr.rest.serialization.html.attr.Context;

/**
 *
 * @author chrisi
 */
public class Attr {
		
	private String key = null;
	private String value = null;

	public Attr(final String key, final String value) {
		this.key = key;
		this.value = value;
	}

	public String format(final Context context) {
		return key + "=\"" + value + "\"";
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
}
