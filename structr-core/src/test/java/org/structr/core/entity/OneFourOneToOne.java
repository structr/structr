package org.structr.core.entity;

import org.structr.core.property.ArrayProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class OneFourOneToOne extends OneToOne<TestOne, TestFour> {

	public static final Property<String>   startNodeId         = new StringProperty("startNodeId");
	public static final Property<String>   endNodeId           = new StringProperty("endNodeId");
	public static final Property<String[]> stringArrayProperty = new ArrayProperty<>("stringArrayProperty", String.class);
	public static final Property<Boolean>  booleanProperty     = new BooleanProperty("booleanProperty").indexed();
	public static final Property<Double>   doubleProperty      = new DoubleProperty("doubleProperty").indexed();
	public static final Property<Integer>  integerProperty     = new IntProperty("integerProperty").indexed();
	public static final Property<Long>     longProperty        = new LongProperty("longProperty").indexed();
	public static final Property<String>   stringProperty      = new StringProperty("stringProperty").indexed();
	public static final Property<TestEnum> enumProperty        = new EnumProperty("enumProperty", TestEnum.class).indexed();

	@Override
	public Class<TestOne> getSourceType() {
		return TestOne.class;
	}

	@Override
	public String name() {
		return "IS_AT";
	}

	@Override
	public Class<TestFour> getTargetType() {
		return TestFour.class;
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return startNodeId;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return endNodeId;
	}
}
