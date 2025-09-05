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
package org.structr.common;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 *
 */
public class SyncState {

	private final Set<Flag> flags = EnumSet.noneOf(Flag.class);

	public enum Flag {

		Pages, Files, Folders, Images, Schema
	}

	/**
	 *
	 * @param flag a SyncState flag
	 * @return whether this SyncState instance has the given flag set
	 */
	public boolean hasFlag(final Flag flag) {
		return flags.contains(flag);
	}

	/**
	 *
	 * @param hasFlags a list of SyncState flags
	 * @return whether this SyncState instance has all of the given flags set
	 */
	public boolean hasFlags(final Flag... hasFlags) {
		return flags.containsAll(Arrays.asList(hasFlags));
	}

	/**
	 * @return a SyncState instance with all possible flags
	 */
	public static SyncState all() {

		final SyncState syncState = new SyncState();

		syncState.flags.addAll(Arrays.asList(Flag.values()));

		return syncState;
	}

	/**
	 * @param flags a list of SyncState flags
	 * @return a SyncState instance with the given flags
	 */
	public static SyncState all(final Flag... flags) {

		final SyncState syncState = new SyncState();

		syncState.flags.addAll(Arrays.asList(flags));

		return syncState;
	}

	/**
	 *
	 * @param flag a SyncState flag
	 * @return a SyncState instance with the given flag
	 */
	public static SyncState one(final Flag flag) {

		final SyncState syncState = new SyncState();

		syncState.flags.add(flag);

		return syncState;
	}

	/**
	 *
	 * @param commaSeparatedNameList a comma-separated list of SyncState flag names
	 * @return a SyncState instance with the given flags
	 */
	public static SyncState fromString(final String commaSeparatedNameList) {

		final SyncState syncState = new SyncState();
		final String[] parts      = commaSeparatedNameList.split("[, ]+");
		final int length          = parts.length;

		for (int i=0; i<length; i++) {

			try {

				final String part = parts[i].trim();
				syncState.flags.add(Flag.valueOf(part));

			} catch (Throwable ignore) { }
		}

		return syncState;
	}
}
