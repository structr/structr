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

import org.structr.diff.model.Entity;

/**
 * One difference between two exports.
 *
 * {@link Operation#REIDENTIFIED} is the one that needs explaining. It means the two sides hold the
 * same thing under different uuids, paired on the fallback key: the honest report for a node a
 * seeder created separately on each instance. Without it, that pair reads as a removal beside an
 * unrelated addition, which is how a textual diff describes it and part of why a textual diff
 * misleads here.
 */
public class Delta {

	public enum Operation {

		ADDED, REMOVED, CHANGED, RENAMED, MOVED, REIDENTIFIED
	}

	/**
	 * What the matcher actually compared, as a token rather than as prose.
	 *
	 * The human-readable detail says the same thing, but keying a classifier off a sentence makes
	 * the sentence load-bearing, and it will be reworded by someone improving the wording.
	 */
	public enum Detail {

		NAME, POSITION, ATTRIBUTES, CONTENT, CONTENT_WHITESPACE_ONLY, IDENTITY
	}

	/** How the two sides were paired. Reported, because a fallback match is weaker evidence. */
	public enum MatchedBy {

		KEY, ALTERNATE_KEY, UNMATCHED
	}

	private final String kind;
	private final String key;
	private final Operation operation;
	private final String name;
	private final String detail;
	private final String origin;
	private final MatchedBy matchedBy;
	private final Detail detailCode;
	private Signal signal = Signal.NORMAL;
	private String signalReason;

	public Delta(final String kind, final String key, final Operation operation, final String name, final Detail detailCode, final String detail, final String origin, final MatchedBy matchedBy) {

		this.kind       = kind;
		this.key        = key;
		this.operation  = operation;
		this.name       = name;
		this.detailCode = detailCode;
		this.detail     = detail;
		this.origin     = origin;
		this.matchedBy  = matchedBy;
	}

	public static Delta added(final Entity e) {

		return new Delta(e.getKind(), e.getKey(), Operation.ADDED, e.getName(), null, null, e.getOrigin(), MatchedBy.UNMATCHED);
	}

	public static Delta removed(final Entity e) {

		return new Delta(e.getKind(), e.getKey(), Operation.REMOVED, e.getName(), null, null, e.getOrigin(), MatchedBy.UNMATCHED);
	}

	public String getKind() {

		return kind;
	}

	public String getKey() {

		return key;
	}

	public Operation getOperation() {

		return operation;
	}

	public String getName() {

		return name;
	}

	public String getDetail() {

		return detail;
	}

	public String getOrigin() {

		return origin;
	}

	public Detail getDetailCode() {

		return detailCode;
	}

	public Signal getSignal() {

		return signal;
	}

	/** Why the classifier demoted it, or null when it did not. */
	public String getSignalReason() {

		return signalReason;
	}

	void setSignal(final Signal signal, final String reason) {

		this.signal       = signal;
		this.signalReason = reason;
	}

	public MatchedBy getMatchedBy() {

		return matchedBy;
	}

	@Override
	public String toString() {

		return operation + " " + kind + " " + (name != null ? name : key) + (detail != null ? " (" + detail + ")" : "");
	}
}
