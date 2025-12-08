package org.structr.docs.ontology;

import java.util.*;

public class Topic extends Concept {

	private final List<UserInterfaceElement> userInterfaceElements = new LinkedList<>();
	private final List<Topic> topics                               = new LinkedList<>();
	private final List<Component> components                       = new LinkedList<>();
	private final List<Mechanism> mechanisms                       = new LinkedList<>();
	private final List<Endpoint> endpoints                         = new LinkedList<>();
	private final List<SystemType> types                           = new LinkedList<>();
	private final List<UseCase> useCases                           = new LinkedList<>();
	private final List<Action> actions                             = new LinkedList<>();
	private final List<Verb> verbs                                 = new LinkedList<>();

	Topic(final Root root, final String name) {

		super(root, name);
	}

	@Override
	public List<String> getFilteredDocumentationLines(Set<Details> details, int level) {
		return List.of();
	}

	public ScreenArea hasScreenArea(final String name) {

		final ScreenArea area = root.screenArea(name);
		userInterfaceElements.add(area);

		return area;
	}

	public Topic hasTopic(final String name) {

		final Topic topic = root.topic(name);
		topics.add(topic);

		return topic;
	}
}
