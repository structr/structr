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
package org.structr.websocket.message;

import com.google.gson.JsonElement;
import org.apache.commons.lang3.BooleanUtils;
import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.GraphObjectTrait;

import java.util.*;

/**
 *
 */
public class WebSocketMessage {

	private SecurityContext securityContext        = null;
	private String button                          = null;
	private String callback                        = null;
	private int chunkSize                          = 512;
	private int code                               = 0;
	private String command                         = null;
	private GraphObjectTrait graphObject                 = null;
	private String id                              = null;
	private String pageId                          = null;
	private String message                         = null;
	private Map<String, Object> nodeData           = new LinkedHashMap();
	private Map<String, Object> commandConfig      = new LinkedHashMap();
	private int page                               = 1;
	private int pageSize                           = Integer.MAX_VALUE;
	private String parent                          = null;
	private Map<String, Object> relData            = new LinkedHashMap();
	private Set<PropertyKey> modifiedProperties    = new LinkedHashSet();
	private Set<PropertyKey> removedProperties     = new LinkedHashSet();
	private Iterable<? extends GraphObjectTrait> result  = null;
	private int rawResultCount                     = 0;
	private String sessionId                       = null;
	private boolean sessionValid                   = false;
	private String sortKey                         = null;
	private String sortOrder                       = null;
	private String view                            = null;
	private Set<String> nodesWithChildren          = null;
	private JsonElement jsonErrorObject            = null;

	public WebSocketMessage copy() {

		WebSocketMessage newCopy = new WebSocketMessage();

		newCopy.securityContext    = this.securityContext;
		newCopy.button             = this.button;
		newCopy.callback           = this.callback;
		newCopy.code               = this.code;
		newCopy.command            = this.command;
		newCopy.pageId             = this.pageId;
		newCopy.nodeData           = this.nodeData;
		newCopy.commandConfig      = this.commandConfig;
		newCopy.relData            = this.relData;
		newCopy.graphObject        = this.graphObject;
		newCopy.id                 = this.id;
		newCopy.message            = this.message;
		newCopy.modifiedProperties = this.modifiedProperties;
		newCopy.removedProperties  = this.removedProperties;
		newCopy.page               = this.page;
		newCopy.pageSize           = this.pageSize;
		newCopy.parent             = this.parent;
		newCopy.result             = this.result;
		newCopy.rawResultCount     = this.rawResultCount;
		newCopy.sessionId          = this.sessionId;
		newCopy.sessionValid       = this.sessionValid;
		newCopy.sortKey            = this.sortKey;
		newCopy.sortOrder          = this.sortOrder;
		newCopy.view               = this.view;
		newCopy.chunkSize          = this.chunkSize;
		newCopy.nodesWithChildren  = this.nodesWithChildren;
		newCopy.jsonErrorObject    = this.jsonErrorObject;

		return newCopy;
	}

	public void clear() {
		this.nodeData           = new LinkedHashMap();
		this.commandConfig      = new LinkedHashMap();
		this.modifiedProperties = new LinkedHashSet();
		this.removedProperties  = new LinkedHashSet();
		this.result             = null;
	}

	public String getCommand() {
		return command;
	}

	public String getId() {
		return id;
	}

	public String getPageId() {
		return pageId;
	}

	public Map<String, Object> getNodeData() {
		return nodeData;
	}

	public Map<String, Object> getCommandConfig() {
		return commandConfig;
	}

	public Map<String, Object> getRelData() {
		return relData;
	}

	public String getCallback() {
		return callback;
	}

	public String getButton() {
		return button;
	}

	public String getParent() {
		return parent;
	}

	public String getView() {
		return view;
	}

	public Iterable<? extends GraphObjectTrait> getResult() {
		return result;
	}

	public int getRawResultCount() {
		return rawResultCount;
	}

	public String getSortKey() {
		return sortKey;
	}

	public int getPageSize() {
		return pageSize;
	}

