#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import ${package}.RelType;
import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.property.GenericProperty;
import org.structr.common.property.Property;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.validator.TypeUniquenessValidator;

public class Person extends AbstractNode {

	public static final Property<City>         city    = new GenericProperty<City>("city");
	public static final Property<List<Person>> friends = new GenericProperty<List<Person>>("friends");
	
	public static final View publicView = new View(Person.class, PropertyView.Public,
		name, city, friends
	);
	
	static {
		
		// register friends relationship
		EntityContext.registerPropertyRelation(Person.class, friends, Person.class, RelType.KNOWS, Direction.BOTH, RelationClass.Cardinality.ManyToMany);

		// register entity relationship between City and Person on the "name" property
		EntityContext.registerEntityRelation(Person.class, City.class, RelType.LIVES_IN, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne, new PropertyNotion(name));

		// register type uniqueness validator
		EntityContext.registerPropertyValidator(Person.class, name, new TypeUniquenessValidator(Person.class));
		
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
