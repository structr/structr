/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.csv;

import org.structr.api.service.LicenseManager;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;

import java.util.Set;

/**
 *
 */
public class CSVModule implements StructrModule {

	@Override
	public void onLoad() {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new FromCsvFunction());
		Functions.put(licenseManager, new ToCsvFunction());
		Functions.put(licenseManager, new GetCsvHeadersFunction());
	}

	@Override
	public String getName() {
		return "csv";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui", "api-builder");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
