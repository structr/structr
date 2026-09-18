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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.OutboundHttpCallMigrationHandler;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * What apply mode actually does to the stored sources.
 *
 * The unit tests next door cover the rewriting of a single call. What they cannot show is the property
 * of the two modes that matters operationally: a dry run must leave the database exactly as it was, and
 * apply must make the change the dry run described. Those are assertions about stored text, so they need
 * a node.
 *
 * MailTemplate is the carrier because its text is scanned like any script source and creating one does
 * not rebuild the schema.
 */
public class OutboundHttpCallMigrationApplyTest extends StructrTest {

	private static final String OLD_SOURCE = """
		${{
			const a = $.GET(url, 'application/octet-stream');
			const b = $.GET(downloadUrl, 'text/html', 'title');
			const c = $.POST(url, body, 'application/json', 'UTF-8');
			const d = $.POST(url, body, 'application/json', charset);
		}}""";

	@Test
	public void testApplyRewritesWhatTheDryRunReported() {

		final String id = createTemplate(OLD_SOURCE);

		// the dry run first: it must change nothing at all
		migrate(false);

		assertEquals("a dry run must leave the source untouched", OLD_SOURCE, textOf(id));

		migrate(true);

		final String migrated = textOf(id);

		assertTrue("the octet-stream call must gain binaryResponse: " + migrated, migrated.contains("$.GET(url, 'application/octet-stream', { binaryResponse: true })"));

		assertTrue("the text/html call must gain a selector: " + migrated, migrated.contains("$.GET(downloadUrl, 'text/html', { selector: 'title' })"));

		assertTrue("the charset must be folded into the content type: " + migrated, migrated.contains("$.POST(url, body, 'application/json; charset=UTF-8')"));

		// the call whose charset is an expression was reported MANUAL, so it must be left as it was
		assertTrue("a MANUAL finding must not be rewritten: " + migrated, migrated.contains("$.POST(url, body, 'application/json', charset)"));
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
	public void testSourcesWithoutHttpCallsAreLeftAlone() {

		final String plain = "${{ $.log('nothing to see here'); }}";
		final String id    = createTemplate(plain);

		migrate(true);

		assertEquals("a source without HTTP calls must not be touched", plain, textOf(id));
	}

	// ----- private methods -----
	private void migrate(final boolean apply) {

		try {

			OutboundHttpCallMigrationHandler.execute(apply);

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
}
