package org.structr.common.error;

import org.structr.common.PropertyKey;


/**
 * @author Bastian Knerr
 *
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
