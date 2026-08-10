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

/**
 * Whether the end-of-build reports may use colour.
 *
 * <p>Shared by both reporters so they can never disagree, and so the answer comes from Maven rather
 * than from a guess. An earlier version ended with {@code System.console() != null}, which is the
 * wrong question: it is null whenever output is piped or redirected, so both reports came out plain in
 * the middle of a build in which Maven itself had colourised 22682 lines.</p>
 */
final class ReportColor {

	private ReportColor() {
	}

	static boolean enabled() {

		// -Dstyle.color decides for Maven's own output, so it decides for ours too
		final String style = System.getProperty("style.color");
		if ("never".equalsIgnoreCase(style) || "false".equalsIgnoreCase(style)) {

			return false;
		}

		if ("always".equalsIgnoreCase(style) || "true".equalsIgnoreCase(style)) {

			return true;
		}

		if (System.getenv("NO_COLOR") != null) {

			return false;
		}

		try {

			// Maven's own answer, so the reports match the rest of the build whatever the terminal is.
			// Reflected rather than compiled against: a report must never be able to break a build, not
			// even if this class moves in a future Maven.
			final Class<?> messageUtils = Class.forName("org.apache.maven.shared.utils.logging.MessageUtils");

			return (Boolean) messageUtils.getMethod("isColorEnabled").invoke(null);

		} catch (final Throwable ignored) {

			return System.console() != null;
		}
	}
}
