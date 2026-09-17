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
package org.structr.test.common;

import org.structr.api.config.Settings;
import org.structr.common.LogThrottle;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The budget that keeps a log statement about a caller-triggered event from becoming the attack.
 *
 * <p>Every statement wrapped in this decides how much an unauthenticated caller can make the server
 * write. The two ceilings are not interchangeable and the second is the one that actually holds: the
 * per-key table is bounded, so a caller who rotates addresses evicts entries and every returning key
 * looks like a first offence again. These tests pin both, and pin that exhausting the budget refuses
 * rather than throws, because a throttle that fails loudly would be the amplifier it replaced.</p>
 *
 * <p>No database and no server: what is being decided here is arithmetic over two settings.</p>
 */
public class LogThrottleTest {

	private Integer previousWindow;
	private Integer previousMaxLines;

	@BeforeMethod
	public void setUp() {

		previousWindow   = Settings.LogThrottleWindow.getValue();
		previousMaxLines = Settings.LogThrottleMaxLines.getValue();
	}

	@AfterMethod
	public void tearDown() {

		Settings.LogThrottleWindow.setValue(previousWindow);
		Settings.LogThrottleMaxLines.setValue(previousMaxLines);
	}

	@Test
	public void testTheFirstEventForAKeyIsLoggedAndTheRestAreNot() {

		Settings.LogThrottleWindow.setValue(60_000);
		Settings.LogThrottleMaxLines.setValue(200);

		final LogThrottle throttle = new LogThrottle("test", 1);

		assertTrue("the first event for a key was suppressed",  throttle.allow("10.0.0.2"));
		assertFalse("a repeat of the same event was logged",    throttle.allow("10.0.0.2"));
		assertFalse("a repeat of the same event was logged",    throttle.allow("10.0.0.2"));
	}

	@Test
	public void testEachKeyGetsItsOwnAllowance() {

		Settings.LogThrottleWindow.setValue(60_000);
		Settings.LogThrottleMaxLines.setValue(200);

		final LogThrottle throttle = new LogThrottle("test", 1);

		// one noisy event must not crowd out a different one, which is what the per-key ceiling is for
		assertTrue("the first key was suppressed",  throttle.allow("10.0.0.2"));
		assertTrue("a second, different key was suppressed", throttle.allow("10.0.0.3"));
		assertTrue("a third, different key was suppressed",  throttle.allow("10.0.0.4"));

		assertFalse("the first key was logged twice", throttle.allow("10.0.0.2"));
	}

	@Test
	public void testTheGlobalCeilingHoldsWhateverTheKeyVaries() {

		Settings.LogThrottleWindow.setValue(60_000);
		Settings.LogThrottleMaxLines.setValue(3);

		// per key this would allow a thousand, so only the global total can stop it
		final LogThrottle throttle = new LogThrottle("test", 1000);

		assertTrue("the first of three allowed entries was suppressed",  throttle.allow("10.0.0.1"));
		assertTrue("the second of three allowed entries was suppressed", throttle.allow("10.0.0.2"));
		assertTrue("the third of three allowed entries was suppressed",  throttle.allow("10.0.0.3"));

		// a caller rotating its address past the ceiling is the case this exists for
		for (int i = 4; i < 200; i++) {

			assertFalse("the global ceiling let entry " + i + " through, so a caller varying its address"
				+ " can still write a line per request", throttle.allow("10.0.0." + i));
		}
	}

	@Test
	public void testAceilingOfZeroRemovesTheGlobalLimitOnly() {

		Settings.LogThrottleWindow.setValue(60_000);
		Settings.LogThrottleMaxLines.setValue(0);

		final LogThrottle throttle = new LogThrottle("test", 1);

		for (int i = 0; i < 500; i++) {

			assertTrue("a distinct key was suppressed although the ceiling is off", throttle.allow("10.0.0." + i));
		}

		// the per-key ceiling is code, not configuration, so it still applies
		assertFalse("the per-key ceiling stopped applying when the global one was removed", throttle.allow("10.0.0.0"));
	}

	@Test
	public void testANewWindowStartsTheCountingAgain() {

		// a window of zero has elapsed by the time it is read, so every call opens a new one
		Settings.LogThrottleWindow.setValue(0);
		Settings.LogThrottleMaxLines.setValue(1);

		final LogThrottle throttle = new LogThrottle("test", 1);

		assertTrue("the first window refused its first entry",  throttle.allow("10.0.0.2"));
		assertTrue("a new window did not reset the per-key count", throttle.allow("10.0.0.2"));
		assertTrue("a new window did not reset the total",         throttle.allow("10.0.0.2"));
	}

	@Test
	public void testANullKeyIsCountedRatherThanRejected() {

		Settings.LogThrottleWindow.setValue(60_000);
		Settings.LogThrottleMaxLines.setValue(200);

		final LogThrottle throttle = new LogThrottle("test", 1);

		// documented as permitted, and a throttle that threw would take the log statement with it
		assertTrue("a null key was suppressed on its first use", throttle.allow(null));
		assertFalse("a null key was not counted", throttle.allow(null));
	}
}
