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

import org.structr.core.function.ExecBinaryFunction;
import org.structr.core.function.ExecFunction;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/** A failed exec() call, e.g. one interrupted by a restart, logs its parameters: a masked one must stay masked there. */
public class ExecFunctionLogMaskingTest {

	private static final String SECRET = "do-not-log-this-secret-4711";

	@Test
	public void testMaskedParameterStaysMaskedInErrorLogs() {

		final Object[] sources = { "my.script", List.of("plain", Map.of("value", SECRET, "mask", true), Map.of("value", "visible", "mask", false)), 2 };

		for (final String log : List.of(new ExecProbe().log(sources), new ExecBinaryProbe().log(sources))) {

			assertFalse(log, log.contains(SECRET));
			assertEquals("[my.script, [plain, ***, visible], 2]", log);
		}
	}

	private static class ExecProbe extends ExecFunction {

		String log(final Object[] sources) {

			return parametersForLog(sources);
		}
	}

	private static class ExecBinaryProbe extends ExecBinaryFunction {

		String log(final Object[] sources) {

			return parametersForLog(sources);
		}
	}
}
