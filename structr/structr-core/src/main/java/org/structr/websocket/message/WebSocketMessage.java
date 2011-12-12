/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.websocket.message;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class WebSocketMessage {

	private Map<String, String> data = new LinkedHashMap<String, String>();
	private Set<String> modifiedProperties = new LinkedHashSet<String>();
	private List<GraphObject> result = null;
	private GraphObject graphObject = null;
	private boolean sessionValid = false;
	private String sortOrder = null;
	private String callback = null;
	private String message = null;
	private String command = null;
	private String sortKey = null;
	private String button = null;
	private String parent = null;
	private String token = null;
	private String view = null;
	private String id = null;
	private int pageSize = 0;
	private int page = 0;
	private int code = 0;

	public void setCommand(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setData(String key, String value) {
		data.put(key, value);
	}

	public void setData(Map<String, String> data) {
		this.data.putAll(data);
	}

	public Map<String, String> getData() {
		return data;
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	public String getButton() {
		return button;
	}

	public void setButton(String button) {
		this.button = button;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	public List<GraphObject> getResult() {
		return result;
	}

	public void setResult(List<GraphObject> result) {
		this.result = result;
	}

	public String getSortKey() {
		return sortKey;
	}

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean isSessionValid() {
		return sessionValid;
	}

	public void setSessionValid(boolean sessionValid) {
		this.sessionValid = sessionValid;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public Set<String> getModifiedProperties() {
		return modifiedProperties;
	}

	public void setModifiedProperties(Set<String> modifiedProperties) {
		this.modifiedProperties = modifiedProperties;
	}

	/**
	 * @return the graphObject
	 */
	public GraphObject getGraphObject() {
		return graphObject;
	}

	/**
	 * @param graphObject the graphObject to set
	 */
	public void setGraphObject(GraphObject graphObject) {
		this.graphObject = graphObject;
	}
}
