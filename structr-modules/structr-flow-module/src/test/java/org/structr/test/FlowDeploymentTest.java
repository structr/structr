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
package org.structr.test;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.flow.impl.FlowAction;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowReturn;
import org.structr.flow.traits.definitions.FlowContainerTraitDefinition;
import org.structr.test.web.advanced.DeploymentTestBase;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

public class FlowDeploymentTest extends DeploymentTestBase {

	@Test
	public void testFlowDeploymentRoundtrip() {

		final Map<String, Object> flowParameters = new HashMap<>();
		final PropertyKey<String> nameKey        = Traits.of(StructrTraits.FLOW_CONTAINER).key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY);
		Object result                            = null;
		FlowContainer container                  = null;
		final String desiredEffectiveName = "flow.deployment.test";

		try {

			try (final Tx tx = app.tx()) {

				container = app.create(StructrTraits.FLOW_CONTAINER, "testFlow").as(FlowContainer.class);

				container.setEffectiveName(desiredEffectiveName);

				FlowAction action = app.create(StructrTraits.FLOW_ACTION, "createAction").as(FlowAction.class);
				action.setScript("{ ['a','b','c'].forEach( data => Structr.create('User','name',data)) }");
				action.setFlowContainer(container);

				FlowDataSource ds = app.create(StructrTraits.FLOW_DATA_SOURCE, "ds").as(FlowDataSource.class);
				ds.setFlowContainer(container);

				FlowReturn ret = app.create(StructrTraits.FLOW_RETURN, "ds").as(FlowReturn.class);
				ret.setDataSource(ds);
				ret.setFlowContainer(container);

				action.setNext(ret);

				container.setStartNode(action);

				ds.setQuery("join(extract(find('User', sort('name')), 'name'), ',')");
				result = container.evaluate(securityContext, flowParameters);
				assertEquals("a,b,c", result);

				tx.success();
			}

			doImportExportRoundtrip(true);

			try (final Tx tx = app.tx()) {

				container = app.nodeQuery(StructrTraits.FLOW_CONTAINER).key(Traits.of(StructrTraits.FLOW_CONTAINER).key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY), desiredEffectiveName).getFirst().as(FlowContainer.class);

				assertNotNull(container);
				result = container.evaluate(securityContext, flowParameters);
				assertEquals("a,b,c", result);

				tx.success();
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testFlowDeploymentRoundtripWhereFlowElementsHaveSlashesInTheirNames() {

		final Map<String, Object> flowParameters = new HashMap<>();
		final String desiredEffectiveName1 = "fl/ow.deploy/ment.te/st1.te/st2.te/st3.te/st4.with?funky(characters)";
		final String desiredEffectiveName2 = "fl/ow.deploy/ment.te/st1.te/st2.flow-at?different(path)";
		final List<String> desiredEffectiveNames = List.of(desiredEffectiveName1, desiredEffectiveName2);
		final int expectedNumberOfFlowContainers = 2;
		final int expectedNumberOfFlowContainerPackages = 6;

		try {

			for (final String effectiveName : desiredEffectiveNames) {

				try (final Tx tx = app.tx()) {

					final FlowContainer container = app.create(StructrTraits.FLOW_CONTAINER, "testFlow").as(FlowContainer.class);

					container.setEffectiveName(effectiveName);

					FlowAction action = app.create(StructrTraits.FLOW_ACTION, "createAction").as(FlowAction.class);
					action.setScript("""
						{
							['a','b','c'].forEach(data => $.getOrCreate('User', 'name', data))
						}""");
					action.setFlowContainer(container);

					FlowDataSource ds = app.create(StructrTraits.FLOW_DATA_SOURCE, "ds").as(FlowDataSource.class);
					ds.setFlowContainer(container);

					FlowReturn ret = app.create(StructrTraits.FLOW_RETURN, "ds").as(FlowReturn.class);
					ret.setDataSource(ds);
					ret.setFlowContainer(container);

					action.setNext(ret);

					container.setStartNode(action);

					ds.setQuery("join(extract(find('User', sort('name')), 'name'), ',')");
					assertEquals("a,b,c", container.evaluate(securityContext, flowParameters));

					tx.success();
				}
			}

			try (final Tx tx = app.tx()) {

				assertEquals("Expected number of flow container packages (BEFORE deployment roundtrip) does not match actual number", expectedNumberOfFlowContainerPackages, app.nodeQuery(StructrTraits.FLOW_CONTAINER_PACKAGE).getAsList().size());
				assertEquals("Expected number of flow containers (BEFORE deployment roundtrip) does not match actual number", expectedNumberOfFlowContainers, app.nodeQuery(StructrTraits.FLOW_CONTAINER).getAsList().size());

				tx.success();
			}

			doImportExportRoundtrip(true);

			try (final Tx tx = app.tx()) {

				assertEquals("Expected number of flow container packages (AFTER deployment roundtrip) does not match actual number", expectedNumberOfFlowContainerPackages, app.nodeQuery(StructrTraits.FLOW_CONTAINER_PACKAGE).getAsList().size());
				assertEquals("Expected number of flow containers (AFTER deployment roundtrip) does not match actual number", expectedNumberOfFlowContainers, app.nodeQuery(StructrTraits.FLOW_CONTAINER).getAsList().size());

				tx.success();
			}

			for (final String effectiveName : desiredEffectiveNames) {

				try (final Tx tx = app.tx()) {

					final FlowContainer container = app.nodeQuery(StructrTraits.FLOW_CONTAINER).key(Traits.of(StructrTraits.FLOW_CONTAINER).key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY), effectiveName).getFirst().as(FlowContainer.class);

					assertNotNull(container);
					assertEquals("a,b,c", container.evaluate(securityContext, flowParameters));

					tx.success();
				}
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
