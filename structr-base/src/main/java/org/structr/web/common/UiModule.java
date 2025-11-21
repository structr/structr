/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import org.structr.api.service.LicenseManager;
import org.structr.core.datasources.DataSources;
import org.structr.core.function.Functions;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.files.url.StructrURLStreamHandlerFactory;
import org.structr.module.StructrModule;
import org.structr.web.datasource.CypherGraphDataSource;
import org.structr.web.datasource.FunctionDataSource;
import org.structr.web.datasource.IdRequestParameterGraphDataSource;
import org.structr.web.function.*;
import org.structr.web.traits.definitions.*;
import org.structr.web.traits.definitions.dom.*;
import org.structr.web.traits.definitions.html.*;
import org.structr.web.traits.relationships.*;

import java.net.URL;
import java.util.Set;

/**
 */
public class UiModule implements StructrModule {

	static {

		URL.setURLStreamHandlerFactory(new StructrURLStreamHandlerFactory());
	}

	@Override
	public void onLoad() {

		DataSources.put("ui", "idRequestParameterDataSource", new IdRequestParameterGraphDataSource("nodeId"));
		DataSources.put("ui", "cypherDataSource",             new CypherGraphDataSource());
		DataSources.put("ui", "functionDataSource",           new FunctionDataSource(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY));

		// relationships: traits
		StructrTraits.registerTrait(new AbstractFileCONFIGURED_BYStorageConfiguration());
		StructrTraits.registerTrait(new ActionMappingPARAMETERParameterMapping());
		StructrTraits.registerTrait(new CssRuleCONTAINSCssRule());
		StructrTraits.registerTrait(new CssRuleHAS_DECLARATIONCssDeclaration());
		StructrTraits.registerTrait(new CssRuleHAS_SELECTORCssSelector());
		StructrTraits.registerTrait(new CssSemanticClassMAPS_TOCssSelector());
		StructrTraits.registerTrait(new DOMElementINPUT_ELEMENTParameterMapping());
		StructrTraits.registerTrait(new DOMElementRELOADSDOMElement());
		StructrTraits.registerTrait(new DOMElementTRIGGERED_BYActionMapping());
		StructrTraits.registerTrait(new DOMNodeCONTAINSDOMNode());
		StructrTraits.registerTrait(new DOMNodeCONTAINS_NEXT_SIBLINGDOMNode());
		StructrTraits.registerTrait(new DOMNodeFAILURE_TARGETActionMapping());
		StructrTraits.registerTrait(new DOMNodePAGEPage());
		StructrTraits.registerTrait(new DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping());
		StructrTraits.registerTrait(new DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping());
		StructrTraits.registerTrait(new DOMNodeSUCCESS_TARGETActionMapping());
		StructrTraits.registerTrait(new DOMNodeSYNCDOMNode());
		StructrTraits.registerTrait(new FolderCONTAINSAbstractFile());
		StructrTraits.registerTrait(new FolderCONTAINSFile());
		StructrTraits.registerTrait(new FolderCONTAINSFolder());
		StructrTraits.registerTrait(new FolderCONTAINSImage());
		StructrTraits.registerTrait(new ImagePICTURE_OFUser());
		StructrTraits.registerTrait(new ImageTHUMBNAILImage());
		StructrTraits.registerTrait(new PageHAS_PATHPagePath());
		StructrTraits.registerTrait(new LinkSourceLINKLinkableTraitDefinition());
		StructrTraits.registerTrait(new PagePathHAS_PARAMETERPagePathParameter());
		StructrTraits.registerTrait(new SiteCONTAINSPage());
		StructrTraits.registerTrait(new StorageConfigurationCONFIG_ENTRYStorageConfigurationEntryTraitDefinition());
		StructrTraits.registerTrait(new UserHOME_DIRFolder());
		StructrTraits.registerTrait(new UserWORKING_DIRFolder());

		// relationships: types
		StructrTraits.registerRelationshipType(StructrTraits.ABSTRACT_FILE_CONFIGURED_BY_STORAGE_CONFIGURATION,              StructrTraits.ABSTRACT_FILE_CONFIGURED_BY_STORAGE_CONFIGURATION);
		StructrTraits.registerRelationshipType(StructrTraits.ACTION_MAPPING_PARAMETER_PARAMETER_MAPPING,                     StructrTraits.ACTION_MAPPING_PARAMETER_PARAMETER_MAPPING);
		StructrTraits.registerRelationshipType(StructrTraits.CSS_RULE_CONTAINS_CSS_RULE,                                     StructrTraits.CSS_RULE_CONTAINS_CSS_RULE);
		StructrTraits.registerRelationshipType(StructrTraits.CSS_RULE_HAS_DECLARATION_CSS_DECLARATION,                       StructrTraits.CSS_RULE_HAS_DECLARATION_CSS_DECLARATION);
		StructrTraits.registerRelationshipType(StructrTraits.CSS_RULE_HAS_SELECTOR_CSS_SELECTOR,                             StructrTraits.CSS_RULE_HAS_SELECTOR_CSS_SELECTOR);
		StructrTraits.registerRelationshipType(StructrTraits.CSS_SEMANTIC_CLASS_MAPS_TO_CSS_SELECTOR,                        StructrTraits.CSS_SEMANTIC_CLASS_MAPS_TO_CSS_SELECTOR);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_ELEMENT_INPUT_ELEMENT_PARAMETER_MAPPING,                    StructrTraits.DOM_ELEMENT_INPUT_ELEMENT_PARAMETER_MAPPING);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_ELEMENT_RELOADS_DOM_ELEMENT,                                StructrTraits.DOM_ELEMENT_RELOADS_DOM_ELEMENT);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_ELEMENT_TRIGGERED_BY_ACTION_MAPPING,                        StructrTraits.DOM_ELEMENT_TRIGGERED_BY_ACTION_MAPPING);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_CONTAINS_DOM_NODE,                                     StructrTraits.DOM_NODE_CONTAINS_DOM_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_CONTAINS_NEXT_SIBLING_DOM_NODE,                        StructrTraits.DOM_NODE_CONTAINS_NEXT_SIBLING_DOM_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_FAILURE_TARGET_ACTION_MAPPING,                         StructrTraits.DOM_NODE_FAILURE_TARGET_ACTION_MAPPING);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_PAGE_PAGE,                                             StructrTraits.DOM_NODE_PAGE_PAGE);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_SUCCESS_NOTIFICATION_ELEMENT_ACTION_MAPPING,           StructrTraits.DOM_NODE_SUCCESS_NOTIFICATION_ELEMENT_ACTION_MAPPING);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_FAILURE_NOTIFICATION_ELEMENT_ACTION_MAPPING,           StructrTraits.DOM_NODE_FAILURE_NOTIFICATION_ELEMENT_ACTION_MAPPING);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_SUCCESS_TARGET_ACTION_MAPPING,                         StructrTraits.DOM_NODE_SUCCESS_TARGET_ACTION_MAPPING);
		StructrTraits.registerRelationshipType(StructrTraits.DOM_NODE_SYNC_DOM_NODE,                                         StructrTraits.DOM_NODE_SYNC_DOM_NODE);
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_ABSTRACT_FILE,                                  StructrTraits.FOLDER_CONTAINS_ABSTRACT_FILE);
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_FILE,                                           StructrTraits.FOLDER_CONTAINS_FILE);
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_FOLDER,                                         StructrTraits.FOLDER_CONTAINS_FOLDER);
		StructrTraits.registerRelationshipType(StructrTraits.FOLDER_CONTAINS_IMAGE,                                          StructrTraits.FOLDER_CONTAINS_IMAGE);
		StructrTraits.registerRelationshipType(StructrTraits.IMAGE_PICTURE_OF_USER,                                          StructrTraits.IMAGE_PICTURE_OF_USER);
		StructrTraits.registerRelationshipType(StructrTraits.IMAGE_THUMBNAIL_IMAGE,                                          StructrTraits.IMAGE_THUMBNAIL_IMAGE);
		StructrTraits.registerRelationshipType(StructrTraits.PAGE_HAS_PATH_PAGE_PATH,                                        StructrTraits.PAGE_HAS_PATH_PAGE_PATH);
		StructrTraits.registerRelationshipType(StructrTraits.LINK_SOURCE_LINK_LINKABLE,                                      StructrTraits.LINK_SOURCE_LINK_LINKABLE);
		StructrTraits.registerRelationshipType(StructrTraits.PAGE_PATH_HAS_PARAMETER_PAGE_PATH_PARAMETER,                    StructrTraits.PAGE_PATH_HAS_PARAMETER_PAGE_PATH_PARAMETER);
		StructrTraits.registerRelationshipType(StructrTraits.SITE_CONTAINS_PAGE,                                             StructrTraits.SITE_CONTAINS_PAGE);
		StructrTraits.registerRelationshipType(StructrTraits.STORAGE_CONFIGURATION_CONFIG_ENTRY_STORAGE_CONFIGURATION_ENTRY, StructrTraits.STORAGE_CONFIGURATION_CONFIG_ENTRY_STORAGE_CONFIGURATION_ENTRY);
		StructrTraits.registerRelationshipType(StructrTraits.USER_HOME_DIR_FOLDER,                                           StructrTraits.USER_HOME_DIR_FOLDER);
		StructrTraits.registerRelationshipType(StructrTraits.USER_WORKING_DIR_FOLDER,                                        StructrTraits.USER_WORKING_DIR_FOLDER);

		// nodes: traits
		StructrTraits.registerTrait(new DOMNodeTraitDefinition());
		StructrTraits.registerTrait(new AbstractFileTraitDefinition());
		StructrTraits.registerTrait(new ActionMappingTraitDefinition());
		StructrTraits.registerTrait(new ApplicationConfigurationDataNodeTraitDefinition());
		StructrTraits.registerTrait(new ContentTraitDefinition());
		StructrTraits.registerTrait(new CommentTraitDefinition());
		StructrTraits.registerTrait(new DOMElementTraitDefinition());
		StructrTraits.registerTrait(new ComponentTraitDefinition());
		StructrTraits.registerTrait(new CssDeclarationTraitDefinition());
		StructrTraits.registerTrait(new CssRuleTraitDefinition());
		StructrTraits.registerTrait(new CssSelectorTraitDefinition());
		StructrTraits.registerTrait(new CssSemanticClassTraitDefinition());
		StructrTraits.registerTrait(new DOMNodeTraitDefinition());
		StructrTraits.registerTrait(new AbstractFileTraitDefinition());
		StructrTraits.registerTrait(new LinkableTraitDefinition());
		StructrTraits.registerTrait(new FileTraitDefinition());
		StructrTraits.registerTrait(new ImageTraitDefinition());
		StructrTraits.registerTrait(new FolderTraitDefinition());
		StructrTraits.registerTrait(new LinkableTraitDefinition());
		StructrTraits.registerTrait(new LinkSourceTraitDefinition());
		StructrTraits.registerTrait(new PageTraitDefinition());
		StructrTraits.registerTrait(new PagePathTraitDefinition());
		StructrTraits.registerTrait(new PagePathParameterTraitDefinition());
		StructrTraits.registerTrait(new ParameterMappingTraitDefinition());
		StructrTraits.registerTrait(new ShadowDocumentTraitDefinition());
		StructrTraits.registerTrait(new SiteTraitDefinition());
		StructrTraits.registerTrait(new StorageConfigurationTraitDefinition());
		StructrTraits.registerTrait(new StorageConfigurationEntryTraitDefinition());
		StructrTraits.registerTrait(new TemplateTraitDefinition());
		StructrTraits.registerTrait(new UserTraitDefinition());
		StructrTraits.registerTrait(new WidgetTraitDefinition());

		// nodes: types
		StructrTraits.registerNodeType(StructrTraits.ABSTRACT_FILE,                       StructrTraits.ABSTRACT_FILE);
		StructrTraits.registerNodeType(StructrTraits.ACTION_MAPPING,                      StructrTraits.ACTION_MAPPING);
		StructrTraits.registerNodeType(StructrTraits.APPLICATION_CONFIGURATION_DATA_NODE, StructrTraits.APPLICATION_CONFIGURATION_DATA_NODE);
		StructrTraits.registerNodeType(StructrTraits.COMMENT,                             StructrTraits.DOM_NODE, StructrTraits.CONTENT, StructrTraits.COMMENT);
		StructrTraits.registerNodeType(StructrTraits.COMPONENT,                           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.COMPONENT);
		StructrTraits.registerNodeType(StructrTraits.CONTENT,                             StructrTraits.DOM_NODE, StructrTraits.CONTENT);
		StructrTraits.registerNodeType(StructrTraits.CSS_DECLARATION,                     StructrTraits.CSS_DECLARATION);
		StructrTraits.registerNodeType(StructrTraits.CSS_RULE,                            StructrTraits.CSS_RULE);
		StructrTraits.registerNodeType(StructrTraits.CSS_SELECTOR,                        StructrTraits.CSS_SELECTOR);
		StructrTraits.registerNodeType(StructrTraits.CSS_SEMANTIC_CLASS,                  StructrTraits.CSS_SEMANTIC_CLASS);
		StructrTraits.registerNodeType(StructrTraits.DOM_NODE,                            StructrTraits.DOM_NODE);
		StructrTraits.registerNodeType(StructrTraits.DOM_ELEMENT,                         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT);
		StructrTraits.registerNodeType(StructrTraits.FILE,                                StructrTraits.ABSTRACT_FILE, StructrTraits.LINKABLE, StructrTraits.FILE);
		StructrTraits.registerNodeType(StructrTraits.IMAGE,                               StructrTraits.ABSTRACT_FILE, StructrTraits.LINKABLE, StructrTraits.FILE, StructrTraits.IMAGE);
		StructrTraits.registerNodeType(StructrTraits.FOLDER,                              StructrTraits.ABSTRACT_FILE, StructrTraits.FOLDER);
		StructrTraits.registerNodeType(StructrTraits.LINKABLE,                            StructrTraits.LINKABLE);
		StructrTraits.registerNodeType(StructrTraits.LINK_SOURCE,                         StructrTraits.LINK_SOURCE);
		StructrTraits.registerNodeType(StructrTraits.PAGE,                                StructrTraits.DOM_NODE, StructrTraits.LINKABLE, StructrTraits.PAGE);
		StructrTraits.registerNodeType(StructrTraits.PAGE_PATH,                           StructrTraits.PAGE_PATH);
		StructrTraits.registerNodeType(StructrTraits.PAGE_PATH_PARAMETER,                 StructrTraits.PAGE_PATH_PARAMETER);
		StructrTraits.registerNodeType(StructrTraits.PARAMETER_MAPPING,                   StructrTraits.PARAMETER_MAPPING);
		StructrTraits.registerNodeType(StructrTraits.SHADOW_DOCUMENT,                     StructrTraits.DOM_NODE, StructrTraits.LINKABLE, StructrTraits.PAGE, StructrTraits.SHADOW_DOCUMENT);
		StructrTraits.registerNodeType(StructrTraits.SITE,                                StructrTraits.SITE);
		StructrTraits.registerNodeType(StructrTraits.STORAGE_CONFIGURATION,               StructrTraits.STORAGE_CONFIGURATION);
		StructrTraits.registerNodeType(StructrTraits.STORAGE_CONFIGURATION_ENTRY,         StructrTraits.STORAGE_CONFIGURATION_ENTRY);
		StructrTraits.registerNodeType(StructrTraits.TEMPLATE,                            StructrTraits.DOM_NODE, StructrTraits.CONTENT, StructrTraits.TEMPLATE);
		StructrTraits.registerNodeType(StructrTraits.USER,                                StructrTraits.PRINCIPAL, StructrTraits.USER);
		StructrTraits.registerNodeType(StructrTraits.WIDGET,                              StructrTraits.WIDGET);

		// HTML elements: traits
		StructrTraits.registerTrait(new A());
		StructrTraits.registerTrait(new Abbr());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.ADDRESS));
		StructrTraits.registerTrait(new Area());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.ARTICLE));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.ASIDE));
		StructrTraits.registerTrait(new Audio());
		StructrTraits.registerTrait(new Base());
		StructrTraits.registerTrait(new Bdi());
		StructrTraits.registerTrait(new Bdo());
		StructrTraits.registerTrait(new B());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.BLOCKQUOTE));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.BODY));
		StructrTraits.registerTrait(new Br());
		StructrTraits.registerTrait(new Button());
		StructrTraits.registerTrait(new Canvas());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.CAPTION));
		StructrTraits.registerTrait(new Cite());
		StructrTraits.registerTrait(new Code());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.COLGROUP));
		StructrTraits.registerTrait(new Col());
		StructrTraits.registerTrait(new Command());
		StructrTraits.registerTrait(new Data());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.DATALIST));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.DD));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.DEL));
		StructrTraits.registerTrait(new Details());
		StructrTraits.registerTrait(new Dfn());
		StructrTraits.registerTrait(new Dialog());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.DIV));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.DL));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.DT));
		StructrTraits.registerTrait(new Embed());
		StructrTraits.registerTrait(new Em());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.FIELDSET));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.FIGCAPTION));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.FIGURE));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.FOOTER));
		StructrTraits.registerTrait(new Form());
		StructrTraits.registerTrait(new G());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.H1));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.H2));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.H3));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.H4));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.H5));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.H6));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.HEADER));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.HEAD));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.HGROUP));
		StructrTraits.registerTrait(new Hr());
		StructrTraits.registerTrait(new Html());
		StructrTraits.registerTrait(new Iframe());
		StructrTraits.registerTrait(new I());
		StructrTraits.registerTrait(new Img());
		StructrTraits.registerTrait(new Input());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.INS));
		StructrTraits.registerTrait(new Kbd());
		StructrTraits.registerTrait(new Keygen());
		StructrTraits.registerTrait(new Label());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.LEGEND));
		StructrTraits.registerTrait(new Li());
		StructrTraits.registerTrait(new Link());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.MAIN));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.MAP));
		StructrTraits.registerTrait(new Mark());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.MENU));
		StructrTraits.registerTrait(new Meta());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.METER));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.NAV));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.NOSCRIPT));
		StructrTraits.registerTrait(new org.structr.web.traits.definitions.html.Object());
		StructrTraits.registerTrait(new Ol());
		StructrTraits.registerTrait(new Optgroup());
		StructrTraits.registerTrait(new Option());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.OUTPUT));
		StructrTraits.registerTrait(new Param());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.PICTURE));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.P));
		StructrTraits.registerTrait(new Pre());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.PROGRESS));
		StructrTraits.registerTrait(new Q());
		StructrTraits.registerTrait(new Rp());
		StructrTraits.registerTrait(new Rt());
		StructrTraits.registerTrait(new Ruby());
		StructrTraits.registerTrait(new Samp());
		StructrTraits.registerTrait(new Script());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.SECTION));
		StructrTraits.registerTrait(new Select());
		StructrTraits.registerTrait(new S());
		StructrTraits.registerTrait(new Slot());
		StructrTraits.registerTrait(new Small());
		StructrTraits.registerTrait(new Source());
		StructrTraits.registerTrait(new Span());
		StructrTraits.registerTrait(new Strong());
		StructrTraits.registerTrait(new Style());
		StructrTraits.registerTrait(new Sub());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.SUMMARY));
		StructrTraits.registerTrait(new Sup());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.TABLE));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.TBODY));
		StructrTraits.registerTrait(new Td());
		StructrTraits.registerTrait(new TemplateElement());
		StructrTraits.registerTrait(new Textarea());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.TFOOT));
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.THEAD));
		StructrTraits.registerTrait(new Th());
		StructrTraits.registerTrait(new Time());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.TITLE));
		StructrTraits.registerTrait(new Track());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.TR));
		StructrTraits.registerTrait(new U());
		StructrTraits.registerTrait(new GenericHtmlElementTraitDefinition(StructrTraits.UL));
		StructrTraits.registerTrait(new Var());
		StructrTraits.registerTrait(new Video());
		StructrTraits.registerTrait(new Wbr());

		// HTML elements: types
		StructrTraits.registerNodeType(StructrTraits.A,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.LINK_SOURCE, StructrTraits.A);
		StructrTraits.registerNodeType(StructrTraits.ABBR,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.ABBR);
		StructrTraits.registerNodeType(StructrTraits.ADDRESS,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.ADDRESS);
		StructrTraits.registerNodeType(StructrTraits.AREA,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.AREA);
		StructrTraits.registerNodeType(StructrTraits.ARTICLE,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.ARTICLE);
		StructrTraits.registerNodeType(StructrTraits.ASIDE,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.ASIDE);
		StructrTraits.registerNodeType(StructrTraits.AUDIO,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.AUDIO);
		StructrTraits.registerNodeType(StructrTraits.BASE,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.BASE);
		StructrTraits.registerNodeType(StructrTraits.BDI,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.BDI);
		StructrTraits.registerNodeType(StructrTraits.BDO,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.BDO);
		StructrTraits.registerNodeType(StructrTraits.B,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.B);
		StructrTraits.registerNodeType(StructrTraits.BLOCKQUOTE,       StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.BLOCKQUOTE);
		StructrTraits.registerNodeType(StructrTraits.BODY,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.BODY);
		StructrTraits.registerNodeType(StructrTraits.BR,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.BR);
		StructrTraits.registerNodeType(StructrTraits.BUTTON,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.BUTTON);
		StructrTraits.registerNodeType(StructrTraits.CANVAS,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.CANVAS);
		StructrTraits.registerNodeType(StructrTraits.CAPTION,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.CAPTION);
		StructrTraits.registerNodeType(StructrTraits.CITE,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.CITE);
		StructrTraits.registerNodeType(StructrTraits.CODE,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.CODE);
		StructrTraits.registerNodeType(StructrTraits.COLGROUP,         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.COLGROUP);
		StructrTraits.registerNodeType(StructrTraits.COL,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.COL);
		StructrTraits.registerNodeType(StructrTraits.COMMAND,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.COMMAND);
		StructrTraits.registerNodeType(StructrTraits.DATA,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DATA);
		StructrTraits.registerNodeType(StructrTraits.DATALIST,         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DATALIST);
		StructrTraits.registerNodeType(StructrTraits.DD,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DD);
		StructrTraits.registerNodeType(StructrTraits.DEL,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DEL);
		StructrTraits.registerNodeType(StructrTraits.DETAILS,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DETAILS);
		StructrTraits.registerNodeType(StructrTraits.DFN,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DFN);
		StructrTraits.registerNodeType(StructrTraits.DIALOG,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DIALOG);
		StructrTraits.registerNodeType(StructrTraits.DIV,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DIV);
		StructrTraits.registerNodeType(StructrTraits.DL,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DL);
		StructrTraits.registerNodeType(StructrTraits.DT,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.DT);
		StructrTraits.registerNodeType(StructrTraits.EMBED,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.EMBED);
		StructrTraits.registerNodeType(StructrTraits.EM,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.EM);
		StructrTraits.registerNodeType(StructrTraits.FIELDSET,         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.FIELDSET);
		StructrTraits.registerNodeType(StructrTraits.FIGCAPTION,       StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.FIGCAPTION);
		StructrTraits.registerNodeType(StructrTraits.FIGURE,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.FIGURE);
		StructrTraits.registerNodeType(StructrTraits.FOOTER,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.FOOTER);
		StructrTraits.registerNodeType(StructrTraits.FORM,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.FORM);
		StructrTraits.registerNodeType(StructrTraits.G,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.G);
		StructrTraits.registerNodeType(StructrTraits.H1,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.H1);
		StructrTraits.registerNodeType(StructrTraits.H2,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.H2);
		StructrTraits.registerNodeType(StructrTraits.H3,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.H3);
		StructrTraits.registerNodeType(StructrTraits.H4,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.H4);
		StructrTraits.registerNodeType(StructrTraits.H5,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.H5);
		StructrTraits.registerNodeType(StructrTraits.H6,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.H6);
		StructrTraits.registerNodeType(StructrTraits.HEADER,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.HEADER);
		StructrTraits.registerNodeType(StructrTraits.HEAD,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.HEAD);
		StructrTraits.registerNodeType(StructrTraits.HGROUP,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.HGROUP);
		StructrTraits.registerNodeType(StructrTraits.HR,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.HR);
		StructrTraits.registerNodeType(StructrTraits.HTML,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.HTML);
		StructrTraits.registerNodeType(StructrTraits.IFRAME,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.IFRAME);
		StructrTraits.registerNodeType(StructrTraits.I,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.I);
		StructrTraits.registerNodeType(StructrTraits.IMG,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.LINK_SOURCE, StructrTraits.IMG);
		StructrTraits.registerNodeType(StructrTraits.INPUT,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.INPUT);
		StructrTraits.registerNodeType(StructrTraits.INS,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.INS);
		StructrTraits.registerNodeType(StructrTraits.KBD,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.KBD);
		StructrTraits.registerNodeType(StructrTraits.KEYGEN,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.KEYGEN);
		StructrTraits.registerNodeType(StructrTraits.LABEL,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.LABEL);
		StructrTraits.registerNodeType(StructrTraits.LEGEND,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.LEGEND);
		StructrTraits.registerNodeType(StructrTraits.LI,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.LI);
		StructrTraits.registerNodeType(StructrTraits.LINK,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.LINK_SOURCE, StructrTraits.LINK);
		StructrTraits.registerNodeType(StructrTraits.MAIN,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.MAIN);
		StructrTraits.registerNodeType(StructrTraits.MAP,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.MAP);
		StructrTraits.registerNodeType(StructrTraits.MARK,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.MARK);
		StructrTraits.registerNodeType(StructrTraits.MENU,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.MENU);
		StructrTraits.registerNodeType(StructrTraits.META,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.META);
		StructrTraits.registerNodeType(StructrTraits.METER,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.METER);
		StructrTraits.registerNodeType(StructrTraits.NAV,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.NAV);
		StructrTraits.registerNodeType(StructrTraits.NOSCRIPT,         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.NOSCRIPT);
		StructrTraits.registerNodeType(StructrTraits.OBJECT,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.OBJECT);
		StructrTraits.registerNodeType(StructrTraits.OL,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.OL);
		StructrTraits.registerNodeType(StructrTraits.OPTGROUP,         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.OPTGROUP);
		StructrTraits.registerNodeType(StructrTraits.OPTION,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.OPTION);
		StructrTraits.registerNodeType(StructrTraits.OUTPUT,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.OUTPUT);
		StructrTraits.registerNodeType(StructrTraits.PARAM,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.PARAM);
		StructrTraits.registerNodeType(StructrTraits.PICTURE,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.PICTURE);
		StructrTraits.registerNodeType(StructrTraits.P,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.P);
		StructrTraits.registerNodeType(StructrTraits.PRE,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.PRE);
		StructrTraits.registerNodeType(StructrTraits.PROGRESS,         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.PROGRESS);
		StructrTraits.registerNodeType(StructrTraits.Q,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.Q);
		StructrTraits.registerNodeType(StructrTraits.RP,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.RP);
		StructrTraits.registerNodeType(StructrTraits.RT,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.RT);
		StructrTraits.registerNodeType(StructrTraits.RUBY,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.RUBY);
		StructrTraits.registerNodeType(StructrTraits.SAMP,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SAMP);
		StructrTraits.registerNodeType(StructrTraits.SCRIPT,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.LINK_SOURCE, StructrTraits.SCRIPT);
		StructrTraits.registerNodeType(StructrTraits.SECTION,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SECTION);
		StructrTraits.registerNodeType(StructrTraits.SELECT,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SELECT);
		StructrTraits.registerNodeType(StructrTraits.S,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.S);
		StructrTraits.registerNodeType(StructrTraits.SLOT,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SLOT);
		StructrTraits.registerNodeType(StructrTraits.SMALL,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SMALL);
		StructrTraits.registerNodeType(StructrTraits.SOURCE,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SOURCE);
		StructrTraits.registerNodeType(StructrTraits.SPAN,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SPAN);
		StructrTraits.registerNodeType(StructrTraits.STRONG,           StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.STRONG);
		StructrTraits.registerNodeType(StructrTraits.STYLE,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.STYLE);
		StructrTraits.registerNodeType(StructrTraits.SUB,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SUB);
		StructrTraits.registerNodeType(StructrTraits.SUMMARY,          StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SUMMARY);
		StructrTraits.registerNodeType(StructrTraits.SUP,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.SUP);
		StructrTraits.registerNodeType(StructrTraits.TABLE,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TABLE);
		StructrTraits.registerNodeType(StructrTraits.TBODY,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TBODY);
		StructrTraits.registerNodeType(StructrTraits.TD,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TD);
		StructrTraits.registerNodeType(StructrTraits.TEMPLATE_ELEMENT, StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TEMPLATE_ELEMENT);
		StructrTraits.registerNodeType(StructrTraits.TEXTAREA,         StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TEXTAREA);
		StructrTraits.registerNodeType(StructrTraits.TFOOT,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TFOOT);
		StructrTraits.registerNodeType(StructrTraits.THEAD,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.THEAD);
		StructrTraits.registerNodeType(StructrTraits.TH,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TH);
		StructrTraits.registerNodeType(StructrTraits.TIME,             StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TIME);
		StructrTraits.registerNodeType(StructrTraits.TITLE,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TITLE);
		StructrTraits.registerNodeType(StructrTraits.TRACK,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TRACK);
		StructrTraits.registerNodeType(StructrTraits.TR,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.TR);
		StructrTraits.registerNodeType(StructrTraits.U,                StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.U);
		StructrTraits.registerNodeType(StructrTraits.UL,               StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.UL);
		StructrTraits.registerNodeType(StructrTraits.VAR,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.VAR);
		StructrTraits.registerNodeType(StructrTraits.VIDEO,            StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.VIDEO);
		StructrTraits.registerNodeType(StructrTraits.WBR,              StructrTraits.DOM_NODE, StructrTraits.DOM_ELEMENT, StructrTraits.WBR);
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new EscapeHtmlFunction());
		Functions.put(licenseManager, new EscapeXmlFunction());
		Functions.put(licenseManager, new UnescapeHtmlFunction());
		Functions.put(licenseManager, new StripHtmlFunction());
		Functions.put(licenseManager, new FromJsonFunction());
		Functions.put(licenseManager, new ToJsonFunction());
		Functions.put(licenseManager, new ToGraphObjectFunction());
		Functions.put(licenseManager, new IncludeFunction());
		Functions.put(licenseManager, new IncludeChildFunction());
		Functions.put(licenseManager, new RenderFunction());
		Functions.put(licenseManager, new SetDetailsObjectFunction());
		Functions.put(licenseManager, new ConfirmationKeyFunction());
		Functions.put(licenseManager, new ImportHtmlFunction());
		Functions.put(licenseManager, new ImportCssFunction());
		Functions.put(licenseManager, new RemoveDOMChildFunction());
		Functions.put(licenseManager, new ReplaceDOMChildFunction());
		Functions.put(licenseManager, new InsertHtmlFunction());
		Functions.put(licenseManager, new GetSourceFunction());
		Functions.put(licenseManager, new HasCssClassFunction());

		Functions.put(licenseManager, new SendHtmlMailFunction());
		Functions.put(licenseManager, new SendPlaintextMailFunction());
		Functions.put(licenseManager, new GetContentFunction());
		Functions.put(licenseManager, new SetContentFunction());
		Functions.put(licenseManager, new AppendContentFunction());
		Functions.put(licenseManager, new CopyFileContentsFunction());
		Functions.put(licenseManager, new SetSessionAttributeFunction());
		Functions.put(licenseManager, new GetSessionAttributeFunction());
		Functions.put(licenseManager, new RemoveSessionAttributeFunction());
		Functions.put(licenseManager, new IsLocaleFunction());

		Functions.put(licenseManager, new LogEventFunction());

		Functions.put(licenseManager, new HttpGetFunction());
		Functions.put(licenseManager, new HttpHeadFunction());
		Functions.put(licenseManager, new HttpPatchFunction());
		Functions.put(licenseManager, new HttpPostFunction());
		Functions.put(licenseManager, new HTTPPostMultiPartFunction());
		Functions.put(licenseManager, new HttpPutFunction());
		Functions.put(licenseManager, new HttpDeleteFunction());
		Functions.put(licenseManager, new AddHeaderFunction());
		Functions.put(licenseManager, new ClearHeadersFunction());
		Functions.put(licenseManager, new SetResponseHeaderFunction());
		Functions.put(licenseManager, new RemoveResponseHeaderFunction());
		Functions.put(licenseManager, new SetResponseCodeFunction());
		Functions.put(licenseManager, new GetRequestHeaderFunction());
		Functions.put(licenseManager, new ValidateCertificatesFunction());
		Functions.put(licenseManager, new GetCookieFunction());
		Functions.put(licenseManager, new SetCookieFunction());
		Functions.put(licenseManager, new FromXmlFunction());
		Functions.put(licenseManager, new CreateArchiveFunction());
		Functions.put(licenseManager, new CreateFolderPathFunction());
		Functions.put(licenseManager, new CreateZipFunction());
		Functions.put(licenseManager, new UnarchiveFunction());
		Functions.put(licenseManager, new MaintenanceFunction());
		Functions.put(licenseManager, new BarcodeFunction());
		Functions.put(licenseManager, new JobInfoFunction());
		Functions.put(licenseManager, new JobListFunction());
		Functions.put(licenseManager, new CreateAccessTokenFunction());
		Functions.put(licenseManager, new CreateAccessAndRefreshTokenFunction());

		Functions.put(licenseManager, new ApplicationStorePutFunction());
		Functions.put(licenseManager, new ApplicationStoreDeleteFunction());
		Functions.put(licenseManager, new ApplicationStoreGetFunction());
		Functions.put(licenseManager, new ApplicationStoreGetKeysFunction());
		Functions.put(licenseManager, new ApplicationStoreHasFunction());

		Functions.put(licenseManager, new RequestStorePutFunction());
		Functions.put(licenseManager, new RequestStoreDeleteFunction());
		Functions.put(licenseManager, new RequestStoreGetFunction());
		Functions.put(licenseManager, new RequestStoreGetKeysFunction());
		Functions.put(licenseManager, new RequestStoreHasFunction());

		Functions.put(licenseManager, new SendEventFunction());
		Functions.put(licenseManager, new BroadcastEventFunction());

		Functions.put(licenseManager, new SystemInfoFunction());

		Functions.put(licenseManager, new IsValidEmailFunction());
	}

	@Override
	public String getName() {
		return "ui";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("rest");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
