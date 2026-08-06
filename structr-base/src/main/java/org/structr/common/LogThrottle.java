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
package org.structr.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A budget for log entries about events a remote caller can trigger at will.
 *
 * Anything logged once per rejected request is an amplification vector: a client sending
 * thousands of requests per second makes the server write thousands of log lines per second,
 * so an attacker can exhaust disk or a log pipeline cheaply and, in an open-source product,
 * knows exactly which requests to send to do it. Wrap such statements in a throttle.
 *
 * Two ceilings, because either alone is insufficient:
 *
 * <ul>
 * <li>per KEY, so one repeated event does not crowd out everything else, and</li>
 * <li>a GLOBAL total, which is what actually holds: a caller who varies the key - rotating
 *     addresses, user names or paths - defeats per-key counting on its own, because the key
 *     table is necessarily bounded and evicted entries look new again.</li>
 * </ul>
 *
 * Suppressed events are counted and reported once per window, so a throttled log still says how
 * much it is hiding rather than going quiet.
 */
public class LogThrottle {

	private static final Logger logger = LoggerFactory.getLogger(LogThrottle.class);

	private static final int MAX_KEYS = 4096;

	private final String name;
	private final int maxPerKey;

	private final Map<String, Integer> perKey;

	private long windowStart = System.currentTimeMillis();
	private int total        = 0;
	private long suppressed  = 0;

	/**
	 * The window length and the total per window are shared configuration, read on each call so
	 * that a change takes effect immediately: instances of this class are typically held in static
	 * fields, so reading the settings here rather than in the constructor is what keeps them from
	 * being frozen for the lifetime of the process.
	 *
	 * @param name       what is being throttled, used in the suppression summary
	 * @param maxPerKey  entries allowed per key per window. Stays in code because it expresses how
	 *                   noisy one particular statement is, which is a property of that statement.
	 */
	public LogThrottle(final String name, final int maxPerKey) {

		this.name      = name;
		this.maxPerKey = maxPerKey;

		this.perKey = new LinkedHashMap<>(256, 0.75f, false) {
			@Override
			protected boolean removeEldestEntry(final Map.Entry<String, Integer> eldest) {

				return size() > MAX_KEYS;
			}
		};
	}

	/**
	 * Whether this event may be logged now. Call it in the condition of the log statement and
	 * keep the statement itself unchanged, so the message stays greppable.
	 *
	 * @param key groups events that are "the same" for throttling purposes, e.g. a signature and
	 *            method, or a remote address. May be null.
	 * @return true if the caller should log
	 */
	public synchronized boolean allow(final String key) {

		final long now      = System.currentTimeMillis();
		final long windowMs = Settings.LogThrottleWindow.getValue();
		final int maxTotal  = Settings.LogThrottleMaxLines.getValue();

		if (now - windowStart >= windowMs) {

			final long hidden = suppressed;

			windowStart = now;
			total       = 0;
			suppressed  = 0;
			perKey.clear();

			if (hidden > 0) {

				logger.info("{}: {} further entries were suppressed in the previous {} ms.", name, hidden, windowMs);
			}
		}

		// 0 removes the ceiling, matching the setting's documented meaning
		if (maxTotal > 0 && total >= maxTotal) {

			suppressed++;

			return false;
		}

		final String effectiveKey = key != null ? key : "";
		final int forKey          = perKey.getOrDefault(effectiveKey, 0);

		if (forKey >= maxPerKey) {

			suppressed++;

			return false;
		}

		perKey.put(effectiveKey, forKey + 1);
		total++;

		return true;
	}
}
