package org.structr.core.traits.wrappers;

import org.structr.core.entity.Person;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;

public class PersonTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements Person {

	public PersonTraitWrapper(Traits traits, NodeInterface node) {
		super(traits, node);
	}
}
