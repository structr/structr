/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.translation;

import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.StringSetting;
import org.structr.api.service.LicenseManager;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;

import java.util.Set;


public class TranslationModule implements StructrModule {

	public static final Setting<String> TranslationGoogleAPIKey      = new StringSetting(Settings.miscGroup,   "Translation Module", "translation.google.apikey", "", "Google Cloud Translation API Key");
	public static final Setting<String> TranslationDeepLAPIKey       = new StringSetting(Settings.miscGroup,   "Translation Module", "translation.deepl.apikey", "", "DeepL API Key");

	@Override
	public void onLoad() {
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new TranslateFunction());
	}

	@Override
	public String getName() {
		return "translation";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
