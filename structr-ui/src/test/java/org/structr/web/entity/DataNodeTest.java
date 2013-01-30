package org.structr.web.entity;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.graph.CreateRelationshipCommand;
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
			// create node with unknown type
			AbstractNode node = createTestNodes("unknown", 1).get(0);
			String uuid       = node.getUuid();

			// should have a UUID by now
			assertNotNull(uuid);
			
			// should be GenericNode			
			assertEquals(GenericNode.class, node.getClass());
			
			// create and link to TypeDefinition node
			TypeDefinition def = createTestNodes(TypeDefinition.class, 1).get(0);
			Services.command(securityContext, CreateRelationshipCommand.class).execute(def, node, RelType.DEFINES_TYPE);
			
			// search node
			Result<AbstractNode> result = Services.command(securityContext, SearchNodeCommand.class).execute(Search.andExactUuid(uuid));
			assertEquals(new Integer(1), result.getRawResultCount());
			
			AbstractNode foundNode = result.get(0);
			
			// should be of type DataNode
			assertEquals(DataNode.class, foundNode.getClass());
			
			
		} catch (FrameworkException fex) {
			
			fail("Unexpected exception: " + fex.toString());
		}
		
	}
}
