/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.structr.common.error.FrameworkException;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.UiFunction;

import java.util.HashMap;
import java.util.Map;

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
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {
			assertArrayHasMinLengthAndAllElementsNotNull(sources, 3);

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

					try {

						final String gctAPIKey = TranslationModule.TranslationGoogleAPIKey.getValue();

						if (gctAPIKey == null || gctAPIKey.isEmpty()) {
							logger.error("Google Cloud Translation API Key not configured in structr.conf");
							return "";
						}

						final Translate translate = TranslateOptions.newBuilder().setApiKey(gctAPIKey).build().getService();

						Translation translation = translate.translate(
							text,
							TranslateOption.sourceLanguage(sourceLanguage),
							TranslateOption.targetLanguage(targetLanguage)
						);

						return translation.getTranslatedText();
					}

					catch (TranslateException te) {
						throw new FrameworkException(422, "Could not translate text: " + text + "\nAPI Response: " + te.getLocalizedMessage());
					}
				}

				case "deepl": {

					final String deeplAPIKey = TranslationModule.TranslationDeepLAPIKey.getValue();
					Gson gson = new Gson();

					if (deeplAPIKey == null || deeplAPIKey.isEmpty()) {
						logger.error("DeepL Translation API Key not configured in structr.conf");
						return "";
					}

					final String apiBaseURL = deeplAPIKey.contains(":fx") ? "https://api-free.deepl.com/v2/translate" : "https://api.deepl.com/v2/translate";

					final Map<String, String> headers = Map.of("Authorization", "DeepL-Auth-Key "+deeplAPIKey, "Content-Type", "application/json");

					Map<String, Object> requestJson = new HashMap<>();
					requestJson.put("text", new String[]{text});
					requestJson.put("target_lang", targetLanguage.toUpperCase());
					requestJson.put("source_lang", sourceLanguage.toUpperCase());

					final Map<String, Object> responseData = HttpHelper.post(apiBaseURL, gson.toJson(requestJson), null, null, headers, "UTF-8", true);

						final String response = responseData.get(HttpHelper.FIELD_BODY) instanceof String ? (String) responseData.get(HttpHelper.FIELD_BODY) : null;

						final JsonObject resultObject = new JsonParser().parse(response).getAsJsonObject();

						if (resultObject.has("translations")) {

							final JsonArray translations = (JsonArray) resultObject.getAsJsonArray("translations");

							if (!translations.isEmpty()) {
								final JsonObject translation = translations.get(0).getAsJsonObject();
								return translation.get("text").getAsString();
							}
						}
						else
						{
							throw new FrameworkException(422, "Could not translate text: " + text + "\nAPI Response: " + response);
						}
				}
				default:
					logger.error("Unknown translation provider - possible values are 'google' and 'deepl'.");

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
