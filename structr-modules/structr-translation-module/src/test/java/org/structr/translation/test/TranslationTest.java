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
package org.structr.translation.test;

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.translation.TranslateFunction;
import org.structr.translation.TranslationModule;
import org.testng.annotations.Test;
import org.structr.core.graph.*;
import org.structr.core.script.Scripting;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class TranslationTest extends StructrUiTest {


    @Test
    public void testWrongTranslationProvider() {

		try (final Tx tx = this.app.tx()) {

            final ActionContext ctx = new ActionContext(securityContext);
			Scripting.replaceVariables(ctx, null, "${{ $.translate('Hello World!', 'en', 'de', 'wrongProvider');}}");
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals(TranslateFunction.ERROR_UNKNOWN_PROVIDER, message);
        }
    }

    @Test
    public void testGoogleTranslationNotConfigured() {

		try (final Tx tx = this.app.tx()) {

            final ActionContext ctx = new ActionContext(securityContext);
            Scripting.replaceVariables(ctx, null, "${{ $.translate('Hello World!', 'en', 'de'); }}");
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals(TranslateFunction.ERROR_NO_GOOGLE_API_KEY, message);
        }
    }

    @Test
    public void testGoogleTranslationEmptyApiKey() {

        TranslationModule.TranslationGoogleAPIKey.setValue("");
        try (final Tx tx = this.app.tx()) {

            final ActionContext ctx = new ActionContext(securityContext);
            Scripting.replaceVariables(ctx, null, "${{ $.translate('Hello World!', 'en', 'de'); }}");
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals(TranslateFunction.ERROR_NO_GOOGLE_API_KEY, message);
        }
    }

    @Test
    public void testDeeplTranslationNotConfigured() {

        try (final Tx tx = this.app.tx()) {

            final ActionContext ctx = new ActionContext(securityContext);
			Scripting.replaceVariables(ctx, null, "${{ $.translate('Hello World!', 'en', 'de', 'deepl'); }}");
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals(TranslateFunction.ERROR_NO_DEEPL_API_KEY, message);
        }
    }

    @Test
    public void testDeeplTranslationEmptyApiKey() {

        TranslationModule.TranslationDeepLAPIKey.setValue("");
        try (final Tx tx = this.app.tx()) {

            final ActionContext ctx = new ActionContext(securityContext);
			Scripting.replaceVariables(ctx, null, "${{ $.translate('Hello World!', 'en', 'de', 'deepl'); }}");
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals(TranslateFunction.ERROR_NO_DEEPL_API_KEY, message);
        }
    }

    @Test
    public void testGoogleTranslationBadAPIKey() {

        final String sourceTranslation = "Hello World!";
		// Checking only first part of message, no API part!
		final String expectedMessage = String.format(TranslateFunction.ERROR_GOOGLE_FAILED, sourceTranslation, "");

        TranslationModule.TranslationGoogleAPIKey.setValue("badConfigKey!");

        try (final Tx tx = this.app.tx()) {

            final ActionContext ctx = new ActionContext(securityContext);
            Scripting.replaceVariables(ctx, null, "${{ $.translate('" + sourceTranslation + "', 'en', 'de', 'google'); }}");
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
			assertTrue(message.startsWith(expectedMessage));

        }
    }

    @Test
    public void testDeeplTranslationBadAPIKey() {

        final String sourceTranslation = "Hello World!";
        // Checking only first part of message, no API part!
        final String expectedMessage = String.format(TranslateFunction.ERROR_DEEPL_FAILED, sourceTranslation, "");

        TranslationModule.TranslationDeepLAPIKey.setValue("badConfigKey!");

        try (final Tx tx = this.app.tx()) {

            final ActionContext ctx = new ActionContext(securityContext);
			Scripting.replaceVariables(ctx, null, "${{ $.translate('" + sourceTranslation + "', 'en', 'de', 'deepl'); }}");
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertTrue(message.startsWith(expectedMessage));
        }
    }

}
