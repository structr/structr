/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates all information stored for Action-/SecurityContexts which are available via scripting
 */
public class ContextStore {

	protected Map<String, String> headers          = new HashMap<>();
	protected Map<String, Object> constants        = new HashMap<>();
	protected Map<String, Object> tmpStore         = new HashMap<>();
	protected Map<String, Date> timerStore         = new HashMap<>();
	protected Map<Integer, Integer> counters       = new HashMap<>();
	protected AdvancedMailContainer amc            = new AdvancedMailContainer();


	// --- Headers ---
	public void addHeader(final String key, final String value) {
		headers.put(key, value);
	}

	public Map<String, String> getHeaders() {
		return headers;
	}


	// --- Constants ---
	public Object getConstant(final String name) {
		return constants.get(name);
	}

	public void setConstant(final String name, final Object data) {
		constants.put(name, data);
	}


	// --- store() / retrieve() ---
	public void setParameters(Map<String, Object> parameters) {

		if (parameters != null) {
			this.tmpStore.putAll(parameters);
		}
	}

	public void store(final String key, final Object value) {
		tmpStore.put(key, value);
	}

	public Object retrieve(final String key) {
		return tmpStore.get(key);
	}

	public Map<String, Object> getAllVariables () {
		return tmpStore;
	}


	// --- Counters ---
	public void incrementCounter(final int level) {
		setCounter(level, getCounter(level) + 1);
	}

	public int getCounter(final int level) {

		Integer value = counters.get(level);
		if (value == null) {

			return 0;
		}

		return value;
	}

	public void setCounter(final int level, final int value) {
		counters.put(level, value);
	}

	public void resetCounter(final int level) {
		counters.put(level, 0);
	}


	// --- Timers ---
	public void addTimer(final String key) {
		timerStore.put(key, new Date());
	}

	public Date getTimer(final String key) {
		return timerStore.get(key);
	}

	public AdvancedMailContainer getAdvancedMailContainer () {
		return amc;
	}
}
