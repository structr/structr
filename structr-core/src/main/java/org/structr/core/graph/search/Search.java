/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.graph.search;

import org.apache.commons.lang.StringUtils;


import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationshipMapping;
import org.structr.core.module.ModuleService;

//~--- JDK imports ------------------------------------------------------------

import java.text.Normalizer;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.BooleanClause.Occur;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public abstract class Search {

	public static final String DISTANCE_SEARCH_KEYWORD    = "distance";
	public static final String LOCATION_SEARCH_KEYWORD    = "location";
	public static final String STREET_SEARCH_KEYWORD      = "street";
	public static final String HOUSE_SEARCH_KEYWORD       = "house";
	public static final String POSTAL_CODE_SEARCH_KEYWORD = "postalCode";
	public static final String CITY_SEARCH_KEYWORD        = "city";
	public static final String STATE_SEARCH_KEYWORD       = "state";
	public static final String COUNTRY_SEARCH_KEYWORD     = "country";
	
	private static final Logger logger                    = Logger.getLogger(Search.class.getName());
	private static final Set<Character> specialCharsExact = new LinkedHashSet<Character>();
	private static final Set<Character> specialChars      = new LinkedHashSet<Character>();

	//~--- static initializers --------------------------------------------

	static {

		specialChars.add('\\');
		specialChars.add('+');
		specialChars.add('-');
		specialChars.add('!');
		specialChars.add('(');
		specialChars.add(')');
		specialChars.add(':');
		specialChars.add('^');
		specialChars.add('[');
		specialChars.add(']');
		specialChars.add('"');
		specialChars.add('{');
		specialChars.add('}');
		specialChars.add('~');
		specialChars.add('*');
		specialChars.add('?');
		specialChars.add('|');
		specialChars.add('&');
		specialChars.add(';');
		specialCharsExact.add('"');
		specialCharsExact.add('\\');

	}

	;

	//~--- methods --------------------------------------------------------

	private static List<SearchAttribute> getExactTypeAndSubtypesInternal(final String searchString) {

		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

		// attrs.add(Search.orExactType(searchString));
		ModuleService moduleService      = Services.getService(ModuleService.class);
		Map<String, Class> entities      = moduleService.getCachedNodeEntities();
		Class parentClass                = entities.get(searchString);

		if (parentClass == null) {

			// no entity class for the given type found,
			// examine interface types and subclasses
			Set<Class> classesForInterface = moduleService.getClassesForInterface(EntityContext.normalizeEntityName(searchString));

			if (classesForInterface != null) {

				for (Class clazz : classesForInterface) {

					attrs.addAll(getExactTypeAndSubtypesInternal(clazz.getSimpleName()));
				}

			}

			return attrs;
		}

		for (Map.Entry<String, Class> entity : entities.entrySet()) {

			Class entityClass = entity.getValue();

			if (parentClass.isAssignableFrom(entityClass)) {

				attrs.add(Search.orExactType(entity.getKey()));
			}

		}

		return attrs;

	}

	public static SearchAttributeGroup andExactTypeAndSubtypes(final String searchString) {

		SearchAttributeGroup attrs          = new SearchAttributeGroup(Occur.MUST);
		List<SearchAttribute> attrsInternal = getExactTypeAndSubtypesInternal(searchString);

		for (SearchAttribute attr : attrsInternal) {

			attrs.add(attr);
		}

		return attrs;

	}
	
	public static SearchAttributeGroup orExactTypeAndSubtypes(final String searchString) {

		SearchAttributeGroup attrs          = new SearchAttributeGroup(Occur.SHOULD);
		List<SearchAttribute> attrsInternal = getExactTypeAndSubtypesInternal(searchString);

		for (SearchAttribute attr : attrsInternal) {

			attrs.add(attr);
		}

		return attrs;

	}

	public static SearchAttribute orName(final String searchString) {
		return new PropertySearchAttribute(AbstractNode.name, searchString, Occur.SHOULD, false);
	}

	public static SearchAttribute andName(final String searchString) {
		return new PropertySearchAttribute(AbstractNode.name, searchString, Occur.MUST, false);
	}

	public static <T> SearchAttribute andProperty(final SecurityContext securityContext, final PropertyKey<T> key, final T searchValue) {
		return key.getSearchAttribute(securityContext, Occur.MUST, searchValue, false);
	}

	public static SearchAttribute orExactType(final String searchString) {
		return new PropertySearchAttribute(AbstractNode.type, searchString, Occur.SHOULD, true);
	}

	public static SearchAttribute andExactType(final String searchString) {
		return new PropertySearchAttribute(AbstractNode.type, searchString, Occur.MUST, true);
	}

	public static SearchAttribute andExactRelType(final RelationshipMapping namedRelation) {

		return andExactRelType(namedRelation.getRelType().name(), namedRelation.getSourceType().getSimpleName(), namedRelation.getDestType().getSimpleName());

	}

	public static SearchAttribute andExactRelType(final String relType, final String sourceType, final String destType) {

		String searchString  = EntityContext.createCombinedRelationshipType(sourceType, relType, destType);
		SearchAttribute attr = new PropertySearchAttribute(AbstractRelationship.combinedType, searchString, Occur.MUST, true);

		return attr;

	}

	public static SearchAttribute orExactRelType(final RelationshipMapping namedRelation) {

		return orExactRelType(namedRelation.getRelType().name(), namedRelation.getSourceType().getSimpleName(), namedRelation.getDestType().getSimpleName());

	}

	public static SearchAttribute orExactRelType(final String relType, final String sourceType, final String destType) {

		String searchString  = EntityContext.createCombinedRelationshipType(sourceType, relType, destType);
		SearchAttribute attr = new PropertySearchAttribute(AbstractRelationship.combinedType, searchString, Occur.SHOULD, true);

		return attr;

	}

	public static SearchAttribute orExactName(final String searchString) {
		return new PropertySearchAttribute(AbstractNode.name, searchString, Occur.SHOULD, true);
	}

	public static SearchAttribute andExactName(final String searchString) {
		return new PropertySearchAttribute(AbstractNode.name, searchString, Occur.MUST, true);
	}

	public static SearchAttribute andExactUuid(final String searchString) {
		return new PropertySearchAttribute(AbstractNode.uuid, searchString, Occur.MUST, true);
	}

	public static <T> SearchAttribute andExactProperty(final SecurityContext securityContext, final PropertyKey<T> propertyKey, final T searchValue) {
		return propertyKey.getSearchAttribute(securityContext, Occur.MUST, searchValue, true);
	}

	public static <T> SearchAttribute orExactProperty(final SecurityContext securityContext, final PropertyKey<T> propertyKey, final T searchValue) {
		return propertyKey.getSearchAttribute(securityContext, Occur.SHOULD, searchValue, true);
	}

	public static String unquoteExactMatch(final String searchString) {

		String result = searchString;

		if (searchString.startsWith("\"")) {

			result = result.substring(1);
		}

		if (searchString.endsWith("\"")) {

			result = result.substring(0, result.length() - 1);
		}

		return result;

	}

	/**
	 * Normalize special characters to ASCII
	 *
	 * @param input
	 * @return
	 */
	public static String normalize(final String input) {

		String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

		return normalized.replaceAll("[^\\p{ASCII}]", "");

	}

	/**
	 * Remove dangerous characters from a search string
	 *
	 * @param input
	 * @return
	 */
	public static String clean(final String input) {

//              String output = Normalizer.clean(input, Form.NFD);
		String output = StringUtils.trim(input);

//              String output = input;
		// Remove all kinds of quotation marks
		output = StringUtils.replace(output, "´", "");
		output = StringUtils.replace(output, "`", "");
		output = StringUtils.replace(output, "'", "");

		// output = StringUtils.replace(output, ".", "");
		output = StringUtils.replace(output, ",", "");
		output = StringUtils.replace(output, " - ", "");
		output = StringUtils.replace(output, "- ", "");
		output = StringUtils.replace(output, " -", "");
		output = StringUtils.replace(output, "=", "");
		output = StringUtils.replace(output, "<", "");
		output = StringUtils.replace(output, ">", "");

		// Remove Lucene special characters
		//
		// + - && || ! ( ) { } [ ] ^ " ~ * ? : \
		output = StringUtils.replace(output, "+", "");

		// output = StringUtils.replace(output, "-", "");
		output = StringUtils.replace(output, "&&", "");
		output = StringUtils.replace(output, "||", "");
		output = StringUtils.replace(output, "!", "");
		output = StringUtils.replace(output, "(", "");
		output = StringUtils.replace(output, ")", "");
		output = StringUtils.replace(output, "{", "");
		output = StringUtils.replace(output, "}", "");
		output = StringUtils.replace(output, "[", "");
		output = StringUtils.replace(output, "]", "");
		output = StringUtils.replace(output, "^", "");
		output = StringUtils.replace(output, "\"", "");
		output = StringUtils.replace(output, "~", "");
		output = StringUtils.replace(output, "*", "");
		output = StringUtils.replace(output, "?", "");
		output = StringUtils.replace(output, ":", "");
		output = StringUtils.replace(output, "\\", "");

		return output;
	}

	public static String escapeForLucene(String input) {

		StringBuilder output = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {

			char c = input.charAt(i);

			if (specialChars.contains(c) || Character.isWhitespace(c)) {

				output.append('\\');
			}

			output.append(c);

		}

		return output.toString();

	}
	
	/*
	public static String escapeForLuceneExact(String input) {

		if (input == null) {

			return null;
		}

		StringBuilder output = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {

			char c = input.charAt(i);

			if (specialCharsExact.contains(c) || Character.isWhitespace(c)) {

				output.append('\\');
			}

			output.append(c);

		}

		return output.toString();

	}
	*/
	
	//~--- get methods ----------------------------------------------------

	/**
	 * Return a list with all nodes matching the given string
	 *
	 * Internally, the wildcard character '*' will be appended to the string.
	 *
	 * @param string
	 * @return
	 */
	public static List<String> getNodeNamesLike(SecurityContext securityContext, final String string) {

		List<String> names                = new LinkedList<String>();
		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		// always add wildcard character '*' for auto completion
		searchAttrs.add(Search.andName(string + SearchAttribute.WILDCARD));

		try {

			Result<AbstractNode> result = Services.command(securityContext, SearchNodeCommand.class).execute(searchAttrs);

			if (result != null) {

				for (AbstractNode node : result.getResults()) {

					names.add(node.getName());
				}

			}

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to execute SearchNodeCommand", fex);

		}

		return names;

	}
	
	/**
	 * Expand a search string by splitting at ',' and add the parts to an exact
	 * 'OR' search attribute group, combined by the given occur
	 * 
	 * @param searchValue 
	 */
	public static SearchAttributeGroup orMatchExactValues(final PropertyKey key, final String searchValue, final Occur occur) {
		
		SearchAttributeGroup group = new SearchAttributeGroup(Occur.SHOULD);
		
		if (searchValue == null || StringUtils.isBlank(searchValue)) {
			return null;
		}
		
		String[] parts = StringUtils.split(searchValue, ",");
		
		for (String part : parts) {
			
			SearchAttribute attr = new PropertySearchAttribute(key, part, occur, true);
			
			group.add(attr);
			
		}
		
		return group;
	}
	
	/**
	 * Expand a search string by splitting at ',' and add the parts to a loose
	 * 'OR' search attribute group, combined by the given occur
	 * 
	 * @param searchValue 
	 */
	public static SearchAttributeGroup orMatchValues(final PropertyKey key, final String searchValue, final Occur occur) {
		
		SearchAttributeGroup group = new SearchAttributeGroup(Occur.SHOULD);
		
		if (searchValue == null || StringUtils.isBlank(searchValue)) {
			return null;
		}
		
		String[] parts = StringUtils.split(searchValue, ",");
		
		for (String part : parts) {
			
			SearchAttribute attr = new PropertySearchAttribute(key, part, occur, false);
			
			group.add(attr);
			
		}
		
		return group;
	}
	
	/**
	 * Expand a search string by splitting at ',' and add the parts to an exact
	 * 'AND' search attribute group, combined by the given occur
	 * 
	 * @param searchValue 
	 */
	public static SearchAttributeGroup andMatchExactValues(final SecurityContext securityContext, final PropertyKey key, final String searchValue, final Occur occur) {
		
		SearchAttributeGroup group = new SearchAttributeGroup(Occur.MUST);
		
		if (searchValue == null || StringUtils.isBlank(searchValue)) {
			return null;
		}
		
		String[] parts = StringUtils.split(searchValue, ",");
		
		for (String part : parts) {
	
			group.add(key.getSearchAttribute(securityContext, occur, part, true));
		}
		
		return group;
	}
	
	/**
	 * Expand a search string by splitting at ',' and add the parts to a loose
	 * 'AND' search attribute group, combined by the given occur
	 * 
	 * @param searchValue 
	 */
	public static SearchAttributeGroup andMatchValues(final SecurityContext securityContext, final PropertyKey key, final String searchValue, final Occur occur) {
		
		SearchAttributeGroup group = new SearchAttributeGroup(Occur.MUST);
		
		if (searchValue == null || StringUtils.isBlank(searchValue)) {
			return null;
		}
		
		String[] parts = StringUtils.split(searchValue, ",");
		
		for (String part : parts) {
			
			group.add(key.getSearchAttribute(securityContext, occur, part, false));
		}
		
		return group;
	}	
}
