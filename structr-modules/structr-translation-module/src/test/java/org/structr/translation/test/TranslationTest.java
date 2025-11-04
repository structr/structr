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

import org.structr.api.config.Setting;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.translation.TranslateFunction;
import org.structr.translation.TranslationModule;
import org.testng.annotations.Test;
import org.structr.core.graph.*;
import org.structr.core.script.Scripting;
import org.structr.api.config.StringSetting;
import org.structr.api.config.Settings;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class TranslationTest extends StructrUiTest {


    @Test
    public void testWrongTranslationProvider()
    {
        try (final Tx tx = this.app.tx()) {

            final StringBuilder func = new StringBuilder();
            final ActionContext ctx = new ActionContext(securityContext);

            func.append("${{\n");
            func.append(" Structr.translate('Hello World!', 'en', 'de', 'wrongProvider');");
            func.append("}}");

            final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals("Unknown translation provider - possible values are 'google' and 'deepl'.", message);
        }
    }

    @Test
    public void testGoogleTranslationNotConfigured()
    {
        try (final Tx tx = this.app.tx()) {

            final StringBuilder func = new StringBuilder();
            final ActionContext ctx = new ActionContext(securityContext);

            func.append("${{\n");
            func.append(" Structr.translate('Hello World!', 'en', 'de');");
            func.append("}}");

            final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals("Google Cloud Translation API Key not configured in structr.conf", message);
        }
    }

    @Test
    public void testGoogleTranslationEmptyApiKey()
    {
        TranslationModule.TranslationGoogleAPIKey.setValue("");
        try (final Tx tx = this.app.tx()) {

            final StringBuilder func = new StringBuilder();
            final ActionContext ctx = new ActionContext(securityContext);

            func.append("${{\n");
            func.append(" Structr.translate('Hello World!', 'en', 'de');");
            func.append("}}");

            final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals("Google Cloud Translation API Key not configured in structr.conf", message);
        }
    }

    @Test
    public void testDeeplTranslationNotConfigured()
    {
        try (final Tx tx = this.app.tx()) {

            final StringBuilder func = new StringBuilder();
            final ActionContext ctx = new ActionContext(securityContext);

            func.append("${{\n");
            func.append(" Structr.translate('Hello World!', 'en', 'de', 'deepl');");
            func.append("}}");

            final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals("DeepL Translation API Key not configured in structr.conf", message);
        }
    }

    @Test
    public void testDeeplTranslationEmptyApiKey()
    {
        TranslationModule.TranslationDeepLAPIKey.setValue("");

        try (final Tx tx = this.app.tx()) {

            final StringBuilder func = new StringBuilder();
            final ActionContext ctx = new ActionContext(securityContext);

            func.append("${{\n");
            func.append(" Structr.translate('Hello World!', 'en', 'de', 'deepl');");
            func.append("}}");

            final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals("DeepL Translation API Key not configured in structr.conf", message);
        }
    }

    @Test
    public void testGoogleTranslationBadAPIKey()
    {
        final String sourceTranslation = "Hello World!";
        final String expectedMessage = "Could not translate text: "+sourceTranslation+" -- Google API Response: API key not valid. Please pass a valid API key.";

        TranslationModule.TranslationGoogleAPIKey.setValue("badConfigKey!");

        try (final Tx tx = this.app.tx()) {

            final StringBuilder func = new StringBuilder();
            final ActionContext ctx = new ActionContext(securityContext);

            func.append("${{\n");
            func.append(" Structr.translate('");
            func.append(sourceTranslation);
            func.append("', 'en', 'de', 'google');");
            func.append("}}");

            final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            assertEquals(expectedMessage, message);
        }
    }

    @Test
    public void testDeeplTranslationBadAPIKey()
    {

        final String sourceTranslation = "Hello World!";
        // Checking only first part of message, no API part!
        final String expectedMessage = "Could not translate text: "+sourceTranslation+" --  Deepl API Response:";

        TranslationModule.TranslationDeepLAPIKey.setValue("badConfigKey!");

        try (final Tx tx = this.app.tx()) {

            final StringBuilder func = new StringBuilder();
            final ActionContext ctx = new ActionContext(securityContext);

            func.append("${{\n");
            func.append(" Structr.translate('");
            func.append(sourceTranslation);
            func.append("', 'en', 'de', 'deepl');");
            func.append("}}");

            final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());
            tx.success();

        } catch (FrameworkException fex) {

            final String message = fex.getMessage();
            final boolean exceptionCheck = message.startsWith(expectedMessage);
            assertEquals(true, exceptionCheck);
        }
    }

}
