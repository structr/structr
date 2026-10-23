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
package org.structr.rest.auth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.event.RuntimeEventLog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Counts failed superuser passwords across all addresses and all ways in, and blocks them all for everyone once there are too many. */
public final class SuperUserLoginGuard {

	private static final Logger logger         = LoggerFactory.getLogger(SuperUserLoginGuard.class);
	private static final Deque<Long> failures  = new ArrayDeque<>();
	private static final AtomicLong lastStep   = new AtomicLong(-1L);
	private static long blockedAt              = 0L;

	private SuperUserLoginGuard() {}

	public static boolean isTwoFactorEnabled() {

		return Settings.SuperUserTwoFactorEnabled.getValue(false);
	}

	public static String getSecret() {

		final String secret = StringUtils.trim(Settings.SuperUserTwoFactorSecret.getValue());

		return StringUtils.isNotBlank(secret) ? secret : null;
	}

	public static boolean isTwoFactorActive() {

		return isTwoFactorEnabled() && getSecret() != null;
	}

	/** The outcome of a code check: a correct code of a time step that was already used is refused, but is no guess. */
	public enum CodeCheck { VALID, REUSED, INVALID }

	public static boolean isValidCode(final String secret, final String code) {

		return checkCode(secret, code) == CodeCheck.VALID;
	}

	/** Same algorithm, digits and period as user logins, the current and the previous time step; each step is accepted once, wherever its code is used, so one seen being typed is useless. */
	public static CodeCheck checkCode(final String secret, final String code) {

		if (StringUtils.isAnyBlank(secret, code)) {

			return CodeCheck.INVALID;
		}

		try {

			final int period       = Settings.TwoFactorPeriod.getValue();
			final long currentStep = System.currentTimeMillis() / 1000 / period;
			final byte[] submitted = code.trim().getBytes(StandardCharsets.UTF_8);

			for (long step = currentStep; step >= currentStep - 1; step--) {

				final String expected = TimeBasedOneTimePasswordHelper.generateNumberStringForStep(secret, "Hmac" + Settings.TwoFactorAlgorithm.getValue(), step, Settings.TwoFactorDigits.getValue());

				if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), submitted)) {

					while (true) {

						final long last = lastStep.get();
						if (step <= last) {

							return CodeCheck.REUSED;
						}

						if (lastStep.compareAndSet(last, step)) {

							return CodeCheck.VALID;
						}
					}
				}
			}

			return CodeCheck.INVALID;

		} catch (Exception ex) {

			logger.warn("Unable to check the second factor of the superuser: {}", ex.getMessage());

			return CodeCheck.INVALID;
		}
	}

	public static boolean isBlocked() {

		final long lockout = lockout();

		synchronized (failures) {

			return lockout > 0 && System.currentTimeMillis() < blockedAt + lockout;
		}
	}

	public static void registerFailure(final String source) {

		final long lockout = lockout();
		final int maximum  = Math.max(1, Settings.ConfigLoginMaxFailedAttempts.getValue(3));
		final long now     = System.currentTimeMillis();
		boolean blocked    = false;

		if (lockout <= 0) {

			return;
		}

		synchronized (failures) {

			while (!failures.isEmpty() && failures.peekFirst() < now - lockout) {

				failures.removeFirst();
			}

			failures.addLast(now);

			if (failures.size() >= maximum) {

				blockedAt = now;
				blocked   = true;

				failures.clear();
			}
		}

		if (blocked) {

			logger.warn("Superuser login blocked for {} s after {} failed password attempts, the last from {}", lockout / 1000, maximum, source);
			RuntimeEventLog.failedLogin("Superuser login blocked", Map.of("source", source, "attempts", maximum));
		}
	}

	/** For tests: forgets failures, the block and the last accepted code. */
	public static void reset() {

		synchronized (failures) {

			failures.clear();
			blockedAt = 0L;
		}

		lastStep.set(-1L);
	}

	private static long lockout() {

		return Settings.ConfigLoginLockoutSeconds.getValue(900) * 1000L;
	}
}
