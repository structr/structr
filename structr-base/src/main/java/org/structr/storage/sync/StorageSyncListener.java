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
package org.structr.storage.sync;

import java.util.List;

/**
 * Callback handle passed to a {@link StorageSynchronizer}. The instance is
 * pre-bound to one sync target (sync root + storage configuration), so
 * events only carry relative virtual paths and/or node UUIDs.
 *
 * Threading contract: all methods may be called from any provider thread.
 * They never block on graph transactions; the sync service only enqueues.
 * Events are debounced and coalesced centrally, so duplicate or rapid-fire
 * events are acceptable and cheap.
 */
public interface StorageSyncListener {

	/**
	 * Report a single observed change.
	 */
	void onEvent(final ExternalChangeEvent event);

	/**
	 * Report a batch of observed changes.
	 */
	default void onEvents(final List<ExternalChangeEvent> events) {

		events.forEach(this::onEvent);
	}

	/**
	 * Signal that events may have been lost (e.g. a WatchService OVERFLOW
	 * or a reconnect after a network outage). The sync service schedules a
	 * full reconciliation scan for this target.
	 */
	void requestFullScan();
}
