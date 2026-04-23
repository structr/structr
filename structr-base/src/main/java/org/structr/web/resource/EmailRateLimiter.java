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
package org.structr.web.resource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Throttles outbound-email-sending endpoints (password reset, registration)
 * to bound both volume per source IP and harassment per target address.
 *
 * Each caller names its own bucket so the password-reset and registration
 * counters do not share state.
 */
final class EmailRateLimiter {

	private static final Cache<String, Counter> counts = CacheBuilder.newBuilder()
		.maximumSize(100_000)
		.expireAfterWrite(1, TimeUnit.HOURS)
		.build();

	private static final class Counter {
		int count;
	}

	private EmailRateLimiter() {}

	/**
	 * Returns true if the request should be allowed. Both buckets are
	 * incremented on success; if the IP limit is already over, the email
	 * counter is not touched.
	 */
	static boolean allow(final String bucket, final String remoteIp, final int maxPerIpPerHour,
	                     final String email,    final int maxPerEmailPerHour) {

		if (!tryIncrement(bucket + ":ip:" + remoteIp, maxPerIpPerHour)) {
			return false;
		}
		if (!tryIncrement(bucket + ":email:" + email, maxPerEmailPerHour)) {
			return false;
		}
		return true;
	}

	private static boolean tryIncrement(final String key, final int max) {

		final Counter counter;
		try {
			counter = counts.get(key, Counter::new);
		} catch (final Exception ex) {
			// Counter::new is total; defensive fall-open so loader failure
			// never blocks a legitimate user.
			return true;
		}

		synchronized (counter) {
			if (counter.count >= max) {
				return false;
			}
			counter.count++;
			return true;
		}
	}
}
