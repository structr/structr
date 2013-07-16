package org.structr.core.entity;

import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class TestRelationship extends AbstractRelationship {
	
	public enum Relation {
		test_relationships
	}

	public static final Property<String>   startNodeId         = new StringProperty("startNodeId");
	public static final Property<String>   endNodeId           = new StringProperty("endNodeId");
	public static final Property<String[]> stringArrayProperty = new ArrayProperty<String>("stringArrayProperty", String.class);
	public static final Property<Boolean>  booleanProperty     = new BooleanProperty("booleanProperty").indexed();
	public static final Property<Double>   doubleProperty      = new DoubleProperty("doubleProperty").indexed();
	public static final Property<Integer>  integerProperty     = new IntProperty("integerProperty").indexed();
	public static final Property<Long>     longProperty        = new LongProperty("longProperty").indexed();
	public static final Property<String>   stringProperty      = new StringProperty("stringProperty").indexed();
	public static final Property<TestEnum> enumProperty        = new EnumProperty("enumProperty", TestEnum.class).indexed();

	static {
		
		EntityContext.registerNamedRelation(Relation.test_relationships.name(), TestRelationship.class, TestOne.class, TestFour.class, RelType.IS_AT);
	}
	
	@Override
	public PropertyKey getStartNodeIdKey() {
		return startNodeId;
	}

	@Override
	public PropertyKey getEndNodeIdKey() {
		return endNodeId;
	}
}
