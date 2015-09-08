package org.structr.files.cmis;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.MutableTypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.files.cmis.config.StructrRepositoryInfo;
import org.structr.files.cmis.wrapper.StructrTypeWrapper;
import org.structr.schema.ConfigurationProvider;

/**
 *
 * @author Christian Morgner
 */
public class CMISRepositoryService extends AbstractStructrCmisService implements RepositoryService {

	private static final Logger logger = Logger.getLogger(CMISRepositoryService.class.getName());

	private final RepositoryInfo repositoryInfo = new StructrRepositoryInfo();

	public CMISRepositoryService(final SecurityContext securityContext) {

		super(securityContext);
	}

	@Override
	public List<RepositoryInfo> getRepositoryInfos(final ExtensionsData extension) {

		final List<RepositoryInfo> infoList = new LinkedList<>();

		infoList.add(repositoryInfo);

		return infoList;
	}

	@Override
	public RepositoryInfo getRepositoryInfo(String repositoryId, ExtensionsData extension) {

		if (repositoryId != null && repositoryId.equals(repositoryInfo.getId())) {
			return repositoryInfo;
		}

		throw new CmisObjectNotFoundException(repositoryId);
	}

	@Override
	public TypeDefinitionList getTypeChildren(String repositoryId, String typeId, Boolean includePropertyDefinitions, BigInteger maxItems, BigInteger skipCount, ExtensionsData extension) {

		// important: children are the direct children of a type, as opposed to the descendants

		final Set<TypeDefinition> results  = new LinkedHashSet<>();
		final ConfigurationProvider config = StructrApp.getConfiguration();

		if (typeId != null) {

			switch (typeId) {

				case "cmis:document":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_DOCUMENT, includePropertyDefinitions));
					break;

				case "cmis:folder":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_FOLDER, includePropertyDefinitions));
					break;

				case "cmis:item":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_ITEM, includePropertyDefinitions));
					break;

				case "cmis:policy":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_POLICY, includePropertyDefinitions));
					break;

				case "cmis:relationship":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_RELATIONSHIP, includePropertyDefinitions));
					break;

				case "cmis:secondary":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_SECONDARY, includePropertyDefinitions));
					break;

				default:

					logger.log(Level.SEVERE, "default case reached for typeId {0}", typeId);

					final Class parentType = config.getNodeEntityClass(typeId);
					if (parentType != null) {

						results.addAll(getTypeDefinitions(config, typeId, false, includePropertyDefinitions));

					} else {

						throw new CmisNotSupportedException("Type with ID " + typeId + " does not exist");
					}
			}

		} else {

			results.add(getDocumentTypeDefinition(includePropertyDefinitions));
			results.add(getFolderTypeDefinition(includePropertyDefinitions));
			results.add(getItemTypeDefinition(includePropertyDefinitions));
			results.add(getPolicyTypeDefinition(includePropertyDefinitions));
			results.add(getRelationshipTypeDefinition(includePropertyDefinitions));
			results.add(getSecondaryTypeDefinition(includePropertyDefinitions));
		}

		final TypeDefinitionListImpl returnValue = new TypeDefinitionListImpl();
		final List<TypeDefinition> unpagedList   = new LinkedList<>(results);
		final int resultSize                     = unpagedList.size();
		final List<TypeDefinition> pagedList     = applyPaging(unpagedList, maxItems, skipCount);
		int skip                                 = 0;

		if (skipCount != null) {
			skip = skipCount.intValue();
		}

		returnValue.setNumItems(BigInteger.valueOf(resultSize));
		returnValue.setList(pagedList);
		returnValue.setHasMoreItems(resultSize > pagedList.size() + skip);

		return returnValue;
	}

	@Override
	public List<TypeDefinitionContainer> getTypeDescendants(String repositoryId, String typeId, BigInteger depth, Boolean includePropertyDefinitions, ExtensionsData extension) {

		// important: descendants are ALL children and children of children, i.e. the whole tree

		/*
		Id typeId: The typeId of an object-type speciﬁed in the repository.
		 - If speciﬁed, then the repository MUST return all of descendant types of the speciﬁed type.
		 - If not speciﬁed, then the Repository MUST return all types and MUST ignore the value of the depth parameter.
		*/


		final Set<TypeDefinitionContainer> results = new LinkedHashSet<>();
		final ConfigurationProvider config          = StructrApp.getConfiguration();

		if (typeId != null) {

			switch (typeId) {

				case "cmis:document":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_DOCUMENT, includePropertyDefinitions));
					break;

				case "cmis:folder":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_FOLDER, includePropertyDefinitions));
					break;

				case "cmis:item":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_ITEM, includePropertyDefinitions));
					break;

				case "cmis:policy":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_POLICY, includePropertyDefinitions));
					break;

				case "cmis:relationship":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_RELATIONSHIP, includePropertyDefinitions));
					break;

				case "cmis:secondary":
					results.addAll(getTypeDescendants(BaseTypeId.CMIS_SECONDARY, includePropertyDefinitions));
					break;

				default:

					final Class parentType = config.getNodeEntityClass(typeId);
					if (parentType != null) {

						results.addAll(getTypeDefinitions(config, typeId, true, includePropertyDefinitions));

					} else {

						throw new CmisNotSupportedException("Type with ID " + typeId + " does not exist");
					}
					break;
			}

		} else {

			// defaults
			results.add(this.wrap(getDocumentTypeDefinition(includePropertyDefinitions), includePropertyDefinitions));
			results.add(this.wrap(getFolderTypeDefinition(includePropertyDefinitions), includePropertyDefinitions));
			results.add(this.wrap(getItemTypeDefinition(includePropertyDefinitions), includePropertyDefinitions));
			results.add(this.wrap(getPolicyTypeDefinition(includePropertyDefinitions), includePropertyDefinitions));
			results.add(this.wrap(getRelationshipTypeDefinition(includePropertyDefinitions), includePropertyDefinitions));
			results.add(this.wrap(getSecondaryTypeDefinition(includePropertyDefinitions), includePropertyDefinitions));

		}

		return new LinkedList<>(results);
	}

	@Override
	public TypeDefinition getTypeDefinition(final String repositoryId, final String typeId, final ExtensionsData extension) {

		switch (typeId) {

			case "cmis:document":
				return getDocumentTypeDefinition(true);

			case "cmis:folder":
				return getFolderTypeDefinition(true);

			case "cmis:item":
				return getItemTypeDefinition(true);

			case "cmis:policy":
				return getPolicyTypeDefinition(true);

			case "cmis:relationship":
				return getRelationshipTypeDefinition(true);

			case "cmis:secondary":
				return getSecondaryTypeDefinition(true);

		}

		final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeId);
		if (type != null) {

				return new StructrTypeWrapper(type, true);
		}

		throw new CmisObjectNotFoundException("Type with ID " + typeId + " does not exist");
	}

	@Override
	public TypeDefinition createType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return null;
	}

	@Override
	public TypeDefinition updateType(String repositoryId, TypeDefinition type, ExtensionsData extension) {
		return null;
	}

	@Override
	public void deleteType(String repositoryId, String typeId, ExtensionsData extension) {
	}

	// ----- private methods -----
	private Set<StructrTypeWrapper> getTypeDefinitions(final ConfigurationProvider config, final String typeId, final boolean recursive, final boolean includePropertyDefinitions) {

		final Set<String> types               = SearchCommand.getAllSubtypesAsStringSet(typeId, !recursive);
		final Set<StructrTypeWrapper> results = new LinkedHashSet<>();

		for (final String type : types) {

			if (!type.equals(typeId)) {

				final Class clazz = config.getNodeEntityClass(type);
				if (clazz != null) {

					results.add(new StructrTypeWrapper(clazz, includePropertyDefinitions));
				}
			}
		}

		return results;
	}

	private Set<StructrTypeWrapper> getTypeDescendants(final BaseTypeId baseTypeId, final boolean includePropertyDefinitions) {

		final Set<StructrTypeWrapper> classes = new LinkedHashSet<>();

		for (final Class<? extends NodeInterface> type : StructrApp.getConfiguration().getNodeEntities().values()) {

			try {

				if (baseTypeId.equals(type.newInstance().getCMISInfo().getBaseTypeId())) {
					classes.add(new StructrTypeWrapper(type, includePropertyDefinitions));
				}

			} catch (Throwable ignore) {}
		}

		return classes;
	}

	private TypeDefinitionContainer wrap(final MutableTypeDefinition typeDefinition, final boolean includePropertyDefinitions) {

		final TypeDefinitionContainerImpl impl    = new TypeDefinitionContainerImpl(typeDefinition);
		final List<TypeDefinitionContainer> list = new LinkedList<>();

		list.addAll(getTypeDescendants(typeDefinition.getBaseTypeId(), includePropertyDefinitions));
		impl.setChildren(list);

		return impl;
	}

	private MutableTypeDefinition getSecondaryTypeDefinition(final boolean includePropertyDefinitions) {

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		final MutableTypeDefinition def     = factory.createBaseSecondaryTypeDefinition(CmisVersion.CMIS_1_1);

		def.setIsCreatable(false);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableTypeDefinition getRelationshipTypeDefinition(final boolean includePropertyDefinitions) {

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		final MutableTypeDefinition def     = factory.createBaseRelationshipTypeDefinition(CmisVersion.CMIS_1_1);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableTypeDefinition getItemTypeDefinition(final boolean includePropertyDefinitions) {

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		final MutableTypeDefinition def     = factory.createBaseItemTypeDefinition(CmisVersion.CMIS_1_1);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableTypeDefinition getPolicyTypeDefinition(final boolean includePropertyDefinitions) {

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		final MutableTypeDefinition def     = factory.createBasePolicyTypeDefinition(CmisVersion.CMIS_1_1);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableTypeDefinition getFolderTypeDefinition(final boolean includePropertyDefinitions) {

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		final MutableTypeDefinition def     = factory.createBaseFolderTypeDefinition(CmisVersion.CMIS_1_1);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}

	private MutableTypeDefinition getDocumentTypeDefinition(final boolean includePropertyDefinitions) {

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();
		final MutableTypeDefinition def     = factory.createBaseDocumentTypeDefinition(CmisVersion.CMIS_1_1);

		if (!includePropertyDefinitions) {
			def.removeAllPropertyDefinitions();
		}

		return def;
	}
}
