/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.translation;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateException;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.UiFunction;


public class TranslateFunction extends UiFunction {

	public static final String ERROR_MESSAGE_TRANSLATE    = "Usage: ${translate(text[, sourceLanguage[, targetLanguage]])}. Example: ${translate(\"Hello world!\", \"en\", \"ru\")}";
	public static final String ERROR_MESSAGE_TRANSLATE_JS = "Usage: ${{Structr.translate(text[, sourceLanguage[, targetLanguage]])}}. Example: ${{Structr.translate(\"Hello world!\", \"en\", \"ru\"))}";

	@Override
	public String getName() {
		return "translate()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

			try {

				final String gctAPIKey = TranslationModule.TranslationAPIKey.getValue();

				if (gctAPIKey == null) {
					logger.error("Google Cloud Translation API Key not configured in structr.conf");
					return "";
				}

				final String text = sources[0].toString();
				final String sourceLanguage = sources[1].toString();
				final String targetLanguage = sources[2].toString();

				final Translate translate = TranslateOptions.builder().apiKey(gctAPIKey).build().service();

				Translation translation = translate.translate(
					text,
					TranslateOption.sourceLanguage(sourceLanguage),
					TranslateOption.targetLanguage(targetLanguage)
				);

				return translation.translatedText();

			} catch (TranslateException te) {

				logger.error("TranslateException: {}", te.getLocalizedMessage());

			} catch (Throwable t) {

				logException(t, "{}: Exception for parameter: {}", new Object[] { getName(), getParametersAsString(sources) });

			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_TRANSLATE_JS : ERROR_MESSAGE_TRANSLATE);
	}

	@Override
	public String shortDescription() {
		return "Translates the given string from the source language to the target language";
	}
}
