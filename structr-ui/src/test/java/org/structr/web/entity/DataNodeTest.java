package org.structr.web.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.web.common.StructrUiTest;
import org.structr.web.common.UiFactoryDefinition;

/**
 *
 * @author Christian Morgner
 */
public class DataNodeTest extends StructrUiTest {
	
	public void testDynamicDataNodeInstantiation() {
		
		EntityContext.registerFactoryDefinition(new UiFactoryDefinition());
		
		try {
			// create data node
			AbstractNode node = createTestNodes(DataNode.class.getSimpleName(), 1).get(0);
			String uuid       = node.getUuid();

			// should have a UUID by now
			assertNotNull(uuid);
			
			// should be DataNode
			assertEquals(DataNode.class, node.getClass());

			// set a "kind" property, should trigger dynamic compiler
			node.setProperty(DataNode.kind, "Unknown");
			
			// create a Type node with kind "Unknown"
			PropertyDefinition def = createTestNodes(PropertyDefinition.class, 1).get(0);
			def.setProperty(PropertyDefinition.kind, "Unknown");
			
			// search node
			Result<AbstractNode> result = Services.command(securityContext, SearchNodeCommand.class).execute(Search.andExactUuid(uuid));
			assertEquals(new Integer(1), result.getRawResultCount());
			
			AbstractNode foundNode = result.get(0);
			
			// should be of dynamic type
			assertEquals("org.structr.web.entity.dynamic.Unknown", foundNode.getClass().getName());
			
			
		} catch (FrameworkException fex) {
			
			fail("Unexpected exception: " + fex.toString());
		}
		
	}
}
