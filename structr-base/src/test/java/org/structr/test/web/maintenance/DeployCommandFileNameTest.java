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
package org.structr.test.web.maintenance;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.maintenance.DeployCommand;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1603: the deployment export builds the file name of a mail template from the template's own
 * name, and a mail template carries no name validation. A template called ../x therefore wrote
 * outside the folder the export meant to fill, with the rights of the admin who started the export.
 */
public class DeployCommandFileNameTest extends StructrUiTest {

	@Test
	public void testATemplateNameCannotEscapeTheExportFolder() {

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of(StructrTraits.MAIL_TEMPLATE);

			app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "../escaped"),
				new NodeAttribute<>(traits.key(MailTemplateTraitDefinition.LOCALE_PROPERTY), "de"),
				new NodeAttribute<>(traits.key(MailTemplateTraitDefinition.TEXT_PROPERTY),   "text")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating the mail template.");
		}

		Path target = null;

		try {

			target = Files.createTempDirectory("structr-mail-template-export");

			final Map<String, Object> exportParams = new HashMap<>();
			exportParams.put("mode",   "export");
			exportParams.put("target", target.toString());

			app.command(DeployCommand.class).execute(exportParams);

			// ../escaped would have landed here, one level above the folder the export writes to
			assertFalse("the mail template was written outside the mail-templates folder", Files.exists(target.resolve("escaped_-_de.html")));

			final Path mailTemplates = target.resolve("mail-templates");

			assertTrue("the export did not write a mail-templates folder", Files.isDirectory(mailTemplates));

			// and it is in the folder it belongs in, under a name that carries no path any more
			try (final var files = Files.list(mailTemplates)) {

				assertEquals("the mail template was not written into the mail-templates folder", 1L,
					files.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().contains("escaped")).count());
			}

		} catch (FrameworkException | IOException ex) {

			ex.printStackTrace();
			fail("Unexpected exception during export.");

		} finally {

			deleteQuietly(target);
		}
	}

	/**
	 * The name is resolved against the export folder, so what matters is not the exact replacement
	 * but that no name can point outside that folder.
	 */
	@Test
	public void testNoNameResolvesOutsideTheExportFolder() {

		final Path exportFolder = Paths.get("/tmp/export/mail-templates");

		for (final String name : new String[] { "../../etc/cron.d/x", "..", "../..", "/etc/passwd", "..\\..\\x", "./../x" }) {

			final String filename = DeployCommand.sanitizeAndShortenFileOrFolderName(name + "_-_de") + ".html";
			final Path resolved   = exportFolder.resolve(filename).normalize();

			assertTrue("'" + name + "' escaped the export folder as '" + resolved + "'", resolved.startsWith(exportFolder));
		}
	}

	// ----- private methods -----
	private void deleteQuietly(final Path path) {

		if (path == null || !Files.exists(path)) {

			return;
		}

		try (final var paths = Files.walk(path)) {

			paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {

				try { Files.deleteIfExists(p); } catch (IOException ignore) {}
			});

		} catch (IOException ignore) {}
	}
}
