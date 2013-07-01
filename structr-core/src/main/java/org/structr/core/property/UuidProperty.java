package org.structr.core.property;

import org.structr.core.graph.NodeService;
import org.structr.core.validator.GlobalPropertyUniquenessValidator;
import org.structr.core.validator.SimpleRegexValidator;

/**
 *
 * @author Christian Morgner
 */
public class UuidProperty extends StringProperty {

	public UuidProperty() {
		
		super("uuid", new GlobalPropertyUniquenessValidator(), new SimpleRegexValidator("[a-zA-Z0-9]{32}"));

		indexed();
		readOnly();
		writeOnce();

		// add uuid indices
		relationshipIndices.add(NodeService.RelationshipIndex.rel_uuid);
		nodeIndices.add(NodeService.NodeIndex.uuid);
	}
}
