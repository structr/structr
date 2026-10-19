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
package org.structr.test.core.graph;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.structr.common.RequestParameters;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.ToExcelMigrationHandler;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.common.StructrTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * What apply mode actually does to the stored sources: a dry run must leave them exactly as they were, and
 * apply must make the change the dry run described.
 *
 * MailTemplate is the carrier because its text is scanned like any script source and creating one does
 * not rebuild the schema.
 */
public class ToExcelMigrationApplyTest extends StructrTest {

	private static final String OLD_SOURCE = """
		${to_excel(find('User'), 'public')}
		${toExcel(find('User'), ['name', 'id'], true, false, 'excel', 1000, 't')}
		${toExcel(find('User'), 'public', this.withHeader)}
		${{
			const a = $.toExcel($.find('User'), 'public', withHeader);
			const b = $.toExcel($.find('User'), viewOrProperties);
			const c = $.toExcel($.find('User'), { view: 'public' });
		}}""";

	@Test
	public void testApplyRewritesWhatTheDryRunReported() {

		final String id = createTemplate(OLD_SOURCE);

		// the dry run first: it must change nothing at all
		migrate(false);

		assertEquals("a dry run must leave the source untouched", OLD_SOURCE, textOf(id));

		migrate(true);

		assertMigrated(textOf(id));
	}

	@Test
	public void testApplyRewritesInDynamicFiles() {

		String id = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface templateFile = FileHelper.createFile(securityContext, OLD_SOURCE.getBytes(StandardCharsets.UTF_8), "text/plain", StructrTraits.FILE, "export.txt", true);

			templateFile.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY), true);

			id = templateFile.getUuid();

			tx.success();

		} catch (FrameworkException | IOException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// the dry run first: it must change nothing at all
		migrate(false);

		assertEquals("a dry run must leave the source untouched", OLD_SOURCE, textOfDynamicFile(id));

		migrate(true);

		assertMigrated(textOfDynamicFile(id));
	}

	@Test
	public void testApplyIsIdempotent() {

		final String id = createTemplate(OLD_SOURCE);

		migrate(true);

		final String once = textOf(id);

		migrate(true);

		assertEquals("running apply again must change nothing: the rewritten calls are up to date now", once, textOf(id));
	}

	@Test
	public void testSourcesWithoutOldCallsAreLeftAlone() {

		final String current = "${{ $.print($.toExcel($.find('User'), { view: 'public', sheetName: 'Users' })); }}";
		final String id      = createTemplate(current);

		migrate(true);

		assertEquals("a source without old calls must not be touched", current, textOf(id));
	}

	// ----- private methods -----
	private void assertMigrated(final String migrated) {

		assertTrue("a StructrScript view must become { view }: " + migrated, migrated.contains("${toExcel(find('User'), { view: 'public' })}"));

		assertTrue("every positional argument must get its key: " + migrated,
			migrated.contains("${toExcel(find('User'), { properties: ['name', 'id'], includeHeader: true, localizeHeader: false, localizationDomain: 'excel', maxCellLength: 1000, overflowMode: 't' })}"));

		assertTrue("a JavaScript expression must move into the options object: " + migrated, migrated.contains("$.toExcel($.find('User'), { view: 'public', includeHeader: withHeader })"));

		// the MANUAL findings must be left as they were
		assertTrue("a StructrScript expression cannot move into a constant: " + migrated, migrated.contains("${toExcel(find('User'), 'public', this.withHeader)}"));
		assertTrue("an expression in the second position must be left alone: " + migrated, migrated.contains("$.toExcel($.find('User'), viewOrProperties)"));

		// and a call that was already up to date too
		assertTrue("an up to date call must not be touched: " + migrated, migrated.contains("$.toExcel($.find('User'), { view: 'public' })"));
	}

	private void migrate(final boolean apply) {

		try {

			ToExcelMigrationHandler.execute(apply);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while migrating: " + fex.getMessage());
		}
	}

	private String createTemplate(final String text) {

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of(StructrTraits.MAIL_TEMPLATE);
			final NodeInterface node = app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "migration-test"),
				new NodeAttribute<>(traits.key(MailTemplateTraitDefinition.TEXT_PROPERTY), text)
			);

			tx.success();

			return node.getUuid();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating the mail template.");
		}

		return null;
	}

	private String textOf(final String id) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.MAIL_TEMPLATE, id);

			assertNotNull("mail template " + id + " not found", node);

			final PropertyKey<String> key = Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.TEXT_PROPERTY);
			final String text             = node.getProperty(key);

			tx.success();

			return text;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while reading the mail template.");
		}

		return null;
	}

	private String textOfDynamicFile(final String id) {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		securityContext.setRequestParameter(RequestParameters.EditMode.getName(), "1");

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final File file = app.getNodeById(StructrTraits.FILE, id).as(File.class);

			try (final InputStream is = file.getInputStream()) {

				return new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();

			} catch (IOException ioex) {

				return null;

			} finally {

				tx.success();
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while reading the file content.");
		}

		return null;
	}
}
