package org.structr.docs.ontology;

import org.structr.api.service.Feature;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Component extends Concept {

	private final List<Concept> concepts     = new LinkedList<>();
	private final List<Feature> features     = new LinkedList<>();
	private final List<Mechanism> mechanisms = new LinkedList<>();
	private final List<Endpoint> endpoints   = new LinkedList<>();

	Component(final Root root, final String name) {
		super(root, name);
	}

	@Override
	public List<String> getFilteredDocumentationLines(Set<Details> details, int level) {
		return List.of();
	}
}
