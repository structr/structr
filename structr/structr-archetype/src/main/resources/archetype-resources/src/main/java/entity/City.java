#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import ${package}.RelType;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.validator.TypeUniquenessValidator;

public class City extends AbstractNode {

	public enum Key implements PropertyKey {
		
		name, persons
	}
	
	static {
		
		// register public property set for the "City" node
		EntityContext.registerPropertySet(City.class, PropertyView.Public, Key.values());

		// register entity relationship between City and Person
		EntityContext.registerEntityRelation(City.class, Person.class, RelType.LIVES_IN, Direction.INCOMING, Cardinality.OneToMany);
		
		// register type uniqueness validator
		EntityContext.registerPropertyValidator(City.class, Key.name, new TypeUniquenessValidator(City.class));
		
	}
	
	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.beforeCreation(securityContext, errorBuffer)) {
			
			return !ValidationHelper.checkPropertyNotNull(this, Key.name, errorBuffer);
		}
		
		return false;
	}
	
	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.beforeCreation(securityContext, errorBuffer)) {
			
			return !ValidationHelper.checkPropertyNotNull(this, Key.name, errorBuffer);
		}
		
		return false;
	}
}
