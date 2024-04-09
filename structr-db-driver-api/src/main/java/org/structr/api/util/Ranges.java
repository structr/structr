/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.api.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility class that parses a string-based range specification for
 * use in a {@link RangesIterator}.
 */
public class Ranges {

	private static final Logger logger = LoggerFactory.getLogger(Ranges.class);
	private List<Range> ranges         = new LinkedList<>();

	public Ranges(final String spec) {

		if (StringUtils.isBlank(spec)) {
			throw new IllegalArgumentException("Range specification must not be empty");
		}

		for (final String range : spec.trim().split("[,]+")) {

			if (StringUtils.isNotBlank(range)) {
				handleRange(range);
			}
		}
	}

	public boolean contains(final int index) {

		for (final Range range : ranges) {

			if (range.contains(index)) {
				return true;
			}
		}

		return false;
	}

	// ----- private methods -----
	private void handleRange(final String range) {

		final String[] parts = range.split("[\\-]+");
		switch (parts.length) {

			case 1:
				// no range
				final Integer singleNumber = toInt(parts[0]);
				if (singleNumber != null) {

					ranges.add(new Range(singleNumber, singleNumber));
				}
				break;

			case 2:
				// valid range
				final Integer first = toInt(parts[0]);
				final Integer last  = toInt(parts[1]);

				if (first != null && last != null) {

					ranges.add(new Range(first, last));

				} else {

					throw new IllegalArgumentException("Range must have two boundaries");
				}
				break;

			default:
				logger.warn("Invalid range specification, unable to parse {}, ignoring.", range);
		}
	}

	private Integer toInt(final String src) {

		try {

			return Integer.parseInt(src.trim());

		} catch (NumberFormatException nex) {
			logger.warn("Invalid range specification, unable to parse {}, ignoring.", src);
		}

		return null;
	}

	// ----- nested classes -----
	private class Range {

		private int first = 0;
		private int last  = 0;

		public Range(final int first, final int last) {

			if (first < 0 || last < 0) {
				throw new IllegalArgumentException("Range boundary must not be negative");
			}

			if (first > last) {
				throw new IllegalArgumentException("Range boundaries must be in ascending order");
			}

			this.first = first;
			this.last  = last;
		}

		public boolean contains(final int number) {
			return (number >= first && number <= last);
		}
	}
}