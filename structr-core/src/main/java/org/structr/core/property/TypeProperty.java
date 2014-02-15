package org.structr.core.property;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.ConfigurationProvider;

/**
 *
 * @author Christian Morgner
 */
public class TypeProperty extends StringProperty {

	public TypeProperty() {
		
		super("type");
		
		readOnly();
		indexed();
		writeOnce();
	}
	
	@Override
	public void setProperty(SecurityContext securityContext, final GraphObject obj, String value) throws FrameworkException {
		
		super.setProperty(securityContext, obj, value);

		if (obj instanceof NodeInterface) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Node dbNode                  = ((NodeInterface)obj).getNode();
			final Class type                   = obj.getClass();

			for (Map.Entry<String, Class<? extends NodeInterface>> entity : config.getNodeEntities().entrySet()) {

				Class<? extends NodeInterface> entityClass = entity.getValue();

				if (entityClass.isAssignableFrom(type)) {

					dbNode.addLabel(DynamicLabel.label(entityClass.getSimpleName()));
				}
			}		

			for (final Class interf : config.getInterfacesForType(type)) {

				dbNode.addLabel(DynamicLabel.label(interf.getSimpleName()));
			}
		}
	}
}
