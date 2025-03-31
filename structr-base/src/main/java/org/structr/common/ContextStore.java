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
package org.structr.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.helper.AdvancedMailContainer;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.GenericProperty;

import java.util.*;

/**
 * Encapsulates all information stored for Action-/SecurityContexts which are available via scripting
 */
public class ContextStore {

	private final static Logger logger = LoggerFactory.getLogger(ContextStore.class);

	private Map<String, String> headers               = new HashMap<>();
	private Map<String, Object> constants             = new HashMap<>();
	private Map<String, Object> requestStore          = new HashMap<>();
	private Map<String, Object> tmpParameters         = new HashMap<>();
	private Map<String, Date> timerStore              = new HashMap<>();
	private Map<Integer, Integer> counters            = new HashMap<>();
	private AdvancedMailContainer amc                 = null;
	private ArrayList<GraphObjectMap> localizations   = new ArrayList<>();
	private Map<String, Object> functionPropertyCache = new HashMap<>();
	private boolean sortDescending                    = false;
	private String sortKey                            = null;
	private int queryRangeStart                       = -1;
	private int queryRangeEnd                         = -1;
	private boolean validateCertificates              = true;

	public ContextStore () {
	}

	public ContextStore (final ContextStore other) {

		this.headers      = other.headers;
		this.constants    = other.constants;
		this.requestStore = other.requestStore;
		this.timerStore   = other.timerStore;
		this.counters     = other.counters;
		this.amc          = other.amc;
	}

	// --- Headers ---
	public void addHeader(final String key, final String value) {
		headers.put(key, value);
	}

	public void removeHeader(final String key) {
		headers.remove(key);
	}

	public void clearHeaders() {
		headers.clear();
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	// --- Headers ---
	public void setValidateCertificates(final boolean validate) {
		validateCertificates = validate;
	}

	public boolean isValidateCertificates() {
		return validateCertificates;
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

	public boolean hasConstant(final String name) {
		return constants.containsKey(name);
	}

	// --- store() / retrieve() ---
	public void setTemporaryParameters(Map<String, Object> parameters) {

		this.tmpParameters = parameters;
	}

	public Set<String> getTemporaryParameterKeys() {
		return this.tmpParameters.keySet();
	}

	public Map<String, Object> getTemporaryParameters() {
		return this.tmpParameters;
	}

	public void clearTemporaryParameters() {
		this.tmpParameters.clear();
	}

	public void store(final String key, final Object value) {

		if (tmpParameters.containsKey(key)) {

			logger.info("Function store() was called for key \"" + key + "\", which is already used in the current context by a method parameter and won't be accessible. Consider using $.requestStore / request_store_put() to store data in the request context.");
		}

		requestStore.put(key, value);
	}

	public Object retrieve(final String key) {

		if (tmpParameters.containsKey(key)) {
			return tmpParameters.get(key);
		}

		return requestStore.get(key);
	}

	public void remove(final String key) { requestStore.remove(key);}

	public Map<String, Object> getRequestStore() {
		return requestStore;
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

		if (amc == null) {
			amc = new AdvancedMailContainer();
		}

		return amc;
	}

	// --- Localizations ---
	public void addRequestedLocalization(final Object node, final String key, final String domain, final String locale, final NodeInterface localization) {

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
