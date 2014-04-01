package org.structr.web.diff;

import java.util.List;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.w3c.dom.Node;

/**
 *
 * @author Christian Morgner
 */
public class MoveOperation extends InvertibleModificationOperation {

	private List<String> siblingHashes = null;
	private DOMNode originalNode       = null;
	private String parentHash          = null;
	private DOMNode newNode            = null;

	public MoveOperation(final Map<String, DOMNode> hashMappedExistingNodes, final String parentHash, final List<String> siblingHashes, final DOMNode newNode, final DOMNode originalNode) {

		super(hashMappedExistingNodes);

		this.siblingHashes = siblingHashes;
		this.originalNode  = originalNode;
		this.parentHash    = parentHash;
		this.newNode       = newNode;
	}

	@Override
	public String toString() {

		if (originalNode instanceof Content) {

			return "Move Content(" + originalNode.getIdHashOrProperty() + ")";

		} else {

			return "Move " + originalNode.getProperty(DOMElement.tag) + "(" + originalNode.getIdHashOrProperty() + ")";
		}
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page sourcePage, final Page newPage) throws FrameworkException {

		final InsertPosition insertPosition = findInsertPosition(sourcePage, parentHash, siblingHashes, newNode);
		if (insertPosition != null) {

			final DOMNode parent       = insertPosition.getParent();
			final DOMNode sibling      = insertPosition.getSibling();
			final Node originalSibling = originalNode.getNextSibling();

			if (sibling != null && sibling.equals(originalSibling)) {

				// nothing to be done
				return;
			}

			if (sibling == null && originalSibling == null) {
				return;
			}

			parent.insertBefore(originalNode, sibling);
		}
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}

	@Override
	public Integer getPosition() {

		// move operations should go after Delete and Create but before Update
		return 300;
	}
}
