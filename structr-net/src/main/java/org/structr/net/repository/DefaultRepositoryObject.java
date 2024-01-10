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
package org.structr.net.repository;

import org.structr.net.data.time.PseudoTime;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *
 */
public class DefaultRepositoryObject implements RepositoryObject {

	private final SortedMap<PseudoTime, PossibleValue> history = new ConcurrentSkipListMap<>();
	private final List<ObjectListener> listeners               = new LinkedList<>();
	private DefaultRepository parent                           = null;
	private PseudoTime creationTime                            = null;
	private PseudoTime deletionTime                            = null;
	private String deviceId                                    = null;
	private String userId                                      = null;
	private String type                                        = null;
	private String uuid                                        = null;

	public DefaultRepositoryObject(final DefaultRepository parent, final String uuid, final String type, final String deviceId, final String userId, final PseudoTime creationTime) {

		this.creationTime = creationTime;
		this.parent       = parent;
		this.deviceId     = deviceId;
		this.userId       = userId;
		this.type         = type;
		this.uuid         = uuid;
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof RepositoryObject) {
			return hashCode() == other.hashCode();
		}

		return false;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getDeviceId() {
		return deviceId;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	@Override
	public PseudoTime getCreationTime() {
		return creationTime;
	}

	@Override
	public PseudoTime getLastModificationTime() {

		if (!history.isEmpty()) {

			return history.lastKey();
		}

		return creationTime;
	}

	public void printHistory() {

		for (final PseudoTime time : history.keySet()) {
			System.out.println("            " + time + ": " + history.get(time));
		}

		if (!history.isEmpty()) {
			System.out.println("            " + history.firstKey() + ": " + getProperties(history.firstKey()));
			System.out.println("            " + history.lastKey() + ": " + getProperties(history.lastKey()));
		}
	}

	@Override
	public void setProperty(final PseudoTime instant, final String transactionId, final String key, final Object value) {

		final Object existingValue = getProperty(instant, transactionId, key);
		if (existingValue != null && existingValue.equals(value)) {

			return;
		}

		final PossibleValue possibleValue = history.get(instant);
		if (possibleValue != null) {

			if (possibleValue.hasValue(key)) {

				System.out.println("###### instant: " + instant.toString());
				System.out.println("REDEFINE: " + uuid + ": " + key + " = " + value + ", NOT set! " + instant + ": " + history.get(instant).getData());
				// redefinition of existing value!

			} else {

				// redefinition solved
				// TODO: check existing values from previous lookups (educing a value)
				possibleValue.set(key, value);
			}

		} else {

			final DefaultPossibility poss = parent.getPossibility(transactionId);
			if (poss != null) {

				poss.addObject(this);
			}

			history.put(instant, new PossibleValue(transactionId, key, value));
		}
	}

	public Object getProperty(final PseudoTime instant, final String key) {
		return getProperty(instant, null, key);
	}

	@Override
	public Object getProperty(final PseudoTime instant, final String transactionId, final String key) {

		PossibleValue value = history.get(instant);

		// optimistic best cas first
		if (value != null && value.isComplete(transactionId) && value.hasValue(key)) {

			return value.get(key);

		} else {

			// do backwards search
			final List<PseudoTime> times = new LinkedList<>(history.keySet());
			Collections.reverse(times);

			for (final PseudoTime time : times) {

				if (time.equals(instant) || time.before(instant)) {

					final PossibleValue val = history.get(time);
					if (val.isComplete(transactionId) && val.hasValue(key)) {

						return val.get(key);

					} else if (val.isAborted()) {

						history.remove(time);
					}
				}
			}
		}

		// nothing found, no values committed yet
		return null;
	}

	@Override
	public Map<String, Object> getProperties(final PseudoTime instant) {
		return getProperties(instant, null);
	}

	@Override
	public Map<String, Object> getProperties(final PseudoTime instant, final String transactionId) {

		final Map<String, Object> map = new HashMap<>();

		// do backwards search
		final List<PseudoTime> times = new LinkedList<>(history.keySet());
		Collections.reverse(times);

		for (final PseudoTime time : times) {

			if (time.equals(instant) || time.before(instant)) {

				final PossibleValue val = history.get(time);
				if (val.isComplete(transactionId)) {

					for (final Entry<String, Object> entry : val.getData().entrySet()) {

						if (!map.containsKey(entry.getKey())) {

							map.put(entry.getKey(), entry.getValue());
						}
					}

				} else if (val.isAborted()) {

					// remove aborted history entries
					history.remove(time);
				}
			}
		}

		return map;
	}

	@Override
	public void onCommit(final String transactionId) {

		final List<PossibleValue> values = new LinkedList<>();

		for (final PossibleValue val : history.values()) {

			if (transactionId != null && transactionId.equals(val.getTransactionId())) {

				values.add(val);
			}
		}

		// notify listeners
		for (final ObjectListener listener : listeners) {

			for (final PossibleValue value : values) {

				for (final Entry<String, Object> entry : value.getData().entrySet()) {

					listener.onPropertyChange(this, entry.getKey(), entry.getValue());
				}
			}
		}
	}

	@Override
	public void addListener(ObjectListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ObjectListener listener) {
		listeners.remove(listener);
	}

	// ----- nested classes -----
	private class PossibleValue {

		private Map<String, Object> data = new HashMap<>();
		private String transactionId     = null;

		public PossibleValue(final String transactionId, final String key, final Object value) {

			this.transactionId = transactionId;
			data.put(key, value);
		}

		@Override
		public String toString() {
			return transactionId;
		}

		public String getTransactionId() {
			return transactionId;
		}

		public boolean isComplete(final String otherTransactionId) {

			// static possibility
			if (transactionId == null) {
				return true;
			}

			// the same possibility will always get the "new" valuie
			if (transactionId.equals(otherTransactionId)) {
				return true;
			}

			final DefaultPossibility p = parent.getPossibility(transactionId);
			if (p != null) {

				return p.isComplete();
			}

			return false;
		}

		public boolean isAborted() {

			// static possibility
			if (transactionId == null) {
				return false;
			}

			final DefaultPossibility p = parent.getPossibility(transactionId);
			if (p != null) {

				return p.isAborted();
			}

			return false;
		}

		public boolean hasValue(final String key) {
			return data.containsKey(key);
		}

		public Object get(final String key) {
			return data.get(key);
		}

		public void set(final String key, final Object value) {
			data.put(key, value);
		}

		public Map<String, Object> getData() {
			return data;
		}
	}
}