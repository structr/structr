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
package org.structr.console.tabcompletion;

/**
 *
 */
public class TabCompletionResult implements Comparable<TabCompletionResult> {

	private String command    = null;
	private String completion = null;
	private String suffix     = " ";

	public TabCompletionResult(final String command, final String completion, final String suffix) {

		this.command    = command;
		this.completion = completion;
		this.suffix     = suffix;
	}

	/**
	 * @return the full command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return the part of the command that completes the command
	 */
	public String getCompletion() {
		return completion;
	}

	/**
	 * @return the suffix that is to be appended after the command
	 */
	public String getSuffix() {
		return suffix;
	}

	// ----- interface Comparable<TabCompletionResult> -----
	@Override
	public int compareTo(final TabCompletionResult o) {
		return getCommand().compareTo(o.getCommand());
	}
}
