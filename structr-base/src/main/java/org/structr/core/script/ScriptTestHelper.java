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
package org.structr.core.script;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 */
public class ScriptTestHelper {

	private static final Logger logger = LoggerFactory.getLogger(ScriptTestHelper.class);

	public static Object testExternalScript(final ActionContext actionContext, final InputStream stream) throws FrameworkException {

		try (final InputStream is = stream) {

			final String script = IOUtils.toString(is, StandardCharsets.UTF_8);
			if (script != null) {

				return Scripting.evaluateScript(actionContext, null, "js", new Snippet("test", script));
			}

		} catch (IOException ioex) {
			logger.error(ExceptionUtils.getStackTrace(ioex));
		}

		return null;
	}

}
