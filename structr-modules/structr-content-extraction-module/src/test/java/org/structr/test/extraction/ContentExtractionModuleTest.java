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
package org.structr.test.extraction;

import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.core.app.StructrApp;
import org.structr.extraction.ContentExtractionModule;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.api.options.OcrMode;
import org.structr.module.StructrModule;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

public class ContentExtractionModuleTest extends StructrUiTest {

	@Test
	public void testModuleIsLoaded() {

		final StructrModule module = StructrApp.getConfiguration().getModules().get(ContentExtractionModule.MODULE_NAME);

		assertNotNull("Content extraction module must be discovered via ServiceLoader", module);
		assertTrue("Module registered under its name must be the content extraction module", module instanceof ContentExtractionModule);
	}

	@Test
	public void testSettingsAreRegistered() {

		final String[] keys = {
			"contentextraction.autoextraction.enabled",
			"contentextraction.extraction.queue",
			"contentextraction.extraction.maxfilesize",
			"contentextraction.extraction.maxcharacters",
			"contentextraction.extraction.includeembedded",
			"contentextraction.split.splitter",
			"contentextraction.split.chunksize",
			"contentextraction.split.chunkoverlap",
			"contentextraction.split.sizeunit",
			"contentextraction.split.tokenencoding",
			"contentextraction.ocr.mode",
			"contentextraction.ocr.languages",
			"contentextraction.ocr.tesseractpath",
			"contentextraction.ocr.tessdatapath",
			"contentextraction.ocr.timeout",
			"contentextraction.ocr.maxpages",
			"contentextraction.ocr.dpi"
		};

		for (final String key : keys) {

			final Setting<?> setting = Settings.getSetting(key);
			assertNotNull("Setting " + key + " must be registered", setting);
		}

		assertEquals("Default chunk size", Integer.valueOf(512), ContentExtractionSettings.ChunkSize.getValue());
		assertEquals("Default OCR mode", "auto", ContentExtractionSettings.OcrMode.getValue());
		assertEquals("Default job queue", "content", ContentExtractionSettings.ExtractionQueue.getValue());
	}
}
