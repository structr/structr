#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.entity;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.structr.common.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.Property;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.validator.TypeUniquenessValidator;
import ${package}.RelType;

public class Person extends AbstractNode {

	public static final Property<City>         city_base = new EntityProperty<City>("city_base", City.class, RelType.LIVES_IN, Direction.OUTGOING, true);
	public static final Property<String>       city      = new EntityNotionProperty("city", city_base, new PropertyNotion(AbstractNode.name));
	public static final Property<List<Person>> friends   = new CollectionProperty<Person>("friends", Person.class, RelType.KNOWS, Direction.BOTH, false);
	
	public static final View publicView = new View(Person.class, PropertyView.Public,
		name, city, friends
	);
	
	static {

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
