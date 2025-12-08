package org.structr.docs.ontology;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class UseCase extends Concept {

	private final List<Action> actions = new LinkedList<>();

	UseCase(final Root root, final String name) {

		super(root, name);
	}

	@Override
	public List<String> getFilteredDocumentationLines(Set<Details> details, int level) {
		return List.of();
	}

	public Action createsType(final SystemType type) {

		final Action creates = root.action("Creates Type");
	}
}
