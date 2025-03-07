/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.flow.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.module.api.DeployableEntity;

import java.util.Map;
import java.util.TreeMap;

public class FlowConstant extends FlowBaseNode implements DataSource, DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowConstant.class);

	public enum ConstantType {
		String,
		Boolean,
		Integer,
		Double,
		Date
	}

	public FlowConstant(Traits traits, NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getConstantType() {
		return wrappedObject.getProperty(traits.key("constantType"));
	}

	public Object getValue() {
		return wrappedObject.getProperty(traits.key("value"));
	}

	@Override
	public Object get(Context context) {

		final SecurityContext securityContext = getSecurityContext();
		final String cType                    = getConstantType();
		final Object val                      =  getValue();

		if (val != null) {

			try {

				PropertyConverter converter = null;

				if (cType != null) {

					switch (cType) {

						case "String":
							converter = new StringProperty("").inputConverter(securityContext);
							break;
						case "Boolean":
							converter = new BooleanProperty("").inputConverter(securityContext);
							break;
						case "Integer":
							converter = new IntProperty("").inputConverter(securityContext);
							break;
						case "Double":
							converter = new DoubleProperty("").inputConverter(securityContext);
							break;
						default:
							converter = new StringProperty("").inputConverter(securityContext);
					}

				}

				return converter != null ? converter.convert(val) : val;

			} catch (FrameworkException ex) {
				logger.warn("FlowConstant: Could not convert given value. " + ex.getMessage());
			}

		}

		return null;

	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("value",                       getValue());
		result.put("constantType",                getConstantType());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}
}
