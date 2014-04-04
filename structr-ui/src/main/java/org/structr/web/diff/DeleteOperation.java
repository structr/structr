package org.structr.web.diff;

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class DeleteOperation extends InvertibleModificationOperation {

	private DOMNode existingNode = null;
	
	public DeleteOperation(final Map<String, DOMNode> hashMappedExistingNodes, final DOMNode existingNode) {
		
		super(hashMappedExistingNodes);
		
		this.existingNode = existingNode;
	}

	@Override
	public String toString() {
		
		if (existingNode instanceof Content) {
	
			return "Delete Content(" + existingNode.getIdHash() + ")";
		
		} else {
			
			return "Delete " + existingNode.getProperty(DOMElement.tag) + "(" + existingNode.getIdHash();
		}
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page sourcePage, final Page newPage) throws FrameworkException {
		app.delete(existingNode);
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}

	@Override
	public Integer getPosition() {
		
		// delete operations should go first
		return 100;
	}
}
