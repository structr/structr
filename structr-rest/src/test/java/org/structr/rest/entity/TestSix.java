package org.structr.rest.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionNotionProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class TestSix extends AbstractNode {

	public static final EntityProperty<TestSeven>     testSeven        = new EntityProperty<TestSeven>("testSeven", TestSeven.class, TestRestRelType.HAS, true);
	public static final Property<String>              testSevenName    = new EntityNotionProperty("testSevenName", testSeven, new PropertyNotion(TestSeven.name));
	
	public static final CollectionProperty<TestEight> testEights       = new CollectionProperty<TestEight>("testEights", TestEight.class, TestRestRelType.HAS, false);
	public static final Property<List<Integer>>       testEightInts    = new CollectionNotionProperty("testEightInts", testEights, new PropertyNotion(TestEight.anInt));
	public static final Property<List<String>>        testEightStrings = new CollectionNotionProperty("testEightStrings", testEights, new PropertyNotion(TestEight.aString));
	    
	public static final Property<String>              aString          = new StringProperty("aString").indexed();
	public static final Property<Integer>             anInt            = new IntProperty("anInt").indexed();
	
	public static final View defaultView = new View(TestSix.class, PropertyView.Public,
		name, testSevenName, testEightInts, testEightStrings, aString, anInt
	);
	
}
