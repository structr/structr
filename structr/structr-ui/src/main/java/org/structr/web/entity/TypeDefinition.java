package org.structr.web.entity;

import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;

/**
 *
 * @author Christian Morgner
 */
public class TypeDefinition extends AbstractNode {
	
	private static final Logger logger = Logger.getLogger(TypeDefinition.class.getName());
	
	public static final Property<String> validationExpression   = new StringProperty("validationExpression");
	public static final Property<String> validationErrorMessage = new StringProperty("validationErrorMessage");
	public static final Property<String> converter              = new StringProperty("converter");
	public static final Property<String> converterDefaultValue  = new StringProperty("converterDefaultValue");
	
	public static final org.structr.common.View publicView = new org.structr.common.View(TypeDefinition.class, PropertyView.Public,
	    validationExpression, validationErrorMessage, converter, converterDefaultValue
	);
	
	static {
		
//		EntityContext.registerPropertySet(TypeDefinition.class, PropertyView.All,    Key.values());
//		EntityContext.registerPropertySet(TypeDefinition.class, PropertyView.Public, Key.values());
		
		EntityContext.registerEntityRelation(TypeDefinition.class, Content.class, RelType.IS_A, Direction.INCOMING, RelationClass.Cardinality.OneToMany);
	}
	
}
