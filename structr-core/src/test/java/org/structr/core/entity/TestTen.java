package org.structr.core.entity;

import java.util.List;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;

/**
 * Test class for testing cascading delete with relationships
 * that reference the same type..
 *
 * @author Christian Morgner
 */
public class TestTen extends AbstractNode {

	public static final Property<TestTen> tenTenParent         = new StartNode<>("testTenParent", TenTenOneToMany.class);
	public static final Property<List<TestTen>> tenTenChildren = new EndNodes<>("testTenChildren", TenTenOneToMany.class);

	public static final Property<TestTen> testParent           = new StartNode<>("testParent", TenTenOneToOne.class);
	public static final Property<TestTen> testChild            = new EndNode<>("testChild", TenTenOneToOne.class);
}
