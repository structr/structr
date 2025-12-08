package org.structr.docs.ontology;

import java.util.List;
import java.util.Set;

public class Verb extends Concept {

	@Override
	public List<String> getFilteredDocumentationLines(final Set<Details> details, final int level) {
		return List.of();
	}

	public Verb(final Root root, final String name) {
		super(root, name);
	}
}
