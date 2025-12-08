package org.structr.docs.ontology;

import java.util.List;
import java.util.Set;

public class Mechanism extends Concept {

	Mechanism(final Root root, final String name) {
		super(root, name);
	}

	@Override
	public List<String> getFilteredDocumentationLines(Set<Details> details, int level) {
		return List.of();
	}
}
