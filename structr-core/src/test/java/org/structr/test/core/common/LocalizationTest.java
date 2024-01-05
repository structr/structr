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
package org.structr.test.core.common;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Localization;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Locale;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Test for the localize() function
 */
public class LocalizationTest extends StructrTest {

	@Test
	public void testLocalizationWithoutDomain() {

		final PropertyKey<String> localizedName = StructrApp.key(Localization.class, "localizedName");
		final PropertyKey<String> domain        = StructrApp.key(Localization.class, "domain");
		final PropertyKey<String> locale        = StructrApp.key(Localization.class, "locale");

		// create
		try (final Tx tx = app.tx()) {

			app.create(Localization.class,
				new NodeAttribute<>(Localization.name, "test1"),
				new NodeAttribute<>(locale, "de"),
				new NodeAttribute<>(localizedName, "test1-de-no_domain")
			);

			app.create(Localization.class,
				new NodeAttribute<>(Localization.name, "test2"),
				new NodeAttribute<>(locale, "de"),
				new NodeAttribute<>(domain, "ExistingDomain"),
				new NodeAttribute<>(localizedName, "test2-de-ExistingDomain")
			);

			app.create(Localization.class,
				new NodeAttribute<>(Localization.name, "test3"),
				new NodeAttribute<>(locale, "de_DE"),
				new NodeAttribute<>(localizedName, "test3-de_DE-no_domain")
			);

			app.create(Localization.class,
				new NodeAttribute<>(Localization.name, "test4"),
				new NodeAttribute<>(locale, "de_DE"),
				new NodeAttribute<>(domain, "ExistingDomain"),
				new NodeAttribute<>(localizedName, "test4-de_DE-ExistingDomain")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			ctx.setLocale(new Locale("de"));

			assertEquals("Invalid localization result", "test1-de-no_domain",      Scripting.evaluate(ctx, null, "${localize('test1', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test1-de-no_domain",      Scripting.evaluate(ctx, null, "${localize('test1', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test1-de-no_domain",      Scripting.evaluate(ctx, null, "${localize('test1')}", "test"));

			assertEquals("Invalid localization result", "test2-de-ExistingDomain", Scripting.evaluate(ctx, null, "${localize('test2', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test2",                   Scripting.evaluate(ctx, null, "${localize('test2', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test2",                   Scripting.evaluate(ctx, null, "${localize('test2')}", "test"));

			assertEquals("Invalid localization result", "test3",                   Scripting.evaluate(ctx, null, "${localize('test3', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test3",                   Scripting.evaluate(ctx, null, "${localize('test3', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test3",                   Scripting.evaluate(ctx, null, "${localize('test3')}", "test"));

			assertEquals("Invalid localization result", "test4",                   Scripting.evaluate(ctx, null, "${localize('test4', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test4",                   Scripting.evaluate(ctx, null, "${localize('test4', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test4",                   Scripting.evaluate(ctx, null, "${localize('test4')}", "test"));

			tx.success();

		} catch (UnlicensedScriptException |FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			ctx.setLocale(new Locale("de", "DE"));

			assertEquals("Invalid localization result", "test1-de-no_domain",         Scripting.evaluate(ctx, null, "${localize('test1', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test1-de-no_domain",         Scripting.evaluate(ctx, null, "${localize('test1', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test1-de-no_domain",         Scripting.evaluate(ctx, null, "${localize('test1')}", "test"));

			assertEquals("Invalid localization result", "test2-de-ExistingDomain",    Scripting.evaluate(ctx, null, "${localize('test2', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test2",                      Scripting.evaluate(ctx, null, "${localize('test2', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test2",                      Scripting.evaluate(ctx, null, "${localize('test2')}", "test"));

			assertEquals("Invalid localization result", "test3-de_DE-no_domain",      Scripting.evaluate(ctx, null, "${localize('test3', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test3-de_DE-no_domain",      Scripting.evaluate(ctx, null, "${localize('test3', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test3-de_DE-no_domain",      Scripting.evaluate(ctx, null, "${localize('test3')}", "test"));

			assertEquals("Invalid localization result", "test4-de_DE-ExistingDomain", Scripting.evaluate(ctx, null, "${localize('test4', 'ExistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test4",                      Scripting.evaluate(ctx, null, "${localize('test4', 'NonexistingDomain')}", "test"));
			assertEquals("Invalid localization result", "test4",                      Scripting.evaluate(ctx, null, "${localize('test4')}", "test"));

			tx.success();

		} catch (UnlicensedScriptException |FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

}
