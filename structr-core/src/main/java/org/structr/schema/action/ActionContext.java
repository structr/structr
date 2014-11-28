/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

package org.structr.schema.action;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Ownership;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class ActionContext {

	protected Map<String, Object> tmpStore   = new LinkedHashMap<>();
	protected Map<Integer, Integer> counters = new LinkedHashMap<>();
	protected ErrorBuffer errorBuffer        = new ErrorBuffer();
	protected GraphObject parent             = null;
	protected Object data                    = null;

	public ActionContext() {
		this(null, null);
	}

	public ActionContext(final ActionContext other) {
		this.tmpStore = other.tmpStore;
		this.counters = other.counters;
		this.parent = other.parent;
		this.errorBuffer = other.errorBuffer;
		this.data = other.data;
	}

	public ActionContext(final ActionContext other, final GraphObject parent, final Object data) {
		this.tmpStore = other.tmpStore;
		this.counters = other.counters;
		this.parent = other.parent;
		this.errorBuffer = other.errorBuffer;

		this.data = data;
	}

	public ActionContext(final GraphObject parent, final Object data) {
		this.parent = parent;
		this.data   = data;
	}

	public boolean returnRawValue(final SecurityContext securityContext) {
		return false;
	}

	public Object getReferencedProperty(final SecurityContext securityContext, final GraphObject entity, final String refKey) throws FrameworkException {

		final String DEFAULT_VALUE_SEP = "!";
		final String[] parts           = refKey.split("[\\.]+");
		String referenceKey            = parts[parts.length - 1];
		String defaultValue            = null;
		Object _data                   = null;

		if (StringUtils.contains(referenceKey, DEFAULT_VALUE_SEP)) {

			String[] ref = StringUtils.split(referenceKey, DEFAULT_VALUE_SEP);
			referenceKey = ref[0];
			if (ref.length > 1) {

				defaultValue = ref[1];

			} else {

				defaultValue = "";
			}
		}

		// walk through template parts
		for (int i = 0; (i < parts.length); i++) {

			String part = parts[i];

			if (_data != null && _data instanceof GraphObject) {


				// special keyword "_source", works on relationship entities only
				if ("_source".equals(part) && _data instanceof RelationshipInterface) {

					_data = ((RelationshipInterface)_data).getSourceNode();
					continue;
				}

				// special keyword "_target", works on relationship entities only
				if ("_target".equals(part) && _data instanceof RelationshipInterface) {

					_data = ((RelationshipInterface)_data).getTargetNode();
					continue;
				}

				PropertyKey referenceKeyProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), part);
				_data                            = ((GraphObject)_data).getProperty(referenceKeyProperty);

				if (_data == null) {

					// check for default value
					if (defaultValue != null && StringUtils.contains(refKey, "!")) {

						return numberOrString(defaultValue);
					}

					// Need to return null here to avoid _data sticking to the (wrong) parent object
					return null;

				}

			}

			// special keyword "size"
			if (i > 0 && "size".equals(part) && _data instanceof List) {
				return ((List)_data).size();
			}

			// special keyword "now":
			if ("now".equals(part)) {

				return new Date();
			}

			// special keyword "me"
			if ("me".equals(part)) {

				Principal me = (Principal) securityContext.getUser(false);

				if (me != null) {

					_data = me;

					continue;
				}

			}

			// special boolean keywords
			if ("true".equals(part)) {
				return true;
			}

			if ("false".equals(part)) {
				return false;
			}

			// the following keywords work only on root level
			// so that they can be used as property keys for data objects
			if (_data == null) {

				// special keyword "parent":
				if ("parent".equals(part)) {

					_data = parent;

					if (parts.length == 1) {
						return _data;
					}

					continue;
				}

				// details data object id
				if ("id".equals(part)) {

					return entity.getUuid();
				}

				// special keyword "this"
				if ("this".equals(part)) {

					_data = entity;

					if (parts.length == 1) {
						return _data;
					}

					continue;

				}

				// special keyword "data"
				if ("data".equals(part)) {

					_data = data;

					if (parts.length == 1) {
						return _data;
					}

					continue;

				}

				// special keyword "element"
				if ("element".equals(part)) {

					_data = entity;

					if (parts.length == 1) {
						return _data;
					}

					continue;

				}

				// special keyword "owner"
				if (entity instanceof NodeInterface && "owner".equals(part)) {

					Ownership rel = ((NodeInterface)entity).getIncomingRelationship(PrincipalOwnsNode.class);
					if (rel != null) {

						_data = rel.getSourceNode();

						if (parts.length == 1) {
							return _data;
						}
					}

					continue;

				}

			}

		}

		return _data;
	}

	public void raiseError(final String type, final ErrorToken errorToken) {
		errorBuffer.add(type, errorToken);
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public boolean hasError() {
		return errorBuffer.hasError();
	}

	public void incrementCounter(final int level) {

		Integer value = counters.get(level);
		if (value == null) {

			value = 0;
		}

		counters.put(level, value+1);
	}

	public int getCounter(final int level) {

		Integer value = counters.get(level);
		if (value == null) {

			return 0;
		}

		return value;
	}

	public void resetCounter(final int level) {
		counters.put(level, 0);
	}

	public void store(final String key, final Object value) {
		tmpStore.put(key, value);
	}

	public Object retrieve(final String key) {
		return tmpStore.get(key);
	}

	protected Object numberOrString(final String value) {

		if (NumberUtils.isNumber(value)) {
			return NumberUtils.createNumber(value);
		}

		return value;
	}
}
