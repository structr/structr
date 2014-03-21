package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class UpdateOperation extends InvertibleModificationOperation {

	private DOMNode existingNode = null;
	private DOMNode newNode      = null;
	
	public UpdateOperation(final DOMNode existingNode, final DOMNode newNode) {
		
		this.existingNode = existingNode;
		this.newNode      = newNode;
	}

	@Override
	public String toString() {
		return "Update " + existingNode.getIdHashOrProperty() + " with " + newNode.getIdHashOrProperty();
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page page) throws FrameworkException {
		existingNode.updateFrom(newNode);
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}
}
