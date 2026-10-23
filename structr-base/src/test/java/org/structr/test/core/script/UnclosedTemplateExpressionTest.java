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
package org.structr.test.core.script;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/** A template expression that never ends used to be skipped without a word: it is still skipped, but logged once. */
public class UnclosedTemplateExpressionTest extends StructrTest {

	@Test
	public void testUnclosedExpressionIsLoggedOnce() throws Exception {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(Scripting.class.getName());
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		final String source                        = "<main>${'one'}|${{ $.log(\"unclosed); }}</main>";

		appender.start();
		logger.addAppender(appender);

		try (final Tx tx = app.tx()) {

			// rendered twice, logged once
			for (int i = 0; i < 2; i++) {

				final String result = Scripting.replaceVariables(new ActionContext(securityContext), null, source, "unclosedTemplateTest");

				assertEquals("the closed block is still evaluated, the unclosed one stays as it is", "<main>one|${{ $.log(\"unclosed); }}</main>", result);
			}

			tx.success();

		} finally {

			logger.detachAppender(appender);
		}

		final List<String> warnings = appender.list.stream().filter(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("is never closed")).map(ILoggingEvent::getFormattedMessage).toList();

		assertEquals(warnings.toString(), 1, warnings.size());
		assertTrue(warnings.get(0), warnings.get(0).contains("unclosedTemplateTest"));
		assertTrue(warnings.get(0), warnings.get(0).contains("inside a double-quoted string"));
		assertTrue(warnings.get(0), warnings.get(0).contains("${{ $.log(\"unclosed); }}"));
	}
}
