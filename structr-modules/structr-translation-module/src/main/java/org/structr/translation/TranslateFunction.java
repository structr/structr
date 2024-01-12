/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.UiFunction;

public class TranslateFunction extends UiFunction {

	public static final String ERROR_MESSAGE_TRANSLATE    = "Usage: ${translate(text, sourceLanguage, targetLanguage[, translationProvider])}. Supported translation providers: google, deepl. Example: ${translate(\"Hello world!\", \"en\", \"ru\", \"deepl\")}";
	public static final String ERROR_MESSAGE_TRANSLATE_JS = "Usage: ${{Structr.translate(text, sourceLanguage, targetLanguage[, translationProvider])}}. Supported translation providers: google, deepl. Example: ${{Structr.translate(\"Hello world!\", \"en\", \"ru\", \"deepl\"))}";

	@Override
	public String getName() {
		return "translate";
	}

	@Override
	public String getSignature() {
		return "text, sourceLanguage, targetLanguage, translationProvider";
	}

	@Override
	public String getRequiredModule() {
		return "translation";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {
			assertArrayHasMinLengthAndAllElementsNotNull(sources, 3);

			try {

				final String text = sources[0].toString();
				final String sourceLanguage = sources[1].toString();
				final String targetLanguage = sources[2].toString();

				// default is google
				String translationProvider = "google";

				if (sources.length == 4 && sources[3] instanceof String) {
					translationProvider = (String) sources[3];
				}

				switch (translationProvider) {

					case "google": {

						final String gctAPIKey = TranslationModule.TranslationGoogleAPIKey.getValue();

						if (gctAPIKey == null) {
							logger.error("Google Cloud Translation API Key not configured in structr.conf");
							return "";
						}

						final Translate translate = TranslateOptions.builder().apiKey(gctAPIKey).build().service();

						Translation translation = translate.translate(
							text,
							TranslateOption.sourceLanguage(sourceLanguage),
							TranslateOption.targetLanguage(targetLanguage)
						);

						return translation.translatedText();
					}
					case "deepl": {

						final String deeplAPIKey = TranslationModule.TranslationDeepLAPIKey.getValue();

						if (deeplAPIKey == null) {
							logger.error("DeepL Translation API Key not configured in structr.conf");
							return "";
						}

						final String apiBaseURL = deeplAPIKey.contains(":fx") ? "https://api-free.deepl.com/v2/translate" : "https://api.deepl.com/v2/translate";

						final String response = HttpHelper.get(apiBaseURL + "?text=" + encodeURL(text)
								+ "&source_lang=" + sourceLanguage.toUpperCase()
								+ "&target_lang=" + targetLanguage.toUpperCase()
								+ "&auth_key=" + deeplAPIKey,
								"UTF-8");

						final JsonObject resultObject = new JsonParser().parse(response).getAsJsonObject();
						final JsonArray translations = (JsonArray) resultObject.getAsJsonArray("translations");

						if (translations.size() > 0) {
							final JsonObject translation = translations.get(0).getAsJsonObject();
							return translation.get("text").getAsString();
						}
					}
				}

			} catch (TranslateException te) {

				logger.error("TranslateException: {}", te.getLocalizedMessage());

			} catch (Throwable t) {

				logException(t, "{}: Exception for parameter: {}", new Object[] { getReplacement(), getParametersAsString(sources) });
			}

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_TRANSLATE_JS : ERROR_MESSAGE_TRANSLATE);
	}

	@Override
	public String shortDescription() {
		return "Translates the given string from the source language to the target language.";
	}
}
