package org.structr.core;

import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;

/**
 *
 * @author Christian Morgner
 */
public interface Incoming<T extends NodeInterface, S extends NodeInterface> extends RelationshipInterface {
}
