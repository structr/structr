/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.engine;

import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * FAILING reproductions for pure-logic findings in {@link ProcessEngine}. Each
 * test asserts the *intended* behaviour and currently FAILS, pinning the bug so
 * that a fix makes the test pass. No graph / database needed.
 */
public class ProcessEngineLogicBugTest {

	/**
	 * PE-8: {@code transpileForeignScript} unconditionally drops every
	 * {@code execution.setVariable(...)} line. A script that *writes* a variable
	 * it never read loses that write entirely -- the transpiled (executable)
	 * portion below the preserved source comment no longer mentions it.
	 */
	@Test
	public void testSetVariableFirstWriteIsNotDropped() {

		final String out = ProcessEngine.transpileForeignScript("execution.setVariable(\"result\", 42);");

		// The whole source is preserved inside a /* ... */ comment; inspect only
		// the executable code emitted AFTER that comment block.
		final String executable = out.substring(out.indexOf("*/") + 2);

		assertTrue("A first-write execution.setVariable must survive transpilation, not be silently dropped; got:\n" + out,
			executable.contains("result"));
	}

	/**
	 * PE-9a: A short/zero P-style ISO-8601 duration (e.g. {@code P0D}) makes
	 * {@code computeFireAt} return null (the {@code millis > 0} guard), so the
	 * timer is silently never scheduled instead of firing immediately.
	 */
	@Test
	public void testZeroDurationTimerStillSchedules() {

		final Date fireAt = ProcessEngine.computeFireAt("timeDuration", "P0D");

		assertNotNull("A zero/near-zero duration timer should be scheduled (fire ~now), not silently dropped", fireAt);
	}

	/**
	 * PE-9b: A {@code timeDate} without an explicit zone/offset
	 * (e.g. {@code 2026-04-25T14:30:00}) makes {@code Instant.parse} throw, which
	 * is swallowed and returns null -- the timer is silently never scheduled.
	 */
	@Test
	public void testLocalDateTimeTimerStillSchedules() {

		final Date fireAt = ProcessEngine.computeFireAt("timeDate", "2026-04-25T14:30:00");

		assertNotNull("A zoneless timeDate should still schedule (assume system zone), not be silently dropped", fireAt);
	}
}
