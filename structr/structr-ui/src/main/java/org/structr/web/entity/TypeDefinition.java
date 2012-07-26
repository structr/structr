package org.structr.web.entity;

import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;

/**
 *
 * @author Christian Morgner
 */
public class TypeDefinition extends AbstractNode {
	
	private static final Logger logger = Logger.getLogger(TypeDefinition.class.getName());
	
	public enum Key implements PropertyKey {
		validationExpression, validationErrorMessage
	}
	
	public enum HiddenKey implements PropertyKey {
		contents
	}
	
	static {
		
		EntityContext.registerPropertySet(TypeDefinition.class, PropertyView.All,    Key.values());
		EntityContext.registerPropertySet(TypeDefinition.class, PropertyView.Public, Key.values());
		
		EntityContext.registerEntityRelation(TypeDefinition.class, Content.class, RelType.IS_A, Direction.INCOMING, RelationClass.Cardinality.OneToMany);
	}
	
	public String getValidationExpression() {
		return getStringProperty(TypeDefinition.Key.validationExpression);
	}
	
	public String getValidationErrorMessage() {
		return getStringProperty(TypeDefinition.Key.validationErrorMessage);
	}
}
