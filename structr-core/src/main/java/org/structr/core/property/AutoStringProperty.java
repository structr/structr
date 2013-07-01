package org.structr.core.property;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 * A string property that lets the user create a value
 * when there is no value associated with it yet.
 * 
 * @author Christian Morgner
 */
public abstract class AutoStringProperty extends StringProperty {

	private static final Logger logger = Logger.getLogger(AutoStringProperty.class.getName());
	
	public AutoStringProperty(String name) {
		
		super(name);
		
		// mark this property as being passively indexed
		// so we can be sure it will be activated when
		// a new entity with this property is created
		passivelyIndexed();
	}
	
	public abstract String createValue(GraphObject entity);
	
	@Override
	public void index(GraphObject entity, Object value) {
		
		Object indexValue = value;
		
		if (indexValue == null) {
			
			indexValue = createValue(entity);
			if (indexValue != null) {
				
				try {
					entity.setProperty(this, (String)indexValue);
					
				} catch (FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to set value {0} on entity {1}: {2}", new Object[] { indexValue, entity, fex.getMessage() } );
				}
			}
		}
				
		super.index(entity, indexValue);
	}
}
