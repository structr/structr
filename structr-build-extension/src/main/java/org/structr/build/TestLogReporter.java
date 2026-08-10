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
package org.structr.build;

import java.io.File;
import java.io.InputStream;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;

/**
 * Maven lifecycle participant that prints the test-log review at the very end of the reactor.
 *
 * <p>A green build hides plenty -- exceptions logged and swallowed, messages at the wrong level, tests
 * that pass while their own log says otherwise -- and none of it is in a surefire report. This makes
 * that visible on every build instead of only when somebody remembers to pipe the console into a file:
 * the tool reads the captured output that surefire and failsafe already write to
 * {@code target/*-reports/TEST-*.xml}, so no {@code tee} is required.</p>
 *
 * <p>{@code afterSessionEnd} for the same reason as {@link CodeQualityReporter}: it is the only hook
 * that runs once, after every module, on success and failure alike. Both reporters use it, and Maven
 * does not define their relative order -- so this one keeps its output to a few labelled lines that
 * read the same wherever they land.</p>
 *
 * <p>Never throws, and prints nothing at all when the build ran no tests. Disable with
 * {@code -DskipTestLogReview=true}.</p>
 */
@Named
@Singleton
public class TestLogReporter extends AbstractMavenLifecycleParticipant {

	@Override
	public void afterSessionEnd(final MavenSession session) {

		if (session != null && Boolean.parseBoolean(session.getUserProperties().getProperty("skipTestLogReview", "false"))) {

			return;
		}

		try {

			final File root = session.getRequest().getMultiModuleProjectDirectory();
			if (root == null) {

				return;
			}

			final File script = new File(root, "config/testlog/TestLogReview.java");
			if (!script.isFile()) {

				return;
			}

			final boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
			final String javaBin  = System.getProperty("java.home") + File.separator + "bin" + File.separator + (windows ? "java.exe" : "java");

			// The review runs as a child process with its output piped back here, so it cannot detect a
			// terminal itself. Decide here, where the real console is, and pass the answer explicitly.
			final ProcessBuilder pb = new ProcessBuilder(javaBin, script.getAbsolutePath(), "--reports", root.getAbsolutePath(), "--brief", "--color", ReportColor.enabled() ? "always" : "never");

			// Run in the project root so the committed baseline at config/testlog/normal.baseline is found
			// and the report can say what is NEW rather than listing everything every time.
			pb.directory(root);

			// Emit on STDERR, never STDOUT: a build's stdout may be captured (e.g.
			// `VERSION=$(mvn help:evaluate -q -DforceStdout)`), and appending to it corrupts that value.
			pb.redirectErrorStream(true);

			final Process process = pb.start();

			try (final InputStream in = process.getInputStream()) {

				in.transferTo(System.err);
			}

			process.waitFor();

		} catch (final Exception e) {

			System.err.println("[test-log] skipped (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
		}
	}

}
