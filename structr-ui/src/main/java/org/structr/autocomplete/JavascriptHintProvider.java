package org.structr.autocomplete;

import org.structr.common.CaseHelper;

/**
 *
 * @author Christian Morgner
 */
public class JavascriptHintProvider extends AbstractHintProvider {

	@Override
	protected String getFunctionName(final String source) {

		if (source.contains("_")) {
			return CaseHelper.toLowerCamelCase(source);
		}

		return source;
	}

	@Override
	protected String visitReplacement(final String replacement) {
		return "Structr." + replacement;
	}

	@Override
	protected boolean isJavascript() {
		return true;
	}
}
