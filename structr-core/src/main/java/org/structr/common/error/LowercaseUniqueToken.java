package org.structr.common.error;

import org.structr.core.property.PropertyKey;

/**
 * Indicates that a given property value is already present in the database and
 * may only occur once.
 * 
 * @author Bastian Knerr
 */
public class LowercaseUniqueToken extends UniqueToken {

	public static final String ERROR_TOKEN = "already_taken_lowercase";

	public LowercaseUniqueToken(final String id, final PropertyKey propKey, final String val) {
		super(id, propKey, val);
	}

	@Override
	public String getErrorToken() {
		return ERROR_TOKEN;
	}

}
