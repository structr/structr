/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.auth;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class HashHelper {

	/**
	 * Calculate a SHA-512 hash of the given password string.
	 *
	 * If salt is given, use salt.
	 *
	 * @param password
	 * @param salt
	 * @return hash
	 */
	public static String getHash(final String password, final String salt) {

		if (StringUtils.isEmpty(salt)) {

			return getSimpleHash(password);

		}

		return DigestUtils.sha512Hex(DigestUtils.sha512Hex(password).concat(salt));

	}

	/**
	 * Calculate a SHA-512 hash without salt
	 *
	 * @param password
	 * @return simple hash
	 * @deprecated Use
	 * {@link AuthHelper#getHash(java.lang.String, java.lang.String)}
	 * instead
	 */
	@Deprecated
	public static String getSimpleHash(final String password) {

		return DigestUtils.sha512Hex(password);

	}

}
