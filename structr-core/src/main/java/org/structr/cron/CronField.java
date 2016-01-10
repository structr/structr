/**
 * Copyright (C) 2010-2016 Structr GmbH
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

/**
 * Encapsulates information about a single time field of a CRON task.
 *
 *
 */
public class CronField {

	private boolean isWildcard = false;
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

	public boolean isInside(int value) {
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

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		buf.append(start);
		buf.append("-");
		buf.append(end);
		buf.append("/");
		buf.append(step);

		return buf.toString();
	}

	public boolean isIsWildcard() {
		return isWildcard;
	}

	public void setIsWildcard(boolean isWildcard) {
		this.isWildcard = isWildcard;
	}
}
