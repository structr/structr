/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.engine;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.schema.action.ActionContext;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class Context {

	private Map<String, Object> data        = new HashMap<>();
	private Map<String, Object> store       = new HashMap<>();
	private Map<String, Object> parameters  = new HashMap<>();
	private Map<String, Object> currentData = new HashMap<>();
	private Queue<Future> forkPromises      = new ConcurrentLinkedQueue<>();
	private GraphObject thisObject          = null;
	private Object result                   = null;
	private FlowError error                 = null;

	public Context() {}

	public Context(final Context context) {

		this.thisObject   = context.thisObject;
		this.data         = deepCopyMap(context.data);
		this.store        = deepCopyMap(context.store);
		this.parameters   = deepCopyMap(context.parameters);
		this.currentData  = deepCopyMap(context.currentData);
		this.forkPromises = deepCopyQueue(context.forkPromises);
	}

	public Context(final GraphObject thisObject) {
		this.thisObject = thisObject;
	}

	public Context(final GraphObject thisObject, final Map<String,Object> data) {
		this.thisObject = thisObject;
		this.data = data;
	}

	public GraphObject getThisObject() {
		return thisObject;
	}

	public void error(final FlowError error) {
		this.error = error;
	}

	public void clearError() {

		this.error = null;
	}

	public void setResult(final Object result) {
		this.result = result;
	}

	public Object getResult() {
		return result;
	}

	public FlowError getError() {
		return error;
	}

	public boolean hasResult() {
		return result != null;
	}

	public boolean hasError() {
		return error != null;
	}

	public void setData(final String key, final Object value) {
		this.data.put(key, value);
	}

	public Object getData(final String key) {
		return this.data.get(key);
	}

	public boolean hasData(final String key) {
		return this.data.containsKey(key);
	}

	public void setParameter(final String key, final Object value) { this.parameters.put(key,value); }

	public void setParameters(final Map<String,Object> parameters) { this.parameters = parameters; }

	public Object getParameter(final String key) {
		return this.parameters.get(key);
	}

	public Object retrieveFromStore(final String key) {
		return store.get(key);
	}

	public void setAggregation(final String key, final Object value) { this.currentData.put(key, value); }

	public Object getAggregation(final String key) { return this.currentData.get(key); }

	public Map<String,Object> getAggregations() { return deepCopyMap(this.currentData); }

	public void setAggregations(final Map<String,Object> aggregations) { this.currentData = aggregations; }

	public void putIntoStore(final String key, final Object value) { store.put(key,value); }

	public Set<String> getStoreKeySet() {
		return this.store.keySet();
	}

	public void queueForkFuture(final Future forkFuture) {
		this.forkPromises.add(forkFuture);
	}

	public Queue<Future> getForkFutures() {
		return this.forkPromises;
	}

	public ActionContext getActionContext(final SecurityContext securityContext, final FlowBaseNode node) {

		final ActionContext ctx = new ActionContext(securityContext);
		final String uuid       = node.getUuid();

		ctx.setDisableVerboseExceptionLogging(true);

		if(this.data.get(uuid) != null) {

			ctx.setConstant("data", this.data.get(uuid));

		} else {

			ctx.setConstant("data", null);
		}

		if(this.currentData.get(uuid) != null) {

			ctx.setConstant("currentData", this.currentData.get(uuid));

		} else {

			ctx.setConstant("currentData", null);
		}

		return ctx;
	}

	private <Q> Queue<Q> deepCopyQueue(final Queue<Q> q) {

		final Queue<Q> newQ = new ConcurrentLinkedQueue<>();
		for (Q o : q) {

			newQ.add(o);
		}

		return newQ;
	}

	private Map<String, Object> deepCopyMap(final Map<String,Object> map) {

		final Map<String,Object> result = new HashMap<>();

		for(Map.Entry<String, Object> entry : map.entrySet()) {

			if (entry.getValue() instanceof Map) {
				result.put(entry.getKey(), deepCopyMap((Map)entry.getValue()));

			} else if (entry.getValue() instanceof List) {

				result.put(entry.getKey(), deepCopyList((List)entry.getValue()));

			} else {

				result.put(entry.getKey(), entry.getValue());
			}

		}

		return result;
	}

	private List<Object> deepCopyList(List<Object> list) {

		final List<Object> result = new ArrayList<>();

		for (Object o : list) {
			result.add(o);
		}

		return result;
	}

}
