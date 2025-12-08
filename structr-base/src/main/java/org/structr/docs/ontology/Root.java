package org.structr.docs.ontology;

import java.util.*;

public class Root {

	private final Map<String, UserInterfaceElement> userInterfaceElements = new LinkedHashMap<>();
	private final Map<String, Topic> topics                               = new LinkedHashMap<>();
	private final Map<String, Component> components                       = new LinkedHashMap<>();
	private final Map<String, Mechanism> mechanisms                       = new LinkedHashMap<>();
	private final Map<String, Endpoint> endpoints                         = new LinkedHashMap<>();
	private final Map<String, SystemType> types                           = new LinkedHashMap<>();
	private final Map<String, UseCase> useCases                           = new LinkedHashMap<>();
	private final Map<String, Action> actions                             = new LinkedHashMap<>();
	private final Map<String, Verb> verbs                                 = new LinkedHashMap<>();

	public Topic topic(final String name) {
		return topics.computeIfAbsent(name, key -> new Topic(this, name));
	}

	public ScreenArea screenArea(final String name) {
		return (ScreenArea) userInterfaceElements.computeIfAbsent(name, key -> new ScreenArea(this, key));
	}

	public DropdownMenu dropdownMenu(final String name) {
		return (DropdownMenu) userInterfaceElements.computeIfAbsent(name, key -> new DropdownMenu(this, key));
	}

	public Dialog dialog(final String name) {
		return (Dialog) userInterfaceElements.computeIfAbsent(name, key -> new Dialog(this, key));
	}

	public Button button(final String name) {
		return (Button) userInterfaceElements.computeIfAbsent(name, key -> new Button(this, key));
	}

	public Component component(final String name) {
		return components.computeIfAbsent(name, key -> new Component(this, key));
	}

	public Mechanism mechanism(final String name) {
		return mechanisms.computeIfAbsent(name, key -> new Mechanism(this, key));
	}

	public Endpoint endpoint(final String name) {
		return endpoints.computeIfAbsent(name, key -> new Endpoint(this, key));
	}

	public SystemType systemType(final String name) {
		return types.computeIfAbsent(name, key -> new SystemType(this, key));
	}

	public UseCase useCase(final String name) {
		return useCases.computeIfAbsent(name, key -> new UseCase(this, key));
	}

	public Action action(final String name) {
		return actions.computeIfAbsent(name, key -> new Action(this));
	}

	public Verb verb(final String name) {
		return verbs.computeIfAbsent(name, key -> new Verb(this, key));
	}
}
