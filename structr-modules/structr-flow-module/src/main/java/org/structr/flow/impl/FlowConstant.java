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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.*;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

public class FlowConstant extends FlowBaseNode implements DataSource, DeployableEntity {

	private static final Logger logger                              = LoggerFactory.getLogger(FlowConstant.class);
	public static final Property<Iterable<FlowBaseNode>> dataTarget = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<String> value                      = new StringProperty("value");
	public static final Property<ConstantType> constantType         = new EnumProperty<>("constantType", ConstantType.class);

	public static final View defaultView = new View(FlowDataSource.class, PropertyView.Public, value, dataTarget, constantType);
	public static final View uiView      = new View(FlowDataSource.class, PropertyView.Public, value, dataTarget, constantType);


	@Override
	public Object get(Context context) {

		ConstantType cType = getProperty(constantType);
		Object val =  getProperty(value);

		if (val != null) {

			try {

				PropertyConverter converter = null;

				if (cType != null) {

					switch (cType) {
						case String:
							converter = new StringProperty("").inputConverter(securityContext);
							break;
						case Boolean:
							converter = new BooleanProperty("").inputConverter(securityContext);
							break;
						case Integer:
							converter = new IntProperty("").inputConverter(securityContext);
							break;
						case Double:
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
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("value", this.getProperty(value));
		result.put("constantType", this.getProperty(constantType));
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	public enum ConstantType {
		String,
		Boolean,
		Integer,
		Double,
		Date
	}
}
