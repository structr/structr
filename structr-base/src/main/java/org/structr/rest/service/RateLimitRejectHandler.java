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
package org.structr.rest.service;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logs requests that rate limiting refused, then hands them to the wrapped reject handler
 * (Jetty's DoSHandler has no logging above DEBUG, so without this a limited instance looks
 * idle in the log).
 *
 * THE LOGGING IS ITSELF RATE LIMITED, on purpose. A client sending thousands of requests per
 * second would otherwise make us write thousands of log lines per second, turning their flood
 * into disk and log-pipeline exhaustion - doing the attacker's work for them. Each client is
 * therefore logged at most twice per window: once when it is first refused, and once more if it
 * keeps going past the escalation threshold.
 *
 * The remote ADDRESS is logged deliberately: it is what an operator needs to block the source at
 * firewall or host level, which is where a serious flood has to be stopped. Request bodies,
 * cookies and Authorization headers are never logged - they would put credentials in the log.
 */
public class RateLimitRejectHandler implements Request.Handler {

	private static final Logger logger = LoggerFactory.getLogger(RateLimitRejectHandler.class);

	private final Request.Handler reject;
	private final int escalateAfter;
	private final int distinctClients;
	private final int maxTracked;

	/** Per-client state within the current window. */
	private static class Refusals {

		long windowStart;
		int count;
	}

	// bounded, so the log throttling cannot itself become a memory vector under a flood from
	// many addresses; oldest entry is evicted once maxTracked is reached
	private final Map<String, Refusals> perClient;

	private final AtomicLong globalWindowStart = new AtomicLong(System.currentTimeMillis());
	private final AtomicInteger clientsInWindow = new AtomicInteger();
	private final AtomicLong refusalsInWindow   = new AtomicLong();
	private final AtomicInteger linesInWindow   = new AtomicInteger();
	private volatile boolean distributedReported = false;
	private volatile boolean budgetReported      = false;

	public RateLimitRejectHandler(final Request.Handler reject) {

		this.reject          = reject;
		this.escalateAfter   = Settings.RateLimitLogEscalateAfter.getValue();
		this.distinctClients = Settings.RateLimitLogDistinctClients.getValue();
		this.maxTracked      = Math.max(1024, Settings.RateLimitMaxTrackers.getValue());

		this.perClient = new LinkedHashMap<>(256, 0.75f, false) {
			@Override
			protected boolean removeEldestEntry(final Map.Entry<String, Refusals> eldest) {

				return size() > maxTracked;
			}
		};
	}

	/*
	 * The window and the line ceiling are shared with every other throttled log site and are read
	 * on each use, so a change takes effect immediately. Everything else here is fixed when the
	 * handler is installed, like the limiter it wraps.
	 */
	private long windowMs() {

		return Settings.LogThrottleWindow.getValue();
	}

	private int maxLines() {

		return Settings.LogThrottleMaxLines.getValue();
	}

	@Override
	public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

		try {

			record(request);

		} catch (Throwable t) {

			// logging must never break the rejection itself
			logger.debug("Unable to record a rate-limited request", t);
		}

		return reject.handle(request, response, callback);
	}

	private void record(final Request request) {

		final String address = Request.getRemoteAddr(request);
		final long now       = System.currentTimeMillis();
		final long windowMs  = windowMs();

		final int count;
		final boolean firstInWindow;

		synchronized (perClient) {

			final Refusals refusals = perClient.computeIfAbsent(address, key -> new Refusals());
			if (now - refusals.windowStart >= windowMs) {

				refusals.windowStart = now;
				refusals.count       = 0;
			}

			refusals.count++;
			count         = refusals.count;
			firstInWindow = (count == 1);
		}

		trackWindow(now, firstInWindow);

		/* A brief overshoot is the everyday case - a client that burst too hard and will settle.
		   One WARN naming the address is enough for that. A client that keeps being refused past
		   the threshold is behaving like an attack and gets the full picture at ERROR, once. */
		if (firstInWindow) {

			if (mayLog()) {

				logger.warn("Rate limit: refused {} {} from {}", request.getMethod(), path(request), address);
			}

		} else if (count == escalateAfter) {

			if (mayLog()) {

				logger.error("Rate limit: {} requests from {} refused within {} ms - sustained flooding from a single address. Block it at firewall or host level if it continues. Last request: {} {}, user agent: {}",
					count, address, windowMs, request.getMethod(), path(request), userAgent(request));
			}
		}
	}

	/**
	 * The GLOBAL log budget for the current window.
	 *
	 * Throttling per client is not enough on its own: the per-client table is bounded, so a flood
	 * that ROTATES its source address evicts entries and every returning address then looks like a
	 * first offence again, which would produce a line per request - exactly the log-volume
	 * exhaustion the throttling exists to prevent, and trivially discoverable by reading this class.
	 * A hard ceiling per window closes that, whatever the addresses do.
	 *
	 * When the budget runs out, one final line says so, and the counters in the window summary keep
	 * counting, so the scale of the event is still visible without a line per request.
	 */
	private boolean mayLog() {

		final int maxLines  = maxLines();
		final long windowMs = windowMs();

		if (maxLines <= 0) {

			return true;
		}

		final int used = linesInWindow.incrementAndGet();
		if (used < maxLines) {

			return true;
		}

		if (used == maxLines && !budgetReported) {

			budgetReported = true;

			logger.error("Rate limit: log budget of {} entries for this {} ms window is exhausted, further refusals are counted but not logged individually. A flood rotating its source address is the usual cause; the totals are reported when the window ends.",
				maxLines, windowMs);
		}

		return false;
	}

	/**
	 * Counts distinct refused clients per window. Many different addresses being refused at once
	 * is the shape of a DISTRIBUTED flood, which no per-client line reveals, and which per-client
	 * rate limiting cannot stop - that has to be handled upstream.
	 */
	private void trackWindow(final long now, final boolean newClient) {

		final long windowStart = globalWindowStart.get();
		final long windowMs    = windowMs();

		if (now - windowStart >= windowMs) {

			if (globalWindowStart.compareAndSet(windowStart, now)) {

				final int clients   = clientsInWindow.getAndSet(0);
				final long refusals = refusalsInWindow.getAndSet(0);
				final boolean capped = budgetReported;

				linesInWindow.set(0);
				distributedReported = false;
				budgetReported      = false;

				/* the closing total is what makes a capped window readable: individual entries were
				   suppressed, but the scale of the event is still on the record */
				if (capped) {

					logger.error("Rate limit: window ended, {} requests from {} distinct addresses were refused in the last {} ms (individual entries were suppressed after the log budget ran out).",
						refusals, clients, windowMs);
				}
			}
		}

		refusalsInWindow.incrementAndGet();

		if (newClient) {

			final int clients = clientsInWindow.incrementAndGet();
			if (clients == distinctClients && !distributedReported) {

				distributedReported = true;

				logger.error("Rate limit: {} distinct addresses refused within {} ms ({} requests total) - this is the shape of a distributed flood, which per-client rate limiting cannot stop. Shed the traffic upstream (reverse proxy, CDN, firewall).",
					clients, windowMs, refusalsInWindow.get());
			}
		}
	}

	private String path(final Request request) {

		final HttpURI uri = request.getHttpURI();

		return uri != null ? uri.getPath() : "-";
	}

	private String userAgent(final Request request) {

		final String agent = request.getHeaders().get(HttpHeader.USER_AGENT);

		return agent != null ? agent : "-";
	}
}
