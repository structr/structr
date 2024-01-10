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
import org.structr.flow.impl.FlowAction;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowReturn;
import org.structr.test.web.advanced.DeploymentTestBase;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

public class FlowDeploymentTest extends DeploymentTestBase {

	@Test
	public void testFlowDeploymentRoundtrip() {

		final Map<String, Object> flowParameters         = new HashMap<>();
		Iterable<Object> result                          = null;
		FlowContainer container                          = null;
		String containerUuid                             = null;

		try {

			try (final Tx tx = app.tx()) {

				container = app.create(FlowContainer.class, "testFlow");

				container.setProperty(FlowContainer.effectiveName, "flow.deployment.test");
				containerUuid = container.getUuid();

				FlowAction action = app.create(FlowAction.class, "createAction");
				action.setProperty(FlowAction.script, "{ ['a','b','c'].forEach( data => Structr.create('User','name',data)) }");
				action.setProperty(FlowAction.flowContainer, container);

				FlowDataSource ds = app.create(FlowDataSource.class, "ds");
				ds.setProperty(FlowAction.flowContainer, container);

				FlowReturn ret = app.create(FlowReturn.class, "ds");
				ret.setProperty(FlowReturn.dataSource, ds);
				ret.setProperty(FlowAction.flowContainer, container);

				action.setProperty(FlowAction.next, ret);

				container.setProperty(FlowContainer.startNode, action);

				ds.setProperty(FlowDataSource.query, "find('User')");
				result = container.evaluate(securityContext, flowParameters);
				assertNotNull(result);

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				app.nodeQuery(FlowContainer.class).uuid(containerUuid).getFirst();

				doImportExportRoundtrip(true);

				tx.success();
			}

			// this is correct
			//doImportExportRoundtrip(true);

			try (final Tx tx = app.tx()) {

				container = app.nodeQuery(FlowContainer.class).and(FlowContainer.effectiveName, "flow.deployment.test").getFirst();

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
