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
package org.structr.test.web.advanced;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Measures what the learned prefetching of the bolt driver saves for a request that looks up the children of many
 * DOM elements, one CONTAINS lookup per element, like the N+1 lookups prefetching is meant for.
 *
 * <p>The same request runs under two hints: one that has learned the CONTAINS pattern and one that has not and cannot
 * learn it, because the threshold is raised after the first hint has learned. Learned patterns are type-wide, so the
 * prefetch loads the DOM of all pages, not only the one the request uses; the other pages are there to make it pay
 * for that like it does in a real application.
 *
 * <p>The number of database queries per request is counted from the Cypher debug log and does not depend on the
 * machine, so the test requires that prefetching reduces it. The time per request is only reported: it is measured
 * alternately for both hints, the median of the pairs is taken, and which one runs first is swapped every other pair,
 * so that drift and JIT warm-up affect both equally (see SitePerformanceTest). Fixed time limits would fail on slow
 * machines, so there are none.
 */
public class PrefetchingEffectivenessTest extends StructrUiTest {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PrefetchingEffectivenessTest.class);

	private static final String BOLT_LOGGER    = "org.structr.bolt.SessionTransaction";
	private static final String LEARNED_HINT   = "Prefetching effectiveness: learned";
	private static final String UNLEARNED_HINT = "Prefetching effectiveness: not learned";

	// a query as SessionTransaction.logQuery() logs it: "<thread id>: <transaction id>: <nodes>/<rels> - <statement>"
	private static final Pattern QUERY_LOG_LINE    = Pattern.compile("^\\d+: \\d+: \\d+/\\d+ - .*", Pattern.DOTALL);

	// a prefetch query, which is not logged by logQuery() but counts as a query of the request: "<transaction id>: prefetched <count> entities ..."
	private static final Pattern PREFETCH_LOG_LINE = Pattern.compile("^\\d+: prefetched \\d+ entities .*", Pattern.DOTALL);

	private static final int CHILDREN    = 1000;
	private static final int OTHER_PAGES = 2;
	private static final int THRESHOLD   = 20;
	private static final int SAMPLES     = 10;
	private static final int WARMUP      = 3;

	@Test
	public void testPrefetchingReducesTheQueriesOfARequest() {

		if (!Settings.DatabaseDriver.getValue().contains("bolt")) {

			throw new SkipException("Prefetching is a feature of the bolt driver, skipping test with " + Settings.DatabaseDriver.getValue());
		}

		String container = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			for (int i = 0; i < OTHER_PAGES; i++) {

				createPageWithDivs("other" + i, CHILDREN);
			}

			container = createPageWithDivs("measured", CHILDREN);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Logger boltLogger                     = (Logger) LoggerFactory.getLogger(BOLT_LOGGER);
		final ListAppender<ILoggingEvent> appender  = new ListAppender<>();
		final boolean previousLogging               = Settings.CypherDebugLogging.getValue();
		final int previousThreshold                 = Settings.PrefetchingThreshold.getValue();

		appender.start();
		boltLogger.addAppender(appender);

		try {

			Settings.CypherDebugLogging.setValue(true);
			Settings.PrefetchingThreshold.setValue(THRESHOLD);

			// the first request under the learning hint learns the pattern, the threshold then keeps the other hint from learning it
			lookupChildren(LEARNED_HINT, container);
			assertTrue("Prefetching was not activated for the CONTAINS lookups", containsMessage(appender, "Activating prefetching for", "CONTAINS"));

			Settings.PrefetchingThreshold.setValue(Integer.MAX_VALUE);

			appender.list.clear();
			lookupChildren(LEARNED_HINT, container);

			final long queriesWithPrefetch = countQueries(appender);

			assertTrue("The learned prefetch was not executed", containsMessage(appender, "prefetched", "(n:DOMNode"));
			assertFalse("The learned prefetch was deactivated, so the measurement would compare two requests without it", containsMessage(appender, "Blacklisting prefetching pattern"));

			appender.list.clear();
			lookupChildren(UNLEARNED_HINT, container);

			final long queriesWithoutPrefetch = countQueries(appender);

			assertFalse("The request without a learned pattern ran a prefetch", containsMessage(appender, "prefetched", "(n:DOMNode"));

			// the time is measured without the debug log, which costs more for the request with more queries
			Settings.CypherDebugLogging.setValue(false);
			appender.list.clear();

			final double[] withPrefetch    = new double[SAMPLES];
			final double[] withoutPrefetch = new double[SAMPLES];

			for (int i = 0; i < WARMUP; i++) {

				lookupChildren(LEARNED_HINT, container);
				lookupChildren(UNLEARNED_HINT, container);
			}

			for (int i = 0; i < SAMPLES; i++) {

				// the two are swapped every other pair, so being measured first is worth the same to each of them
				if (i % 2 == 0) {

					withPrefetch[i]    = timeLookupChildren(LEARNED_HINT, container);
					withoutPrefetch[i] = timeLookupChildren(UNLEARNED_HINT, container);

				} else {

					withoutPrefetch[i] = timeLookupChildren(UNLEARNED_HINT, container);
					withPrefetch[i]    = timeLookupChildren(LEARNED_HINT, container);
				}
			}

			final double medianWith    = median(withPrefetch);
			final double medianWithout = median(withoutPrefetch);

			logger.info("Prefetching effectiveness: {} CONTAINS lookups in a DOM of {} pages with {} elements each", CHILDREN, OTHER_PAGES + 1, CHILDREN);
			logger.info("  queries per request:        {} with prefetching, {} without", queriesWithPrefetch, queriesWithoutPrefetch);
			logger.info("  median time per request:    {} ms with prefetching, {} ms without ({} pairs after {} warmup pairs)", format(medianWith), format(medianWithout), SAMPLES, WARMUP);
			logger.info("  time with prefetching:      {} % of the time without", format(100.0 * medianWith / medianWithout));

			assertTrue("Prefetching did not reduce the queries of the request: " + queriesWithPrefetch + " with prefetching, " + queriesWithoutPrefetch + " without",
				queriesWithPrefetch < queriesWithoutPrefetch);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");

		} finally {

			boltLogger.detachAppender(appender);
			appender.stop();

			Settings.CypherDebugLogging.setValue(previousLogging);
			Settings.PrefetchingThreshold.setValue(previousThreshold);
		}
	}

	// ----- private methods -----
	private double timeLookupChildren(final String hint, final String containerId) throws FrameworkException {

		final long start = System.nanoTime();

		lookupChildren(hint, containerId);

		return (System.nanoTime() - start) / 1_000_000.0;
	}

	/**
	 * Simulates a request: a transaction with a prefetch hint that looks up the children of the
	 * given container and of each child (one CONTAINS lookup per element, like rendering does).
	 */
	private void lookupChildren(final String hint, final String containerId) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			tx.prefetchHint(hint);

			final DOMNode container = app.getNodeById(StructrTraits.DOM_NODE, containerId).as(DOMNode.class);

			for (final DOMNode child : container.getChildren()) {

				for (final DOMNode grandChild : child.getChildren()) {

					grandChild.getUuid();
				}
			}

			tx.success();
		}
	}

	/**
	 * Creates a page with a container div that has the given number of child divs, each with a text node.
	 *
	 * @return the UUID of the container div
	 */
	private String createPageWithDivs(final String name, final int count) throws FrameworkException {

		final Page page   = Page.createSimplePage(securityContext, name);
		final DOMNode div = page.getElementsByTagName("div").get(0);

		for (int i = 0; i < count; i++) {

			final DOMElement child = page.createElement("div");

			div.appendChild(child);
			child.appendChild(page.createTextNode(name + " " + i));
		}

		return div.getUuid();
	}

	private long countQueries(final ListAppender<ILoggingEvent> appender) {

		return appender.list.stream().map(ILoggingEvent::getFormattedMessage).filter(message -> QUERY_LOG_LINE.matcher(message).matches() || PREFETCH_LOG_LINE.matcher(message).matches()).count();
	}

	private boolean containsMessage(final ListAppender<ILoggingEvent> appender, final String... parts) {

		return appender.list.stream().map(ILoggingEvent::getFormattedMessage).anyMatch(message -> {

			for (final String part : parts) {

				if (!message.contains(part)) {

					return false;
				}
			}

			return true;
		});
	}

	private double median(final double[] values) {

		final double[] sorted = values.clone();

		Arrays.sort(sorted);

		return sorted[sorted.length / 2];
	}

	private String format(final double value) {

		return String.format("%.1f", value);
	}
}
