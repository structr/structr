package org.structr.docs.ontology;

import com.headius.invokebinder.transform.Drop;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ScreenArea extends UserInterfaceElement {

	ScreenArea(final Root root, final String name) {
		super(root, name);
	}

	@Override
	public List<String> getFilteredDocumentationLines(Set<Details> details, int level) {
		return List.of();
	}
}
