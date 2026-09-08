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
package org.structr.diff.compare;

import org.structr.diff.model.Kind;

import java.util.List;

/**
 * Marks the deltas a reader can safely collapse first.
 *
 * Every rule here holds on ANY Structr instance. That is the whole admission policy: a rule that
 * needs to know which application was exported does not belong, however obviously noisy the thing
 * it would catch. Those cases are real, and the UI answers them by letting a reader group and
 * collapse a kind, which is reversible and visible in a way a server-side rule is not.
 */
public class Classifier {

	/** Assigns a signal to every delta, in place. */
	public static void classify(final List<Delta> deltas) {

		for (final Delta delta : deltas) {

			apply(delta);
		}
	}

	private static void apply(final Delta delta) {

		// Same entity, different uuid: the two sides agreed on a fallback key, which happens when a
		// seeder created the node independently on each instance. Nothing changed about it.
		if (Delta.Operation.REIDENTIFIED.equals(delta.getOperation())) {

			delta.setSignal(Signal.LOW, "same entity under a different uuid");
			return;
		}

		// A leading or trailing newline in a script or text body. Real - the exporter is
		// deterministic, so this IS a difference in the database - but rarely what a reader is
		// looking for. Reported, never suppressed.
		if (Delta.Detail.CONTENT_WHITESPACE_ONLY.equals(delta.getDetailCode())) {

			delta.setSignal(Signal.LOW, "whitespace only");
			return;
		}

		// Working notes, not application definition.
		if (Kind.SCRATCHPAD.equals(delta.getKind())) {

			delta.setSignal(Signal.LOW, "scratchpad, working data");
			return;
		}

		// deployment.conf records the build that wrote the export, so it differs whenever the two
		// sides were exported by different builds, which is most of the time.
		if (Kind.CONFIG_FILE.equals(delta.getKind()) && "deployment.conf".equals(delta.getKey())) {

			delta.setSignal(Signal.LOW, "build stamp");
			return;
		}

		delta.setSignal(Signal.NORMAL, null);
	}

	private Classifier() {}
}
