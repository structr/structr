package org.structr.docs.ontology;

import java.util.List;
import java.util.Set;

public class Action extends Concept {

	private final Verb verb;
	private final Concept concept;

	public Action(final Root root, final String name, final Verb verb, final Concept concept) {

		super(root, name);

		this.verb    = verb;
		this.concept = concept;
	}

	@Override
	public List<String> getFilteredDocumentationLines(Set<Details> details, int level) {
		return List.of();
	}
}