	public int getPage() {
		return page;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public String getMessage() {
		return message;
	}

	public int getCode() {
		return code;
	}

	public Set<PropertyKey> getModifiedProperties() {
		return modifiedProperties;
	}

	public Set<PropertyKey> getRemovedProperties() {
		return removedProperties;
	}

	public int getChunkSize() {
		return chunkSize;
	}

	public GraphObjectTrait getGraphObject() {
		return graphObject;
	}

	public String getSessionId() {
		return sessionId;
	}

	public boolean isSessionValid() {
		return sessionValid;
	}

	public Set<String> getNodesWithChildren() {
		return nodesWithChildren;
	}

	public JsonElement getJsonErrorObject() {
		return jsonErrorObject;
	}

	public String getRelDataStringValue(final String key) {
		return (String) getRelDataValue(key);
	}

	public Object getRelDataValue(final String key) {
		return getRelData().get(key);
	}

	public Object getNodeDataValue(final String key) {
		return getNodeData().get(key);
	}

	public boolean hasCommandConfigValue(final String key) {
		return getCommandConfig().containsKey(key);
	}

	public Object getCommandConfigValue(final String key) {
		return getCommandConfig().get(key);
	}

	public boolean getNodeDataBooleanValue(final String key) {
		final Object value = getNodeDataValue(key);
		return BooleanUtils.isTrue((Boolean) value);
	}

	public boolean getCommandConfigBooleanValue(final String key) {
		final Object value = getCommandConfigValue(key);
		return BooleanUtils.isTrue((Boolean) value);
	}

	public String getNodeDataStringValue(final String key) {
		return (String) getNodeDataValue(key);
	}

	public String getCommandConfigStringValue(final String key) {
		return (String) getCommandConfigValue(key);
	}

	public String getNodeDataStringValueTrimmedOrDefault(final String key, final String defaultValue) {
		final String value = getNodeDataStringValueTrimmed(key);

		if (value != null) {
			return value;
		}

		return defaultValue;
	}

	public String getNodeDataStringValueTrimmed(final String key) {

		final String value = getNodeDataStringValue(key);

		if (value != null) {
			return value.trim();
		}

		return "";
	}

	public Long getNodeDataLongValue(final String key) {
		final Object value = getNodeDataValue(key);

		if (value instanceof Number) {
			return ((Number)value).longValue();
		}

		if (value instanceof String) {
			try { return Long.parseLong(value.toString()); } catch (Throwable t) {}
		}

		return null;
	}

	public Integer getNodeDataIntegerValue(final String key) {
		final Object value = getNodeDataValue(key);

		if (value instanceof Number) {
			return ((Number)value).intValue();
		}

		if (value instanceof String) {
			try { return Integer.parseInt(value.toString()); } catch (Throwable t) {}
		}

		return -1;
	}

	public List<String> getNodeDataStringList(final String key) {
		return (List<String>) getNodeDataValue(key);
	}

	public void setCommand(final String command) {
		this.command = command;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setPageId(final String pageId) {
		this.pageId = pageId;
	}

	public void setNodeData(final String key, Object value) {
		nodeData.put(key, value);
	}

	public void setCommandConfig(final String key, Object value) {
		commandConfig.put(key, value);
	}

	public void setNodeData(final Map<String, Object> data) {
		this.nodeData.putAll(data);
	}

	public void setRelData(final String key, Object value) {
		relData.put(key, value);
	}

	public void setRelData(final Map<String, Object> data) {
		this.relData.putAll(data);
	}

	public void setCallback(final String callback) {
		this.callback = callback;
	}

	public void setButton(final String button) {
		this.button = button;
	}

	public void setParent(final String parent) {
		this.parent = parent;
	}

	public void setView(final String view) {
		this.view = view;
	}

	public void setResult(final Iterable<? extends GraphObjectTrait> result) {
		this.result = result;
	}

	public void setRawResultCount(final int rawResultCount) {
		this.rawResultCount = rawResultCount;
	}

	public void setSortKey(final String sortKey) {
		this.sortKey = sortKey;
	}

	public void setPageSize(final int pageSize) {
		this.pageSize = pageSize;
	}

	public void setPage(final int page) {
		this.page = page;
	}

	public void setSortOrder(final String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}

	public void setSessionValid(final boolean sessionValid) {
		this.sessionValid = sessionValid;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public void setCode(final int code) {
		this.code = code;
	}

	public void setModifiedProperties(final Set<PropertyKey> modifiedProperties) {
		this.modifiedProperties = modifiedProperties;
	}

	public void setRemovedProperties(final Set<PropertyKey> removedProperties) {
		this.removedProperties = removedProperties;
	}

	public void setChunkSize(final int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public void setGraphObject(final GraphObjectTrait graphObject) {
		this.graphObject = graphObject;
	}

	public void setNodesWithChildren(final Set<String> nodesWithChildren) {
		this.nodesWithChildren = nodesWithChildren;
	}

	public void setJsonErrorObject(final JsonElement jsonErrorObject) {
		this.jsonErrorObject = jsonErrorObject;
	}

	public void setSecurityContext(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public SecurityContext getSecurityContext() {
		return securityContext;
	}
}
