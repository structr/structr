/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.cron;

import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates information about a single time field of a CRON task.
 *
 *
 */
public class CronField {

	private boolean isWildcard = false;
	private List<Integer> list = null;
	private int start = 0;
	private int step = 0;
	private int end = 0;

	public CronField(int start, int end, int step) {
		this(start, end, step, false);
	}

	public CronField(int start, int end, int step, boolean isWildcard) {
		this.isWildcard = isWildcard;
		this.start = start;
		this.step = step;
		this.end = end;
	}

	public CronField(final List<Integer> values) {
		this.list = values;
	}

	public boolean isInside(int value) {

		if (isWildcard) {
			return true;
		}

		if (list != null) {
			return list.contains(value);
		}

		return value >= start && value <= end && ((value+start) % step) == 0;
	}

	public int getStartValue() {
		return start;
	}

	public int getEndValue() {
		return end;
	}

	public int getStep() {
		return step;
	}

	public List<Integer> getList() {
		return list;
	}

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		if (list != null) {

			for (final Iterator<Integer> it = list.iterator(); it.hasNext();) {

				buf.append(it.next());

				if (it.hasNext()) {
					buf.append(",");
				}
			}

		} else {

			if (start == end) {

				buf.append(start);

			} else if (step == 1) {

				buf.append(start);
				buf.append("-");
				buf.append(end);

			} else {

				buf.append(start);
				buf.append("-");
				buf.append(end);
				buf.append("/");
				buf.append(step);
			}
		}

		return buf.toString();
	}

	public boolean isIsWildcard() {
		return isWildcard;
	}

	public void setIsWildcard(boolean isWildcard) {
		this.isWildcard = isWildcard;
	}
}
