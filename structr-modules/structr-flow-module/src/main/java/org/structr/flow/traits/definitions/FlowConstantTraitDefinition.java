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
package org.structr.flow.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowConstant;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.Map;
import java.util.Set;

public class FlowConstantTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(FlowConstantTraitDefinition.class);

	public FlowConstantTraitDefinition() {
		super("FlowConstant");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowConstant constant           = node.as(FlowConstant.class);
					final SecurityContext securityContext = node.getSecurityContext();
					final String cType                    = constant.getConstantType();
					final Object val                      = constant.getValue();

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
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowConstant.class, (traits, node) -> new FlowConstant(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes("dataTarget", "FlowDataInput");
		final Property<String> value                       = new StringProperty("value");
		final Property<String> constantType                = new EnumProperty("constantType", FlowConstant.ConstantType.class);

		return newSet(
			dataTarget,
			value,
			constantType
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"value", "dataTarget", "constantType"
			),
			PropertyView.Ui,
			newSet(
				"value", "dataTarget", "constantType"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
