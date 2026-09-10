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

/**
 * How much attention a delta is asking for.
 *
 * Not a severity and not a filter. Every delta is reported whatever its signal; this only tells a
 * reader which ones can be collapsed behind a count by default. The distinction matters because the
 * alternative - dropping deltas server-side - has a failure mode nobody can see, whereas a collapsed
 * group is one click from being read.
 */
public enum Signal {

	/** Report it. Anything the classifier has no generic reason to demote. */
	NORMAL,

	/**
	 * Collapsible by default, for a reason that holds on any Structr instance.
	 *
	 * Deliberately NOT used for app-specific volatility. Which application-configuration rows are
	 * runtime state is a fact about one application, and baking that in would make this product
	 * carry one deployment's assumptions. Grouping in the UI covers that case instead.
	 */
	LOW
}
