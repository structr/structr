/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.api.service;

import java.security.CodeSigner;

/**
 */
public interface LicenseManager {

	public static final int Community              = 0x01; // 0001
	public static final int Basic                  = 0x02; // 0010
	public static final int SmallBusiness          = 0x04; // 0100
	public static final int Enterprise             = 0x08; // 1000

	String getEdition();
	boolean isEdition(final int bitmask);

	String getLicensee();
	String getHardwareFingerprint();

	boolean isValid(final Feature feature);
	boolean isValid(final CodeSigner[] codeSigners);

	boolean isModuleLicensed(final String module);
}
