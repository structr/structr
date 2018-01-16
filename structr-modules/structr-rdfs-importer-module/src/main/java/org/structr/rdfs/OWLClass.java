/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rdfs;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class OWLClass extends RDFItem<OWLClass> {

	private final Set<OWLProperty> properties = new TreeSet<>();
	private final Set<OWLClass> sourceClasses = new TreeSet<>();
	private final Set<OWLClass> targetClasses = new TreeSet<>();
	private final Set<String> domainIds       = new TreeSet<>();
	private final Set<String> rangeIds        = new TreeSet<>();
	private boolean isRelationship            = false;
	private boolean allowsInstances           = false;
	private boolean multipleOccurrences       = false;
	private boolean isMixedIn                 = false;
	private boolean oneWay                    = false;
	private boolean primary                   = false;
	private OWLClass actualSourceType         = null;
	private OWLClass actualTargetType         = null;
	private OWLClass inverse                  = null;
	private URI inverseId                     = null;
	protected String icon                     = null;

	public OWLClass(final Element element) {

		super(element);

		initialize();
	}

	@Override
	public String toString() {
		return getStructrName(true); // + (getSuperclass() != null ? " extends " + getSuperclass().toString() : "");
	}

	public String getIcon() {
		return icon;
	}

	public void addProperty(final OWLProperty property) {
		properties.add(property);
	}

	public Set<OWLProperty> getProperties() {
		return properties;
	}

	public Set<OWLProperty> getAllProperties() {

		final Set<OWLClass> typeAndSuperclasses = getTypeAndSuperclasses();
		final Set<OWLProperty> allProperties    = new TreeSet<>();

		for (final OWLClass type : typeAndSuperclasses) {
			allProperties.addAll(type.getProperties());
		}

		return allProperties;
	}


	public boolean hasSuperclass(final OWLClass type) {

		final OWLClass superclass = getSuperclass();
		if (superclass != null) {

			if (superclass.getId().equals(type.getId())) {
				return true;
			}

			return superclass.hasSuperclass(type);
		}

		return false;
	}

	public Set<String> getDomainIds() {

		if (domainIds.isEmpty() && getSuperclass() != null) {
			return getSuperclass().getDomainIds();
		}

		return domainIds;
	}

	public Set<String> getRangeIds() {

		if (rangeIds.isEmpty() && getSuperclass() != null) {
			return getSuperclass().getRangeIds();
		}
		return rangeIds;
	}

	public void resolveRelatedTypes(final Map<URI, OWLClass> classes) {

		for (final String domainId : getDomainIds()) {

			final URI uri     = URI.create(domainId);
			final OWLClass cl = classes.get(uri);
			if (cl != null) {

				sourceClasses.add(cl);

			} else {

				OWLParserv2.logger.println("No class found for " + domainId);
			}
		}

		for (final String rangeId : getRangeIds()) {

			final URI uri     = URI.create(rangeId);
			final OWLClass cl = classes.get(uri);
			if (cl != null) {

				targetClasses.add(cl);

			} else {

				OWLParserv2.logger.println("No class found for " + rangeId);
			}
		}
	}

	public void setIsRelationship(final boolean isRelationship) {
		this.isRelationship = isRelationship;
	}

	public boolean isRelationship() {
		return isRelationship;
	}

	// ----- methods from OWLProperty -----
	public void setSourceClass(final OWLClass sourceClass) {
		this.sourceClasses.add(sourceClass);
	}

	public void setTargetClass(final OWLClass targetClass) {
		this.targetClasses.add(targetClass);
	}

	public void resolveRelationshipTypes(final Map<URI, OWLClass> owlClasses) {

		if (inverseId != null) {

			inverse = owlClasses.get(inverseId);
		}

		this.actualSourceType = findCommonBaseType(sourceClasses);
		this.actualTargetType = findCommonBaseType(targetClasses);
	}

	public Set<OWLClass> getSourceClasses() {
		return sourceClasses;
	}

	public Set<OWLClass> getTargetClasses() {
		return targetClasses;
	}

	public OWLClass getInverse() {
		return inverse;
	}

	public URI getInverseOfId() {
		return inverseId;
	}

	public boolean multipleOccurrences() {
		return multipleOccurrences;
	}

	public List<OWLClass> getActualSourceTypes() {

		final List<OWLClass> actualSourceTypes = new LinkedList<>();
		if (actualSourceType != null) {

			actualSourceTypes.add(actualSourceType);

		} else {

			actualSourceTypes.addAll(findCommonBaseTypes(sourceClasses));
		}

		return actualSourceTypes;
	}

	public List<OWLClass> getActualTargetTypes() {

		final List<OWLClass> actualTargetTypes = new LinkedList<>();
		if (actualTargetType != null) {

			actualTargetTypes.add(actualTargetType);

		} else {

			actualTargetTypes.addAll(findCommonBaseTypes(targetClasses));

		}

		return actualTargetTypes;
	}

	public boolean hasInverse() {
		return inverse != null;
	}

	public boolean isPrimary() {
		return primary;
	}

	private OWLClass findCommonBaseType(final Set<OWLClass> source) {

		if (!source.isEmpty()) {

			if (source.size() > 1) {

				Set<OWLClass> commonSuperclasses = null;

				for (final OWLClass sourceClass : source) {

					final Set<OWLClass> superclasses = sourceClass.getTypeAndSuperclasses();
					if (commonSuperclasses == null) {

						// create initial set
						commonSuperclasses = new LinkedHashSet<>();
						commonSuperclasses.addAll(superclasses);

					} else {

						// intersect with existing set
						commonSuperclasses.retainAll(superclasses);
					}
				}

				// commonSuperclasses are sorted so the last
				// class is the lowest common supertype
				if (!commonSuperclasses.isEmpty()) {

					return commonSuperclasses.iterator().next();
				}

			} else {

				return source.iterator().next();
			}
		}

		return null;
	}

	private List<OWLClass> findCommonBaseTypes(final Set<OWLClass> source) {

		final List<OWLClass> commonBaseTypes = new LinkedList<>();

		if (!source.isEmpty()) {

			// first test: add all types
			commonBaseTypes.addAll(source);
		}

		return commonBaseTypes;
	}


	// ----- protected methods -----
	@Override
	protected Set<String> getInheritanceIdentifiers() {

		final Set<String> identifiers = new HashSet<>();

		identifiers.add("rdfs:subClassOf");
		identifiers.add("rdfs:subPropertyOf");

		return identifiers;
	}

	// ----- private methods -----
	private void initialize() {

		domainIds.addAll(getResourceIds("rdfs:domain"));
		rangeIds.addAll(getResourceIds("rdfs:range"));

		allowsInstances     = "true".equals(getValue(getFirstElement(getElement(), "krdf:allowsInstances")));
		multipleOccurrences = "true".equals(getValue(getFirstElement(getElement(), "krdf:multipleOccurrences")));
		isMixedIn           = "true".equals(getValue(getFirstElement(getElement(), "krdf:isMixedIn")));
		oneWay              = "true".equals(getValue(getFirstElement(getElement(), "krdf:oneWay")));
		icon                = getValue(getFirstElement(getElement(), "krdf:icon"));

		final Object isPrimaryValue = getValue(getFirstElement(getElement(), "krdf:primary"));
		if (isPrimaryValue == null || "true".equals(isPrimaryValue)) {

			primary = true;
		}


		final Node node = getFirstElement(getElement(), "owl:inverseOf");
		if (node != null) {

			final String inverse = getResourceId(node);
			if (inverse != null) {

				this.inverseId = URI.create(inverse);
			}
		}

	}
}
