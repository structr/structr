package org.structr.app;

import org.structr.app.entity.Friend;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.search.Search;

/**
 *
 * @author Christian Morgner
 */
public class AppTest {
	
	public static void main(String[] args) {
		
		try {

			final App app = App.getInstance();

			app.beginTx();
			
			Friend me  = app.create(Friend.class, "chrisi");
			
			
			app.findNodes(Friend.class, Search.andExactType(Friend.class), Search.andExactName("chrisi"));
			
			
			
			app.commitTx();
			
			
			// stop Structr
			// app.shutdown();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void testCreate() throws Exception {
		
		final App app = App.getInstance();
		
		Friend me  = app.create(Friend.class, new NodeAttribute(AbstractNode.name, "chrisi"));
		
		// Friend me2 = app.find
	}

	public void testGetInstance_SecurityContext() {
	}

	public void testGetInstance_String_String() throws Exception {
	}

	public void testGetInstance_0args() {
	}
}
