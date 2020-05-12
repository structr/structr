/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.*;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.Localization;
import org.structr.core.property.GenericProperty;

/**
 * Encapsulates all information stored for Action-/SecurityContexts which are available via scripting
 */
public class ContextStore {

	protected Map<String, String> headers               = new HashMap<>();
	protected Map<String, Object> constants             = new HashMap<>();
	protected Map<String, Object> tmpStore              = new HashMap<>();
	protected Map<String, Date> timerStore              = new HashMap<>();
	protected Map<Integer, Integer> counters            = new HashMap<>();
	protected AdvancedMailContainer amc                 = new AdvancedMailContainer();
	protected ArrayList<GraphObjectMap> localizations   = new ArrayList<>();
	protected Map<String, Object> functionPropertyCache = new HashMap<>();
	protected boolean sortDescending                    = false;
	protected String sortKey                            = null;
	protected int queryRangeStart                       = -1;
	protected int queryRangeEnd                         = -1;

	public ContextStore () {
	}

	public ContextStore (final ContextStore other) {

		this.headers     = other.headers;
		this.constants   = other.constants;
		this.tmpStore    = other.tmpStore;
		this.timerStore  = other.timerStore;
		this.counters    = other.counters;
		this.amc         = other.amc;
	}

	// --- Headers ---
	public void addHeader(final String key, final String value) {
		headers.put(key, value);
	}

	public void removeHeader(final String key) {
		headers.remove(key);
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

	public Set<String> getConstantKeys() {
		return constants.keySet();
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

	public void remove(final String key) { tmpStore.remove(key);}

	public Map<String, Object> getAllVariables () {
		return tmpStore;
	}

	// --- Function Properties ---
	public void storeFunctionPropertyResult(final String uuid, final String propertyName, final Object value) {
		this.functionPropertyCache.put(contextCacheKey(uuid, propertyName), value);
	}

	public Object retrieveFunctionPropertyResult(final String uuid, final String propertyName) {
		return this.functionPropertyCache.get(contextCacheKey(uuid, propertyName));
	}

	public void clearFunctionPropertyCache() {
		this.functionPropertyCache.clear();
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

	// --- Localizations ---
	public void addRequestedLocalization(final Object node, final String key, final String domain, final String locale, final Localization localization) {

		final GenericProperty keyKey    = new GenericProperty("key");
		final GenericProperty domainKey = new GenericProperty("domain");
		final GenericProperty localeKey = new GenericProperty("locale");
		final GenericProperty nodeKey   = new GenericProperty("node");

		boolean notContained = true;
		for (GraphObject gom : localizations) {
			if (notContained) {

				if (gom.getProperty(keyKey).equals(key) && gom.getProperty(domainKey).equals(domain) && gom.getProperty(localeKey).equals(locale)) {

					final GraphObject prevNode = (GraphObject)gom.getProperty(nodeKey);
					if (prevNode != null && node != null && prevNode.getUuid().equals(((GraphObject)node).getUuid())) {
						notContained = false;
					}
				}
			}
		}

		if (notContained) {

			final Map<String, Object> data = new HashMap();
			data.put("node", node);
			data.put("key", key);
			data.put("domain", domain);
			data.put("locale", locale);
			data.put("localization", localization);

			GraphObjectMap converted = GraphObjectMap.fromMap(data);

			if (!localizations.contains(converted)) {
				localizations.add(converted);
			}
		}
	}

	public ArrayList<GraphObjectMap> getRequestedLocalizations () {

		return localizations;

	}

	// ----- query configuration
	public void setRangeStart(final int start) {
		this.queryRangeStart = start;
	}

	public void setRangeEnd(final int end) {
		this.queryRangeEnd = end;
	}

	public void setSortKey(final String sortKey) {
		this.sortKey = sortKey;
	}

	public void setSortDescending(final boolean descending) {
		this.sortDescending = descending;
	}

	public int getRangeStart() {
		return queryRangeStart;
	}

	public int getRangeEnd() {
		return queryRangeEnd;
	}

	public String getSortKey() {
		return sortKey;
	}

	public boolean getSortDescending() {
		return sortDescending;
	}

	public void resetQueryParameters() {

		sortKey         = null;
		sortDescending  = false;
		queryRangeStart = -1;
		queryRangeEnd   = -1;
	}


	// ----- private methods -----
	private String contextCacheKey(final String uuid, final String propertyName) {
		return uuid + "." + propertyName;
	}
}
