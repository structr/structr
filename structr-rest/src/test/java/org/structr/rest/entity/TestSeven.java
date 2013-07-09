package org.structr.rest.entity;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionNotionProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class TestSeven extends AbstractNode {

	public static final CollectionProperty<TestSix> testSixs   = new CollectionProperty<TestSix>("testSixs", TestSix.class, TestRestRelType.HAS, Direction.INCOMING, true);
	public static final Property<List<String>>      testSixIds = new CollectionNotionProperty("testSixIds", testSixs, new PropertyNotion(GraphObject.uuid));
	
	public static final Property<String>            aString    = new StringProperty("aString").indexed().indexedWhenEmpty();
	public static final Property<Integer>           anInt      = new IntProperty("anInt").indexed();
	
	public static final View defaultView = new View(TestSeven.class, PropertyView.Public,
		name, testSixIds, aString, anInt
	);
}
