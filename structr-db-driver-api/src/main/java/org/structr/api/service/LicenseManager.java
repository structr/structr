/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import java.util.Date;

/**
 */
public interface LicenseManager {

	void logLicenseInfo();

	void refresh();

	String getEdition();

	String getLicensee();
	String getHardwareFingerprint();

	Date getStartDate();
	Date getEndDate();

	int getNumberOfUsers();

	boolean isValid(final Feature feature);
	boolean isValid(final CodeSigner[] codeSigners);

	boolean isModuleLicensed(final String module);
	boolean isClassLicensed(final String fqcn);

	void addLicensedClass(final String fqcn);
}
