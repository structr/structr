package org.structr.files.cmis.wrapper;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeMutability;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.structr.cmis.common.CMISExtensionsData;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.search.SearchCommand;
import org.structr.schema.ConfigurationProvider;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class StructrTypeWrapper extends CMISExtensionsData implements TypeDefinition, TypeMutability, TypeDefinitionContainer {

	private Class type = null;

	public StructrTypeWrapper(final Class type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		return type.getName().hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		return other != null && other.hashCode() == hashCode();
	}

	@Override
	public String getId() {
		return getLocalName();
	}

	@Override
	public String getLocalName() {
		return type.getSimpleName();
	}

	@Override
	public String getLocalNamespace() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return getLocalName();
	}

	@Override
	public String getQueryName() {
		return getLocalName();
	}

	@Override
	public String getDescription() {
		return getLocalName();
	}

	@Override
	public BaseTypeId getBaseTypeId() {

		if (Folder.class.isAssignableFrom(type)) {
			return BaseTypeId.CMIS_FOLDER;

		}

		return BaseTypeId.CMIS_DOCUMENT;
	}

	@Override
	public String getParentTypeId() {
		return type.getSuperclass().getSimpleName();
	}

	@Override
	public Boolean isCreatable() {
		return true;
	}

	@Override
	public Boolean isFileable() {
		return true;
	}

	@Override
	public Boolean isQueryable() {
		return true;
	}

	@Override
	public Boolean isFulltextIndexed() {
		return false;
	}

	@Override
	public Boolean isIncludedInSupertypeQuery() {
		return true;
	}

	@Override
	public Boolean isControllablePolicy() {
		return false;
	}

	@Override
	public Boolean isControllableAcl() {
		return false;
	}

	@Override
	public Map<String, PropertyDefinition<?>> getPropertyDefinitions() {

		final Map<String, PropertyDefinition<?>> properties = new LinkedHashMap<>();
		final TypeDefinitionFactory factory                 = TypeDefinitionFactory.newInstance();

		switch (getBaseTypeId()) {

			case CMIS_DOCUMENT:
				properties.putAll(factory.createDocumentTypeDefinition(CmisVersion.CMIS_1_1, getParentTypeId()).getPropertyDefinitions());
				break;

			case CMIS_FOLDER:
				properties.putAll(factory.createFolderTypeDefinition(CmisVersion.CMIS_1_1, getParentTypeId()).getPropertyDefinitions());
				break;
		}

		return properties;
	}

	@Override
	public TypeMutability getTypeMutability() {
		return this;
	}

	// ----- interface TypeMutability -----
	@Override
	public Boolean canCreate() {
		return true;
	}

	@Override
	public Boolean canUpdate() {
		return true;
	}

	@Override
	public Boolean canDelete() {
		return true;
	}

	// ----- interface TypeDefinitionContainer -----
	@Override
	public TypeDefinition getTypeDefinition() {
		return this;
	}

	@Override
	public List<TypeDefinitionContainer> getChildren() {

		final ConfigurationProvider config           = StructrApp.getConfiguration();
		final List<TypeDefinitionContainer> children = new LinkedList<>();

		for (final String subtype : SearchCommand.getAllSubtypesAsStringSet(type.getSimpleName(), true)) {

			final Class subclass = config.getNodeEntityClass(subtype);
			if (subclass != null && !subclass.getName().equals(type.getName())) {

				children.add(new StructrTypeWrapper(subclass));
			}
		}

		return children;
	}
}
