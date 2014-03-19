package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class CreateOperation implements InvertibleModificationOperation {

	private String treeIndex = null;
	private DOMNode parent   = null;
	private DOMNode newNode  = null;
	
	public CreateOperation(final String treeIndex, final DOMNode parent, final DOMNode newNode) {
		
		this.treeIndex = treeIndex;
		this.newNode   = newNode;
		this.parent    = parent;
	}

	public String getInsertIndex() {
		return treeIndex;
	}

	public DOMNode getNewNode() {
		return newNode;
	}

	@Override
	public String toString() {
		return "Create " + newNode + " at " + treeIndex;
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page page) throws FrameworkException {
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}
}
