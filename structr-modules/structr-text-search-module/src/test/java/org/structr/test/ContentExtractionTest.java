/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.text.model.StructuredDocument;
import org.structr.text.model.StructuredTextNode;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.testng.AssertJUnit.fail;

/**
 *
 */
public class ContentExtractionTest extends StructrUiTest {

	@Test
	public void testContentExtraction() {

		try (final Tx tx = app.tx()) {

			try (final InputStream is = FulltextIndexingTest.class.getResourceAsStream("/test/test.pdf")) {

				final File file = FileHelper.createFile(securityContext, is, "", File.class, "test.pdf");

				StructrApp.getInstance(securityContext).getContentAnalyzer().analyzeContent(file);

				// check result
				final StructuredDocument document = app.nodeQuery("StructuredDocument").getFirst();

				for (final StructuredTextNode node : document.getAllChildNodes()) {

					System.out.println("Node: " + node.getContent());
				}
			}

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		// wait for indexing to finish
		try { Thread.sleep(1000); } catch (Throwable t) {}
	}
}

