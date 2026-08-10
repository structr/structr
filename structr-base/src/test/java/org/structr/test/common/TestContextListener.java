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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Puts the running test into the logging context, so that every log line says which test produced
 * it, and writes one machine-readable verdict per test method.
 *
 * <h3>Why the log line has to carry its owner</h3>
 *
 * <p>The suite runs with {@code forkCount=4} and {@code reuseForks=false}, so up to four JVMs write
 * into a single merged stream. Measured on a full run: 115 of 2753 test boundaries overlapped, and
 * <b>73% of all WARN/ERROR lines were emitted while more than one test was open</b>. For those lines
 * the owner is not recoverable afterwards -- nothing on the line distinguishes the forks. The
 * information existed at write time; merging threw it away.</p>
 *
 * <p>Everything that reconstructs it downstream is therefore guesswork: pairing the
 * {@code ##### Starting} markers is a coin flip at that overlap rate, and surefire's per-class XML
 * capture is only approximate at the boundaries (it flushes the next test's first lines into the
 * previous test's block). Both fail silently, which is the worst property a triage tool can have. So
 * the line carries its owner instead, and nothing has to be reconstructed.</p>
 *
 * <p>This is not a new mechanism for Structr: the production log already threads context through the
 * MDC with {@code %X{structrScratchMDC}}. This is the same idiom with the test as its subject, and it
 * is what lets {@code config/testlog/TestLogReview.java} answer questions no surefire report can --
 * which test logged a swallowed exception, which ERROR signatures occur <i>only</i> in tests that
 * pass, and which output belongs to no test at all.</p>
 *
 * <h3>Unowned output is a feature, not a gap</h3>
 *
 * <p>The MDC is thread-local, so lines written by background threads -- pools, agents, shutdown
 * hooks -- deliberately carry no test. That is worth seeing rather than papering over: output nobody
 * owns is where leaks and teardown bugs live, and it is precisely the class of finding a per-test
 * capture cannot show, because such a capture attributes those lines to whichever test happened to
 * be open and thereby blames an innocent one.</p>
 */
public class TestContextListener implements IInvokedMethodListener {

	/** Read by the {@code %X{test}} field of the logback pattern; empty outside a test. */
	public static final String MDC_KEY = "test";

	/**
	 * A short, explicit logger name rather than this class's own: {@code %logger{36}} abbreviates
	 * anything longer, and this logger's output is read by a tool.
	 */
	private static final Logger logger = LoggerFactory.getLogger("org.structr.test.TestContext");

	@Override
	public void beforeInvocation(final IInvokedMethod method, final ITestResult result) {

		// configuration methods included: output from setUp/tearDown belongs to the test around it
		MDC.put(MDC_KEY, " {" + name(result) + "}");
	}

	@Override
	public void afterInvocation(final IInvokedMethod method, final ITestResult result) {

		if (method.isTestMethod()) {

			// key=value, written by code and never typed by a person, so a tool can read the verdict
			// without depending on anybody's prose
			logger.info("test={} status={} durationMs={}", qualifiedName(result), statusOf(result), durationOf(result));
		}

		MDC.remove(MDC_KEY);
	}

	/**
	 * Simple class name, not the qualified one: this goes on every single log line, and the verdict
	 * line carries the qualified name for anyone who needs to find the class.
	 */
	private String name(final ITestResult result) {

		final String method = (result.getMethod() != null) ? result.getMethod().getMethodName() : "?";
		final Class<?> type = (result.getTestClass() != null) ? result.getTestClass().getRealClass() : null;

		return ((type != null) ? type.getSimpleName() : "?") + "#" + method;
	}

	private String qualifiedName(final ITestResult result) {

		final String method = (result.getMethod() != null) ? result.getMethod().getMethodName() : "?";
		final Class<?> type = (result.getTestClass() != null) ? result.getTestClass().getRealClass() : null;

		return ((type != null) ? type.getName() : "?") + "#" + method;
	}

	private String statusOf(final ITestResult result) {

		return switch (result.getStatus()) {

			case ITestResult.SUCCESS -> "SUCCESS";
			case ITestResult.FAILURE -> "FAILURE";
			case ITestResult.SKIP    -> "SKIP";

			default -> "OTHER";
		};
	}

	private long durationOf(final ITestResult result) {

		final long end = (result.getEndMillis() > 0) ? result.getEndMillis() : System.currentTimeMillis();

		return Math.max(0, end - result.getStartMillis());
	}
}
