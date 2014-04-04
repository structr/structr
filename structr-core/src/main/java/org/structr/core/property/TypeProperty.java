package org.structr.core.property;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
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

			final Node dbNode = ((NodeInterface)obj).getNode();
			final Class type  = obj.getClass();

			for (final Class supertype : SearchCommand.typeAndAllSupertypes(type)) {
				dbNode.addLabel(DynamicLabel.label(supertype.getSimpleName()));
			}
		}
	}
}
