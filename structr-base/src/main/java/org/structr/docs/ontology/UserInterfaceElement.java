package org.structr.docs.ontology;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class UserInterfaceElement extends Concept {

	private final List<UserInterfaceElement> userInterfaceElements = new LinkedList<>();
	private final List<UseCase> useCases                           = new LinkedList<>();

	UserInterfaceElement(final Root root, final String name) {
		super(root, name);
	}

	@Override
	public List<String> getFilteredDocumentationLines(Set<Details> details, int level) {
		return List.of();
	}

	public DropdownMenu hasDropdownMenu(final String name) {

		final DropdownMenu menu = root.dropdownMenu(name);
		userInterfaceElements.add(menu);

		return menu;
	}

	public Button hasButton(final String name) {

		final Button button = root.button(name);
		userInterfaceElements.add(button);

		return button;
	}

	public UseCase implementsUseCase(final String name) {

		final UseCase useCase = root.useCase(name);
		useCases.add(useCase);

		return useCase;
	}

	public void implementsUseCase(final UseCase useCase) {
		useCases.add(useCase);
	}
}
