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
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;

/**
 * Maven lifecycle participant that prints the review-need summary at the very end of the reactor.
 *
 * <p>{@code afterSessionEnd} is Maven's only hook that runs once, after every module has built,
 * on success and on failure alike -- which is exactly the requirement here (a per-module plugin
 * binding would run mid-reactor and would be skipped when an earlier module, e.g. the Playwright
 * container, fails). It simply shells out to the single-source {@code config/style/ReviewNeed.java}
 * via the JDK source launcher, so the scoring logic lives in one place and no Python is needed.</p>
 *
 * <p>Never throws: a triage report must not be able to break a build. Disable with
 * {@code -DskipReviewNeed=true}.</p>
 */
@Named
@Singleton
public class ReviewNeedReporter extends AbstractMavenLifecycleParticipant {

	@Override
	public void afterSessionEnd(final MavenSession session) {

		if (session != null && Boolean.parseBoolean(session.getUserProperties().getProperty("skipReviewNeed", "false"))) {

			return;
		}

		try {

			final File root = session.getRequest().getMultiModuleProjectDirectory();
			if (root == null) {

				return;
			}

			final File script = new File(root, "config/style/ReviewNeed.java");
			if (!script.isFile()) {

				return;
			}

			final boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
			final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + (windows ? "java.exe" : "java");
			final ProcessBuilder pb = new ProcessBuilder(javaBin, script.getAbsolutePath(), "--summary", "--main-only", root.getAbsolutePath());

			pb.inheritIO();
			pb.start().waitFor();

		} catch (final Exception e) {

			System.out.println("[review-need] skipped (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
		}
	}
}
