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
public class UpdateOperation extends InvertibleModificationOperation {

	private DOMNode existingNode = null;
	private DOMNode newNode      = null;

	public UpdateOperation(final Map<String, DOMNode> hashMappedExistingNodes, final DOMNode existingNode, final DOMNode newNode) {

		super(hashMappedExistingNodes);

		this.existingNode = existingNode;
		this.newNode      = newNode;
	}

	@Override
	public String toString() {

		if (existingNode instanceof Content) {

			return "Update Content(" + existingNode.getIdHashOrProperty() + ") with " + newNode.getIdHashOrProperty();

		} else {

			return "Update " + newNode.getProperty(DOMElement.tag) + "(" + existingNode.getIdHashOrProperty() + ") with " + newNode.getIdHashOrProperty();
		}
	}

	// ----- interface InvertibleModificationOperation -----
	@Override
	public void apply(final App app, final Page sourcePage, final Page newPage) throws FrameworkException {
		existingNode.updateFromNode(newNode);
	}

	@Override
	public InvertibleModificationOperation revert() {
		return null;
	}

	@Override
	public Integer getPosition() {

		// update operations should go last
		return 400;
	}
}
