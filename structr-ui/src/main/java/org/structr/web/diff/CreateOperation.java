package org.structr.web.diff;

import java.util.List;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;

/**
 *
 * @author Christian Morgner
 */
public class CreateOperation extends InvertibleModificationOperation {

	private List<String> siblingHashes = null;
	private String parentHash          = null;
	private DOMNode newNode            = null;
	private int depth                  = 0;

	public CreateOperation(final Map<String, DOMNode> hashMappedExistingNodes, final String parentHash, final List<String> siblingHashes, final DOMNode newNode, final int depth) {

		super(hashMappedExistingNodes);

		this.siblingHashes = siblingHashes;
		this.parentHash  = parentHash;
		this.newNode     = newNode;
		this.depth       = depth;
	}

	@Override
	public String toString() {

		if (newNode instanceof Content) {

			return "Create Content(" + newNode.getIdHashOrProperty() + ")";

		} else {

			return "Create " + newNode.getProperty(DOMElement.tag) + "(" + newNode.getIdHashOrProperty() + ")";
		}
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page sourcePage, final Page newPage) throws FrameworkException {

		final InsertPosition insertPosition = findInsertPosition(sourcePage, parentHash, siblingHashes, newNode);
		if (insertPosition != null) {

			final DOMNode parent  = insertPosition.getParent();
			final DOMNode sibling = insertPosition.getSibling();

			if (parent != null) {

				sourcePage.adoptNode(newNode);
				parent.insertBefore(newNode, sibling);

				// make existing node known to other operations
				hashMappedExistingNodes.put(newNode.getIdHashOrProperty(), newNode);

				// remove children of new node so that existing nodes can be moved later
				for (final DOMChildren childRel : newNode.getChildRelationships()) {
					app.delete(childRel);
				}
			}
		}
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}

	@Override
	public Integer getPosition() {

		// create perations should go after Delete but before Move
		return 200 + depth;
	}
}
