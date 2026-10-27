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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.function.Functions;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/** A wrong call of login(), e.g. with the null of an unknown user, must not write the password into the server log. */
public class LoginFunctionLogRedactionTest extends StructrTest {

	private static final String PASSWORD = "do-not-log-this-password-4711";

	@Test
	public void testPasswordIsNotLoggedForAnUnknownUser() throws Exception {

		assertPasswordNotLogged("${{ $.login(null, '" + PASSWORD + "') }}");
	}

	@Test
	public void testPasswordIsNotLoggedForAWrongParameterCount() throws Exception {

		assertPasswordNotLogged("${{ $.login('" + PASSWORD + "') }}");
	}

	// ----- private methods -----
	private void assertPasswordNotLogged(final String script) throws Exception {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(Functions.class.getName());
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try (final Tx tx = app.tx()) {

			Scripting.replaceVariables(new ActionContext(SecurityContext.getSuperUserInstance()), null, script, "loginRedactionTest");

			tx.success();

		} finally {

			logger.detachAppender(appender);
		}

		final List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).filter(m -> m.contains("login")).toList();

		assertFalse("the wrong call must be logged", messages.isEmpty());

		for (final String message : messages) {

			assertFalse("the password must not be in the log: " + message, message.contains(PASSWORD));
			assertTrue(message, message.contains("not logged because they are credentials"));
		}
	}
}
