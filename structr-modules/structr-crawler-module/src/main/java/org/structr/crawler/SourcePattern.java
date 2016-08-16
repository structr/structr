/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.crawler;

import org.structr.core.entity.AbstractNode;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.Actions;
import org.structr.core.Export;
import org.structr.common.View;
import java.util.Map;
import org.structr.core.property.*;

public class SourcePattern extends AbstractNode {

	public static final Property<java.util.List<SourcePattern>> subPatternsProperty = new EndNodes<>("subPatterns", SourcePatternSUBSourcePattern.class).dynamic();
	public static final Property<SourcePage> subPageProperty = new EndNode<>("subPage", SourcePatternSUBPAGESourcePage.class).dynamic();
	public static final Property<SourcePage> sourcePageProperty = new StartNode<>("sourcePage", SourcePageUSESourcePattern.class).dynamic();
	public static final Property<SourcePattern> parentPatternProperty = new StartNode<>("parentPattern", SourcePatternSUBSourcePattern.class).dynamic();
	public static final Property<java.lang.Long> fromProperty = new LongProperty("from").indexed().dynamic();
	public static final Property<java.lang.Long> toProperty = new LongProperty("to").indexed().dynamic();
	public static final Property<java.lang.String> selectorProperty = new StringProperty("selector").indexed().dynamic();
	public static final Property<java.lang.String> elementIdProperty = new StringProperty("elementId").indexed().dynamic();
	public static final Property<java.lang.String> elementClassProperty = new StringProperty("elementClass").indexed().dynamic();
	public static final Property<java.lang.String> mappedTypeProperty = new StringProperty("mappedType").indexed().dynamic();
	public static final Property<java.lang.String> mappedAttributeProperty = new StringProperty("mappedAttribute").indexed().dynamic();

	public static final View uiView = new View(SourcePattern.class, "ui",
		subPatternsProperty, subPageProperty, sourcePageProperty, parentPatternProperty, fromProperty, toProperty, selectorProperty, elementIdProperty, elementClassProperty, mappedTypeProperty, mappedAttributeProperty
	);

	@Export
	public Object extract(final Map<String, Object> parameters) throws FrameworkException {

		return Actions.execute(securityContext, this, "${{\n\nvar pattern = Structr.get('this');\nvar page = pattern.sourcePage;\nvar subPatterns = pattern.subPatterns;\nvar from = pattern.from;\nvar to = pattern.to;\n\n//Structr.log('url: ' , page.url);\n\nvar content = Structr.GET(page.url, 'text/html');\nvar doc = org.jsoup.Jsoup.parse(content);\n//Structr.log(content);\n// var parts = content.split('\\n');\n\n//var parts = Structr.GET(page.url, 'text/html', pattern.selector);\nvar parts = doc.select(pattern.selector).toArray();\n\n//Structr.log(pattern.selector);\nStructr.log('extracted ', parts.length, ' elements from ', page.url, ' with selector ', pattern.selector);\n\nvar i = 1;\n\nif (from != null) {\n  i = from;\n}\n\nparts.every(function(part) {\n\n  //Structr.log('Extracted part: ', part);\n//  doc.body().html(part);\n  //Structr.log('document: ', doc);\n  \n  var obj = Structr.create(pattern.mappedType);\n\n\n  subPatterns.forEach(function(subPattern) {\n\n    var selector = pattern.selector + ':nth-child(' + i + ') > ' + subPattern.selector;\n    var ex = doc.select(selector).text();\n    Structr.log('(', parseInt(i).toString(), ') ', ex, ' from ', selector);\n\n    if (subPattern.mappedAttribute != null) {\n      obj[subPattern.mappedAttribute] = ex;\n    } else if (subPattern.subPage != null) {\n\n      var subUrl = page.url.match(/^(?:https?:\\/\\/)?(\u200C\u200B?:[^@\\/\\n]+@)?(?:www\\\u200C\u200B.)?([^:\\/\\n]+)/im)[0] + doc.select(selector).attr('href');\n      Structr.log('sub href: ', subUrl);\n      var subContent = Structr.GET(subUrl, 'text/html');\n      //Structr.log('sub content: ', subContent);\n      Structr.log('sub page: ', subPattern.subPage);\n      \n      var subDoc = org.jsoup.Jsoup.parse(subContent);\n\n      subPattern.subPage.patterns.forEach(function(subPagePattern) {\n\n        Structr.log('sub page selector: ', subPagePattern.selector);\n        var subEx = subDoc.select(subPagePattern.selector).text();\n        Structr.log('sub ex: ', subEx);\n\n        if (subPagePattern.mappedType != null) {\n\n          Structr.log('sub page pattern has mapped type: ', subPagePattern.mappedType);\n          Structr.log('sub page pattern selector: ', subPagePattern.selector);\n          var subParts = subDoc.select(subPagePattern.selector).toArray();\n\n          Structr.log(subParts.length, ' sub page pattern parts found');\n\n          var j = 1;\n\n          subParts.every(function(subPart) {\n\n            Structr.log('found sub page pattern part: ', subPart);\n\n            var subObj = Structr.create(subPagePattern.mappedType);\n\n            subPagePattern.subPatterns.forEach(function(subPageSubPattern) {\n\n              var subPageSubPatternSelector = subPagePattern.selector + ':nth-child(' + j + ') > ' + subPageSubPattern.selector;\n\n              var subSubEx = subDoc.select(subPageSubPatternSelector).text();\n              Structr.log('sub sub ex: ', subSubEx);\n              Structr.log('sub page sub selector: ', subPageSubPatternSelector);\n\n              if (subSubEx != null && subSubEx !== '' && subPageSubPattern.mappedAttribute != null) {\n                subObj[subPageSubPattern.mappedAttribute] = subSubEx;\n              }\n\n            });\n\n            obj[subPagePattern.mappedAttribute].push(subObj);\n\n          });\n\n        } else {\n\n          if (subEx != null && subEx !== '' && subPagePattern.mappedAttribute != null) {\n            obj[subPagePattern.mappedAttribute] = subEx;\n          }\n        }\n\n        j++;\n        return true;\n\n      });\n\n    }\n\n  });\n\n  if (to != null && to == i) {\n    return false;\n  }\n\n  i++;\n  return true;\n\n});\n\nStructr.log('######### extraction finished ############');\n\n}}", parameters);

	}

}