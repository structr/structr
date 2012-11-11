#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import ${package}.RelType;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.property.Property;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.validator.TypeUniquenessValidator;

public class City extends AbstractNode {

	public static final Property<List<Person>> persons = new GenericProperty<List<Person>>("persons");
	
	public static final View publicView = new View(Person.class, PropertyView.Public,
		name, persons
	);
	
	static {
		
		// register entity relationship between City and Person
		EntityContext.registerEntityRelation(City.class, Person.class, RelType.LIVES_IN, Direction.INCOMING, Cardinality.OneToMany);
		
		// register type uniqueness validator
		EntityContext.registerPropertyValidator(City.class, name, new TypeUniquenessValidator(City.class));
		
	}
	
	@Override
	public boolean beforeCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.beforeCreation(securityContext, errorBuffer)) {
			
			return !ValidationHelper.checkPropertyNotNull(this, name, errorBuffer);
		}
		
		return false;
	}
	
	@Override
	public boolean beforeModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.beforeCreation(securityContext, errorBuffer)) {
			
			return !ValidationHelper.checkPropertyNotNull(this, name, errorBuffer);
		}
		
		return false;
	}
}
