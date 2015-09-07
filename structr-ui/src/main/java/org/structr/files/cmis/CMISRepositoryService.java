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
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.TypeDefinitionListImpl;
import org.apache.chemistry.opencmis.commons.spi.RepositoryService;
import org.apache.chemistry.opencmis.server.support.TypeDefinitionFactory;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
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

	private final RepositoryInfo repositoryInfo     = new StructrRepositoryInfo();
	private final TypeDefinition documentDefinition;
	private final TypeDefinition folderDefinition;

	public CMISRepositoryService(final SecurityContext securityContext) {

		super(securityContext);

		final TypeDefinitionFactory factory = TypeDefinitionFactory.newInstance();

		documentDefinition = factory.createBaseDocumentTypeDefinition(CmisVersion.CMIS_1_1);
		folderDefinition   = factory.createBaseFolderTypeDefinition(CmisVersion.CMIS_1_1);
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
		final App app                      = StructrApp.getInstance();

		if (typeId != null) {

			final Class parentType = config.getNodeEntityClass(typeId);
			if (parentType != null) {

				results.addAll(getTypeDefinitions(config, typeId, false));

			} else {

				throw new CmisNotSupportedException("Type with ID " + typeId + " does not exist");
			}

		} else {

			try (final Tx tx = app.tx()) {

				for (final SchemaNode schemaNode : app.nodeQuery(SchemaNode.class).getAsList()) {

					final Class type = config.getNodeEntityClass(schemaNode.getName());
					if (type != null) {

						results.add(new StructrTypeWrapper(type));

						// add descendants
						results.addAll(getTypeDefinitions(config, type.getSimpleName(), false));
					}

				}

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		final TypeDefinitionListImpl returnValue   = new TypeDefinitionListImpl();
		returnValue.setNumItems(BigInteger.valueOf(results.size()));
		returnValue.setList(new LinkedList<>(results));
		returnValue.setHasMoreItems(false);

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
		final App app                               = StructrApp.getInstance();

		if (typeId != null) {

			final Class parentType = config.getNodeEntityClass(typeId);
			if (parentType != null) {

				results.addAll(getTypeDefinitions(config, typeId, true));

			} else {

				throw new CmisNotSupportedException("Type with ID " + typeId + " does not exist");
			}

		} else {

			try (final Tx tx = app.tx()) {

				for (final SchemaNode schemaNode : app.nodeQuery(SchemaNode.class).getAsList()) {

					final Class type = config.getNodeEntityClass(schemaNode.getName());
					if (type != null) {

						results.add(new StructrTypeWrapper(type));

						// add descendants
						results.addAll(getTypeDefinitions(config, type.getSimpleName(), true));
					}

				}

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return new LinkedList<>(results);
	}

	@Override
	public TypeDefinition getTypeDefinition(final String repositoryId, final String typeId, final ExtensionsData extension) {

		if (StringUtils.isBlank(typeId)) {
			logger.log(Level.WARNING, "###################### Empty type ID!");
			return null;
		}

		switch (typeId) {

			case "cmis:document":
				return documentDefinition;

			case "cmis:folder":
				return folderDefinition;

		}

		final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeId);
		if (type != null) {

				return new StructrTypeWrapper(type);
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
	private Set<StructrTypeWrapper> getTypeDefinitions(final ConfigurationProvider config, final String typeId, final boolean recursive) {

		final Set<String> types               = SearchCommand.getAllSubtypesAsStringSet(typeId, !recursive);
		final Set<StructrTypeWrapper> results = new LinkedHashSet<>();

		for (final String type : types) {

			final Class clazz = config.getNodeEntityClass(type);
			if (clazz != null) {

				results.add(new StructrTypeWrapper(clazz));
			}
		}

		return results;
	}
}
