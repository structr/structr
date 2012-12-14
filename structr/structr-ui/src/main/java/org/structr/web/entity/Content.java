/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity;

import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.search.Search;
import org.structr.web.common.PageHelper;
import org.structr.web.entity.html.*;
import org.structr.web.validator.DynamicValidator;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.EntityProperty;
import org.structr.web.property.DynamicContentProperty;
import org.structr.web.property.PathsProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a content container
 *
 * @author axel
 */
public class Content extends AbstractNode {

	private static final Logger logger         = Logger.getLogger(Content.class.getName());

	public static final Property<String>               tag              = new StringProperty("tag");
	public static final Property<String>               content          = new DynamicContentProperty("content");
	public static final Property<String>               contentType      = new StringProperty("contentType");
	public static final Property<Integer>              size             = new IntProperty("size");
	public static final Property<Set<String>>          paths            = new PathsProperty(("paths"));
	public static final Property<String>               dataKey          = new StringProperty("data-key");
		
	public static final EntityProperty<TypeDefinition> typeDefinition   = new EntityProperty<TypeDefinition>("typeDefinition", TypeDefinition.class, RelType.IS_A, true);
	public static final Property<String>               typeDefinitionId = new EntityIdProperty("typeDefinitionId", typeDefinition);
	
	public static final CollectionProperty<Element>    elements         = new CollectionProperty<Element>("elements", Element.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Title>      titles           = new CollectionProperty<Title>("titles", Title.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Body>       bodys            = new CollectionProperty<Body>("bodys", Body.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Style>      styles           = new CollectionProperty<Style>("styles", Style.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Script>     scripts          = new CollectionProperty<Script>("scripts", Script.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<P>          ps               = new CollectionProperty<P>("ps", P.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Div>        divs             = new CollectionProperty<Div>("divs", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<H1>         h1s              = new CollectionProperty<H1>("h1s", H1.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<H2>         h2s              = new CollectionProperty<H2>("h2s", H2.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<H3>         h3s              = new CollectionProperty<H3>("h3s", H3.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<H4>         h4s              = new CollectionProperty<H4>("h4s", H4.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<H5>         h5s              = new CollectionProperty<H5>("h5s", H5.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<H6>         h6s              = new CollectionProperty<H6>("h6s", H6.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<A>          as               = new CollectionProperty<A>("as", A.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Em>         ems              = new CollectionProperty<Em>("ems", Em.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Strong>     strongs          = new CollectionProperty<Strong>("strongs", Strong.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Small>      smalls           = new CollectionProperty<Small>("smalls", Small.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<S>          ss               = new CollectionProperty<S>("ss", S.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Cite>       cites            = new CollectionProperty<Cite>("cites", Cite.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<G>          gs               = new CollectionProperty<G>("gs", G.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Dfn>        dfns             = new CollectionProperty<Dfn>("dfns", Dfn.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Abbr>       abbrs            = new CollectionProperty<Abbr>("abbrs", Abbr.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Time>       times            = new CollectionProperty<Time>("times", Time.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Code>       codes            = new CollectionProperty<Code>("codes", Code.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Var>        vars             = new CollectionProperty<Var>("vars", Var.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Samp>       samps            = new CollectionProperty<Samp>("samps", Samp.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Kbd>        kbds             = new CollectionProperty<Kbd>("kbds", Kbd.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Sub>        subs             = new CollectionProperty<Sub>("subs", Sub.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Sup>        sups             = new CollectionProperty<Sup>("sups", Sup.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<I>          is               = new CollectionProperty<I>("is", I.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<B>          bs               = new CollectionProperty<B>("bs", B.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<U>          us               = new CollectionProperty<U>("us", U.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Mark>       marks            = new CollectionProperty<Mark>("marks", Mark.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Ruby>       rubys            = new CollectionProperty<Ruby>("rubys", Ruby.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Rt>         rts              = new CollectionProperty<Rt>("rts", Rt.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Rp>         rps              = new CollectionProperty<Rp>("rps", Rp.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Bdi>        bdis             = new CollectionProperty<Bdi>("bdis", Bdi.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Bdo>        bdos             = new CollectionProperty<Bdo>("bdos", Bdo.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<Span>       spans            = new CollectionProperty<Span>("spans", Span.class, RelType.CONTAINS, Direction.INCOMING, false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Content.class, PropertyView.Ui,
		name, tag, content, contentType, size, type, paths, dataKey, typeDefinitionId
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(Content.class, PropertyView.Public,
		name, tag, content, contentType, size, type, paths, dataKey, typeDefinitionId
	);

	//~--- static initializers --------------------------------------------

	static {
		
		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.keyword.name(),  uiView.properties());
		
		EntityContext.registerPropertyValidator(Content.class, content, new DynamicValidator(content));

	}

	//~--- methods --------------------------------------------------------

	/**
	 * Do necessary updates on all containing pages
	 *
	 * @throws FrameworkException
	 */
	private void updatePages(SecurityContext securityContext) throws FrameworkException {

		List<Page> pages = PageHelper.getPages(securityContext, this);

		for (Page page : pages) {

			page.unlockReadOnlyPropertiesOnce();
			page.increaseVersion();

		}

	}

	@Override
	public void afterModification(SecurityContext securityContext) {

		try {

			updatePages(securityContext);

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "Updating page versions failed", ex);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public java.lang.Object getPropertyForIndexing(final PropertyKey key) {

		if (key.equals(Content.content)) {

			String value = getProperty(Content.content);

			if (value != null) {

				return Search.escapeForLucene(value);
			}

		}

		return getProperty(key);

	}

	public Element getParent() {

		// FIXME: this is an ugly hack :)
		return (Element) getRelToParent().getStartNode();
	}

	public AbstractRelationship getRelToParent() {

		// FIXME: this is an ugly hack :)
		return getRelationships(RelType.CONTAINS, Direction.INCOMING).get(0);
	}

	public Component getParentComponent() {

		for (AbstractRelationship in : getRelationships(RelType.CONTAINS, Direction.INCOMING)) {

			String componentId = in.getProperty(Component.componentId);

			if (componentId != null) {

				AbstractNode node = in.getStartNode();

				while (!(node instanceof Page)) {

					if (node instanceof Component) {

						return (Component) node;
					}

					node = node.getIncomingRelationships(RelType.CONTAINS).get(0).getStartNode();

				}

			}

		}

		return null;

	}

	public String getPropertyWithVariableReplacement(HttpServletRequest request, AbstractNode page, String pageId, String componentId, AbstractNode viewComponent, PropertyKey<String> key) throws FrameworkException {

		if (securityContext.getRequest() == null) {

			securityContext.setRequest(request);
		}

		return HtmlElement.replaceVariables(securityContext, page, this, pageId, componentId, viewComponent, super.getProperty(key));

	}

	public TypeDefinition getTypeDefinition() {
		return getProperty(Content.typeDefinition);
	}

}
