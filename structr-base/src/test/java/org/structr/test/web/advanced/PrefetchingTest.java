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

import java.util.UUID;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the self-regulation of the query prefetching in the bolt driver. Prefetching is learned per
 * request (hint): when a transaction runs the same relationship query more than a threshold number
 * of times, the driver loads all relationships of that type up front in the following requests.
 * Those patterns are type-wide, so their cost grows with the database, not with the request. A
 * prefetch that loads far more than the request uses must be deactivated again, otherwise every
 * request with that hint pays for loading the whole graph.
 */
public class PrefetchingTest extends StructrUiTest {

	private static final String BOLT_LOGGER = "org.structr.bolt.SessionTransaction";

	@Test
	public void testIneffectivePrefetchingIsDeactivated() {

		if (!Settings.DatabaseDriver.getValue().contains("bolt")) {

			throw new SkipException("Prefetching is a feature of the bolt driver, skipping test with " + Settings.DatabaseDriver.getValue());
		}

		final int threshold = 20;
		final int costRatio = 10;
		String smallDiv     = null;
		String largeDiv     = null;

		// a DOM that is large compared to the container "small": 4 pages with 300 divs each
		try (final Tx tx = app.tx()) {

			createAdminUser();

			for (int i = 0; i < 4; i++) {

				createPageWithDivs("filler" + i, 300);
			}

			// looking up the children of "small" runs the CONTAINS lookup more often than the threshold, so
			// prefetching is activated, but the prefetch then loads the whole DOM for a few dozen lookups
			smallDiv = createPageWithDivs("small", threshold + 10);

			// "large" uses enough of the prefetched DOM to make the prefetch worthwhile
			largeDiv = createPageWithDivs("large", 3000);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Logger logger                         = (Logger) LoggerFactory.getLogger(BOLT_LOGGER);
		final ListAppender<ILoggingEvent> appender  = new ListAppender<>();
		final boolean previousLogging               = Settings.CypherDebugLogging.getValue();
		final int previousThreshold                 = Settings.PrefetchingThreshold.getValue();

		Settings.CypherDebugLogging.setValue(true);
		Settings.PrefetchingThreshold.setValue(threshold);
		Settings.PrefetchingCostRatio.setValue(costRatio);

		appender.start();
		logger.addAppender(appender);

		try {

			// 1st request: learns the pattern
			lookupChildren("small request", smallDiv);
			assertTrue("Prefetching was not activated for the CONTAINS lookups", containsMessage(appender, "Activating prefetching for", "CONTAINS"));

			// 2nd request: runs the prefetch, which loads the whole DOM for a few lookups => must be deactivated
			appender.list.clear();
			lookupChildren("small request", smallDiv);
			assertTrue("Prefetch was not executed in the second request", containsMessage(appender, "prefetched", "(n:DOMNode"));
			assertTrue("Ineffective prefetch was not deactivated", containsMessage(appender, "Blacklisting prefetching pattern", "lookups"));

			// 3rd request: no prefetch any more
			appender.list.clear();
			lookupChildren("small request", smallDiv);
			assertFalse("Deactivated prefetch was executed again", containsMessage(appender, "prefetched", "(n:DOMNode"));

			// control: the large request profits from the prefetch, so it stays active
			lookupChildren("large request", largeDiv);
			appender.list.clear();
			lookupChildren("large request", largeDiv);
			assertTrue("Prefetch was not executed for the large request", containsMessage(appender, "prefetched", "(n:DOMNode"));
			assertFalse("Effective prefetch was deactivated", containsMessage(appender, "Blacklisting prefetching pattern", "lookups"));

			appender.list.clear();
			lookupChildren("large request", largeDiv);
			assertTrue("Effective prefetch was not executed again", containsMessage(appender, "prefetched", "(n:DOMNode"));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");

		} finally {

			logger.detachAppender(appender);
			appender.stop();

			Settings.CypherDebugLogging.setValue(previousLogging);
			Settings.PrefetchingThreshold.setValue(previousThreshold);
			Settings.PrefetchingCostRatio.setValue(Settings.PrefetchingCostRatio.getDefaultValue());
		}
	}

	/**
	 * A hint names a kind of request, and requests that differ only in the object they address must share it. The
	 * hints contain the request path, so they used to differ by the UUID in it, and every object learned its own
	 * patterns (ticket 1420). Both UUID formats must be recognized, independent of the configured one.
	 */
	@Test
	public void testRequestsForDifferentObjectsShareTheLearnedPatterns() {

		if (!Settings.DatabaseDriver.getValue().contains("bolt")) {

			throw new SkipException("Prefetching is a feature of the bolt driver, skipping test with " + Settings.DatabaseDriver.getValue());
		}

		final int threshold = 20;
		String div          = null;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			div = createPageWithDivs("detail", threshold + 100);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Logger logger                         = (Logger) LoggerFactory.getLogger(BOLT_LOGGER);
		final ListAppender<ILoggingEvent> appender  = new ListAppender<>();
		final boolean previousLogging               = Settings.CypherDebugLogging.getValue();
		final int previousThreshold                 = Settings.PrefetchingThreshold.getValue();

		Settings.CypherDebugLogging.setValue(true);
		Settings.PrefetchingThreshold.setValue(threshold);

		appender.start();
		logger.addAppender(appender);

		try {

			final String first  = UUID.randomUUID().toString();
			final String second = UUID.randomUUID().toString();

			// UUIDs without dashes: the pattern learned for the first object must be used for the second one
			lookupChildren("REST GET /structr/rest/DOMNode/" + first.replace("-", "") + "/children", div);
			assertTrue("Prefetching was not activated for the CONTAINS lookups", containsMessage(appender, "Activating prefetching for", "CONTAINS"));

			appender.list.clear();
			lookupChildren("REST GET /structr/rest/DOMNode/" + second.replace("-", "") + "/children", div);
			assertTrue("A request for another object (UUID without dashes) did not use the learned pattern", containsMessage(appender, "prefetched", "(n:DOMNode"));

			// UUIDs with dashes, under a hint that has not learned anything yet
			appender.list.clear();
			lookupChildren("HTTP GET /detail/" + first, div);
			assertTrue("Prefetching was not activated for the CONTAINS lookups", containsMessage(appender, "Activating prefetching for", "CONTAINS"));

			appender.list.clear();
			lookupChildren("HTTP GET /detail/" + second, div);
			assertTrue("A request for another object (UUID with dashes) did not use the learned pattern", containsMessage(appender, "prefetched", "(n:DOMNode"));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");

		} finally {

			logger.detachAppender(appender);
			appender.stop();

			Settings.CypherDebugLogging.setValue(previousLogging);
			Settings.PrefetchingThreshold.setValue(previousThreshold);
		}
	}

	// ----- private methods -----
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
	 * Creates a page with a container div that has the given number of child divs.
	 *
	 * @return the UUID of the container div, to be rendered as a partial
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
}
