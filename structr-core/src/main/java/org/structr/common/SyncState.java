package org.structr.common;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author Christian Morgner
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
