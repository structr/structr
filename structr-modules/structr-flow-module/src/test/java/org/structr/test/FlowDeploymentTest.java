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
import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

public class FlowDeploymentTest extends DeploymentTestBase {

	@Test
	public void testFlowDeploymentRoundtrip() {

		final Map<String, Object> flowParameters = new HashMap<>();
		final PropertyKey<String> nameKey        = Traits.of(StructrTraits.FLOW_CONTAINER).key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY);
		Iterable<Object> result                  = null;
		FlowContainer container                  = null;
		String containerUuid                     = null;

		try {

			try (final Tx tx = app.tx()) {

				container = app.create(StructrTraits.FLOW_CONTAINER, "testFlow").as(FlowContainer.class);

				container.setEffectiveName("flow.deployment.test");
				containerUuid = container.getUuid();

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

				ds.setQuery("find('User')");
				result = container.evaluate(securityContext, flowParameters);
				assertNotNull(result);

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				app.nodeQuery(StructrTraits.FLOW_CONTAINER).uuid(containerUuid).getFirst();

				doImportExportRoundtrip(true);

				tx.success();
			}

			// this is correct
			//doImportExportRoundtrip(true);

			try (final Tx tx = app.tx()) {

				container = app.nodeQuery(StructrTraits.FLOW_CONTAINER).key(Traits.of(StructrTraits.FLOW_CONTAINER).key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY), "flow.deployment.test").getFirst().as(FlowContainer.class);

				assertNotNull(container);
				result = container.evaluate(securityContext, flowParameters);
				assertNotNull(result);

				tx.success();
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}


	}

}
