/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import org.testng.annotations.Test;
import org.structr.core.graph.Tx;
import org.structr.flow.impl.FlowAction;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowReturn;

import java.util.HashMap;
import java.util.Map;
import org.structr.test.web.StructrUiTest;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

public class FlowTest extends StructrUiTest {

	@Test
	public void testFlowWithIterablesAndScripting() {

		final Map<String, Object> flowParameters         = new HashMap<>();
		Map<String,Object> resultMap                     = null;

		try (final Tx tx = app.tx()) {

			FlowContainer container = app.create(FlowContainer.class, "testFlow");

			resultMap = container.evaluate(flowParameters);

			assertNotNull(resultMap);
			assertNull(resultMap.get("result"));

			FlowAction action = app.create(FlowAction.class, "createAction");
			action.setProperty(FlowAction.script, "{ ['a','b','c'].forEach( data => Structr.create('User','name',data)) }");
			action.setProperty(FlowAction.flowContainer, container);

			FlowDataSource ds = app.create(FlowDataSource.class, "ds");
			ds.setProperty(FlowDataSource.query, "find('User')");
			ds.setProperty(FlowAction.flowContainer, container);

			FlowReturn ret = app.create(FlowReturn.class, "ds");
			ret.setProperty(FlowReturn.dataSource, ds);
			ret.setProperty(FlowAction.flowContainer, container);

			action.setProperty(FlowAction.next, ret);

			container.setProperty(FlowContainer.startNode, action);

			resultMap = container.evaluate(flowParameters);
			assertNotNull(resultMap);
			assertNotNull(resultMap.get("result"));

			ds.setProperty(FlowDataSource.query, "size(find('User'))");

			resultMap = container.evaluate(flowParameters);
			assertNotNull(resultMap);
			assertNotNull(resultMap.get("result"));

		} catch (Throwable ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}


	}

}
