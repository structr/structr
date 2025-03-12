/*
 * Copyright (C) 2010-2024 Structr GmbH
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
require.config({
	baseUrl: location.origin + location.pathname,
	paths: {
		vs: 'js/lib/monaco-editor/min/vs'
	}
});
require(['vs/editor/editor.main'], () => {
	_Editors.setupMonacoAutoCompleteOnce();

	// adjust the HTML parser to support our ${{ .. }} syntax for JavaScript
	// This uses private monaco API ".loader()", which might break in future versions!
	monaco.languages.getLanguages().filter(langModule => langModule.id === 'html')[0]?.loader().then(htmlModule => {

		// we need to insert our two rules anywhere before the last element
		let lastRootRule = htmlModule.language.tokenizer.root.pop();
		htmlModule.language.tokenizer.root.push([/.*\${{/, { token: 'keyword', bracket: '@open', next: '@structr', nextEmbedded: 'text/javascript' }]);
		htmlModule.language.tokenizer.root.push([/}}/,   { token: 'keyword', bracket: '@close' }]);
		htmlModule.language.tokenizer.root.push(lastRootRule);

		htmlModule.language.tokenizer.structr = [
			[/}}/, { token: "@rematch", next: "@pop", nextEmbedded: "@pop" }],
			[/[^}]+/, ""]
		]
	});

	// add custom definition provider, so we can hook into "Go to definition"
	monaco.languages.registerDefinitionProvider('javascript', {
		provideDefinition: (model, position, cancellationToken) => {

			if (Structr.isModuleActive(_Code)) {

				return new Promise((resolve) => {

					let wordAtPos = model.getWordAtPosition(position);
					// console.log(`Looking up definition for word (${wordAtPos.word}) at position ${position}`, wordAtPos);

					let definitionForCurrentModel = {
						uri: model.uri,
						range: new monaco.Range(position.lineNumber, wordAtPos.startColumn, position.lineNumber, wordAtPos.endColumn)
					};

					let queryURL = `/structr/rest/NodeInterface?type=SchemaMethod;SchemaNode;SchemaProperty&name=${wordAtPos.word}`;

					fetch(queryURL).then(response => {
						if (response.ok) {
							return response.json();
						}
					}).then(data => {

						let getPropertyNameForEntity = (entity) => {
							switch (entity.type) {
								case 'SchemaMethod':
									return 'source';
								case 'SchemaNode':
								case 'SchemaProperty':
									return entity.name;
							}

							return 'source';
						};

						let getSourcePreviewForEntity = (entity) => {
							switch (entity.type) {
								case 'SchemaMethod':
									return entity.source;
								case 'SchemaNode':
									return 'Schema Type';
								case 'SchemaProperty':
									return 'Schema Attribute on type: ' + entity.schemaNode?.name;
							}

							return 'unable to show preview';
						};

						// transform results to definitions
						let foundDefinitions = data.result.map(entity => {

							let propertyName = getPropertyNameForEntity(entity);
							let modelUri     = _Editors.getModelURI(entity, propertyName);

							if (!monaco.editor.getModel(modelUri)) {

								// model must exist for preview
								monaco.editor.createModel(getSourcePreviewForEntity(entity), 'javascript', _Editors.getModelURI(entity, propertyName, {
									isFromCustomDefinitionProvider: true,
									structr_entity: entity
								}));
							}

							return {
								uri: modelUri,
								range: new monaco.Range(0, 0, 0, 0)
								// re: range: the name of the result will not show up in the source, thus, we do not want text highlighting and use a range before line 1
							};
						});

						let definitions = [
							definitionForCurrentModel,	// without the definition for the current model, the cursor does not change (unless there is more than 1 definition)
							...foundDefinitions
						];

						// console.log(`found ${definitions.length} definitions`);

						resolve(definitions);
					});
				});
			}

			return undefined;
		}
	});

	monaco.editor.registerEditorOpener({
		openCodeEditor(sourceEditor, resourceUri, selectionOrPosition) {

			if (Structr.isModuleActive(_Code)) {

				let targetModel = monaco.editor.getModel(resourceUri);

				if (targetModel.uri.isFromCustomDefinitionProvider === true) {

					if (_Code.persistence.isDirty()) {

						_Dialogs.confirmation.showPromise("You have unsaved changes, jump without saving?", false).then(result => {

							if (result === true) {

								_Code.persistence.forceNotDirty();

								_Code.helpers.navigateToSchemaObjectFromAnywhere(targetModel.uri.structr_entity, true);
								return true;
							}
						});

					} else {

						_Code.helpers.navigateToSchemaObjectFromAnywhere(targetModel.uri.structr_entity, true);
						return true;
					}

				} else {

					console.log('URI is not from custom definition provider... not doing anything');
				}
			}

			return false;
		}
	});

	monaco.editor.onDidCreateEditor(newEditor => {

		newEditor.onDidChangeModel(e => {

			// we currently never change models, this only serves as a helper for definition peek window to keep display-only models from being written to
			let allowWrite = (e.oldModelUrl === null || e.newModelUrl?.isFromCustomDefinitionProvider !== true);

			newEditor.updateOptions({
				readOnly: !allowWrite
			});
		});
	});

	// useless?
	// monaco.editor.registerLinkOpener({
	// 	open: function (resource) {
	// 		console.log('Custom link opener: ', resource)
	// 		return false;
	// 	}
	// });

	// show hover info...
	// monaco.languages.registerHoverProvider('javascript', {
	// 	provideHover: function (model, position) {
	//
	// 		// console.log('Hover Provider: ', model, position)
	//
	// 		let wordAtPos = model.getWordAtPosition(position);
	//
	// 		let definitionFound = (wordAtPos?.word === 'deactivatedAtTheMoment');
	//
	// 		if (definitionFound) {
	//
	// 			// TODO: create model
	// 			// TODO: get URI from model to put in the link
	//
	// 			return {
	// 				range: new monaco.Range(
	// 					2,
	// 					1,
	// 					2,
	// 					10
	// 				),
	// 				contents: [
	// 					{ value: "**SOURCE**" },
	// 					{
	// 						value:
	// 							'<a href="fcc64fd05ef64bf391607e7fd9c5954b">Hello</a> World!',
	// 						supportHtml: true
	// 					},
	// 				],
	// 			};
	// 		}
	//
	// 		return undefined;
	// 	},
	// });
});

let _Editors = {
	keyEditorOptions:         'structrEditorOptions_' + location.port,
	keyEditorViewStatePrefix: 'structrViewStatePrefix_' + location.port,
	editors: {
		/*	id: {
			property: {
				instance:  ...,
				model:     ...,
				viewState: ...,
				instanceDisposables: [],
				modelDisposables:    []
			},
			lastScriptErrorLookup: 0
		}	*/
	},
	getContainerForIdAndProperty: (id, propertyName) => {

		_Editors.editors[id]               = _Editors.editors?.[id] ?? {};
		_Editors.editors[id][propertyName] = _Editors.editors[id]?.[propertyName] ?? {};

		return _Editors.editors[id][propertyName];
	},
	getEditorInstanceForIdAndProperty: (id, propertyName) => {

		_Editors.editors[id]               = _Editors.editors?.[id] ?? {};
		_Editors.editors[id][propertyName] = _Editors.editors[id]?.[propertyName] ?? {};

		return _Editors.editors[id][propertyName].instance;
	},
	disposeAllUnattachedEditors: (exceptionIds = []) => {

		for (let id in _Editors.editors) {

			for (let propertyName in _Editors.editors[id]) {

				if (exceptionIds.indexOf(id) === -1) {

					let container = _Editors.getContainerForIdAndProperty(id, propertyName);
					let domNode   = container?.instance?.getDomNode();
					if (domNode === null || domNode === undefined || domNode.offsetParent === null) {
						_Editors.disposeEditor(id, propertyName);
					}
				}
			}
		}
	},
	disposeAllEditors: (exceptionIds = []) => {

		for (let id in _Editors.editors) {

			for (let propertyName in _Editors.editors[id]) {

				if (exceptionIds.indexOf(id) === -1) {
					_Editors.disposeEditor(id, propertyName);
				}
			}
		}
	},
	disposeEditor: (id, propertyName) => {

		let container = _Editors.getContainerForIdAndProperty(id, propertyName);
		_Editors.disposeInstanceForStorageContainer(container);

	},
	disposeEditorModel: (id, propertyName) => {

		let container = _Editors.getContainerForIdAndProperty(id, propertyName);
		_Editors.disposeModelForStorageContainer(container);
	},
	disposeInstanceForStorageContainer: (container) => {

		if (container.instance && container.instance.customConfig) {
			// before disposing the instance, remove our customConfig to enable garbage collection
			container.instance.customConfig = null;
		}

		container?.instance?.dispose();
		delete container?.instance;

		// dispose previous disposables
		for (let disposable of container?.instanceDisposables ?? []) {
			disposable.dispose();
		}
		delete container?.instanceDisposables;
	},
	disposeModelForStorageContainer: (container) => {

		container?.model?.dispose();
		delete container?.model;

		for (let disposable of container?.modelDisposables ?? []) {
			disposable.dispose();
		}
		delete container?.modelDisposables;
	},
	nukeAllEditors: () => {

		for (let id in _Editors.editors) {
			_Editors.nukeEditorsById(id);
		}
	},
	nukeEditorsById: (id) => {

		for (let propertyName in _Editors.editors?.[id] ?? {}) {
			_Editors.nukeEditor(id, propertyName);
		}

		delete _Editors.editors?.[id];
	},
	nukeEditor: (id, propertyName) =>{

		let container = _Editors.getContainerForIdAndProperty(id, propertyName);

		_Editors.disposeInstanceForStorageContainer(container);
		_Editors.disposeModelForStorageContainer(container);

		delete _Editors.editors?.[id]?.[propertyName];
	},
	nukeEditorsInDomElement: (el) => {

		for (let monacoContainer of el.querySelectorAll('[data-monaco-entity-id][data-monaco-entity-property-name]')) {

			let id           = monacoContainer.dataset['monacoEntityId'];
			let propertyName = monacoContainer.dataset['monacoEntityPropertyName'];

			_Editors.nukeEditor(id, propertyName);
		}
	},
	saveViewState: (id, propertyName) => {

		let container       = _Editors.getContainerForIdAndProperty(id, propertyName);
		let viewState       = container?.instance?.saveViewState?.();
		container.viewState =  viewState;

		_Editors.saveViewStateInLocalStorage(id, propertyName, viewState);
	},
	restoreViewState: (id, propertyName) => {

		let storageContainer = _Editors.getContainerForIdAndProperty(id, propertyName);

		// 1. previously loaded editor
		let viewState = storageContainer?.instance?.saveViewState?.();

		// 2. previously stored viewState in-memory
		if (!viewState) {
			viewState = storageContainer?.viewState;
		}

		if (viewState) {

			_Editors.saveViewStateInLocalStorage(id, propertyName, viewState);

		} else {

			// 3. previously stored viewState in localstorage
			viewState = _Editors.restoreViewStateFromLocalStorage(id, propertyName);
		}

		return viewState;
	},
	getStorageKeyForViewState: (id, propertyName) => {
		return `${_Editors.keyEditorViewStatePrefix}_${id}_${propertyName}`;
	},
	saveViewStateInLocalStorage: (id, propertyName, viewState) => {
		return LSWrapper.setItem(_Editors.getStorageKeyForViewState(id, propertyName), viewState);
	},
	restoreViewStateFromLocalStorage: (id, propertyName) => {
		return LSWrapper.getItem(_Editors.getStorageKeyForViewState(id, propertyName));
	},
	updateMonacoLintingDecorations: async (entity, propertyName, errorPropertyName, forceUpdate = false) => {

		// prevent script error lookup for new/unsaved methods
		if (entity?.isNew === true) {
			return;
		}

		let storageContainer = _Editors.getContainerForIdAndProperty(entity.id, propertyName);

		// prevent script error lookup for same editor in less than 5 seconds
		let minTimeBetweenLookups = 5000;
		let lastLookup = storageContainer.lastScriptErrorLookup ?? 0;
		if (lastLookup + minTimeBetweenLookups < performance.now() || forceUpdate === true) {

			storageContainer.lastScriptErrorLookup = performance.now();

			// check linting updates for this element max every 10 seconds (unless forced)
			let newErrorEvents = await _Editors.getScriptErrors(entity, errorPropertyName);

			if (!storageContainer.instance) {
				storageContainer = _Editors.getContainerForIdAndProperty(entity.id, propertyName);
			}
			if (storageContainer.instance) {
				storageContainer?.decorationsCollection?.clear();
				storageContainer.decorationsCollection = storageContainer.instance.createDecorationsCollection(newErrorEvents);
			}
		}
	},
	getErrorPropertyNameForLinting: (entity, propertyName) => {

		let errorPropertyNameForLinting = propertyName;

		if (entity.type === 'SchemaMethod') {
			errorPropertyNameForLinting = entity.name;
		} else if (entity.type === 'SchemaProperty') {
			if (propertyName === 'readFunction') {
				errorPropertyNameForLinting = `getProperty(${entity.name})`;
			} else if (propertyName === 'writeFunction') {
				errorPropertyNameForLinting = `setProperty(${entity.name})`;
			}
		} else if (entity.type === 'Content' || entity.type === 'Template') {
			errorPropertyNameForLinting = 'content';
		} else if (entity.type === 'File') {
			errorPropertyNameForLinting = 'getInputStream';
		}

		return errorPropertyNameForLinting;
	},
	getScriptErrors: async (entity, errorAttributeName) => {

		let schemaType = entity?.type ?? '';
		let response   = await fetch(`${Structr.rootUrl}_runtimeEventLog?type=Scripting&seen=false&${Structr.getRequestParameterName('pageSize')}=100`);

		if (response.ok) {

			let eventLog = await response.json();
			let keys     = {};
			let events   = [];

			for (let runtimeEvent of eventLog.result) {

				if (runtimeEvent.data) {

					let message = runtimeEvent.data.message;
					let line    = runtimeEvent.data.row;
					let column  = runtimeEvent.data.column;
					let type    = runtimeEvent.data.type;
					let name    = runtimeEvent.data.name;
					let id      = runtimeEvent.data.id;

					if (
						(id === entity.id) &&
						(!type || type === schemaType) &&
						name === errorAttributeName
					) {

						let fromLine = line;
						let toLine   = runtimeEvent.data.endLine ? runtimeEvent.data.endLine : line;
						let fromCol  = column;
						let toCol    = runtimeEvent.data.endColumn ? runtimeEvent.data.endColumn + 1 : column;

						// column == 0 => error column unknown
						if (column === 0) {

							toLine  = line;
							fromCol = 0;
							toCol   = 0;
						}

						// prevent duplicate error messages
						let key = `${entity.id}.${entity.name}:${fromLine}:${fromCol}${toLine}:${toCol}`;
						if (!keys[key]) {

							keys[key] = true;
							let date = new Date(runtimeEvent.absoluteTimestamp);

							events.push({
								range: new monaco.Range(fromLine, fromCol, toLine, toCol),
								options: {
									isWholeLine: (fromLine === toLine &&  fromCol === toCol),
									className: 'monaco-editor-warning-line',
									glyphMarginClassName: _Icons.monacoGlyphMarginClassName,
									glyphMarginHoverMessage: {
										value: `[${date.toLocaleDateString()} ${date.toLocaleTimeString()}] __Scripting error__` + '\n\n' + message
									}
								}
							});
						}
					}
				}
			}

			return events;
		}
	},
	prevAnimFrameReqId_resizeVisibleEditors: undefined,
	resizeVisibleEditors: () => {

		_Helpers.requestAnimationFrameWrapper(_Editors.prevAnimFrameReqId_resizeVisibleEditors, () => {

			for (let id in _Editors.editors) {

				for (let propertyName in _Editors.editors[id]) {

					let monacoEditor = _Editors.editors[id][propertyName].instance;
					if (monacoEditor) {
						let domNode = monacoEditor.getDomNode();

						if (domNode !== null && domNode.offsetParent !== null) {

							_Editors.resizeEditor(monacoEditor);
						}
					}
				}
			}
		});
	},
	resizeEditor: (monacoEditor) => {

		// resize editor to minimal size...
		let editorMinimumHeight = 200;
		let editorMinimumWidth  = 200;

		monacoEditor.layout({
			width: editorMinimumWidth,
			height: editorMinimumHeight
		});

		// ... so that editor auto-layout works
		monacoEditor.layout();
	},
	setupMonacoAutoCompleteOnce: () => {

		if (window.monacoAutoCompleteSetupComplete !== true) {

			window.monacoAutoCompleteSetupComplete = true;

			// remove library completions (like DOM) the defaults
			let defaultJSCompilerOptions = monaco.languages.typescript.javascriptDefaults.getCompilerOptions();
			defaultJSCompilerOptions.noLib = true;
			monaco.languages.typescript.javascriptDefaults.setCompilerOptions(defaultJSCompilerOptions);

			let defaultCompletionProvider = {
				triggerCharacters: ['.', '('],
				provideCompletionItems: async (model, position, token) => {

					let textBefore = model.getValueInRange({startLineNumber: 0, startColumn: 0, endLineNumber: position.lineNumber, endColumn: position.column});
					let textAfter  = model.getValueInRange({startLineNumber: position.lineNumber, startColumn: position.column, endLineNumber: position.lineNumber+1, endColumn: 0});

					let isAutoscriptEnv        = model.uri.isAutoscriptEnv || false;
					let forceAllowAutoComplete = model.uri.forceAllowAutoComplete || false;
					let modelLanguage          = model.getLanguageId();
					let contentType            = (modelLanguage === 'javascript') ? 'text/javascript' : 'text';

					let doAutocomplete = forceAllowAutoComplete || modelLanguage === 'javascript' || isAutoscriptEnv === true;

					let fetchPromise = new Promise((resolve, reject) => {

						if (doAutocomplete) {

							Command.autocomplete(model.uri.structr_uuid, isAutoscriptEnv, textBefore, textAfter, position.lineNumber, position.column, contentType, (result) => {

								// prevent monaco from sorting the entries
								let len = ("" + result.length).length;
								let cnt = 0;

								resolve(
									result.map((hint) => {

										// see https://microsoft.github.io/monaco-editor/typedoc/interfaces/languages.CompletionItem.html#documentation
										return {
											sortText:      ("" + cnt++).padStart(len, '0'), // effectively disable sorting
											label:         hint.text,

											// is shown on the right of the initial popup and on top of the secondary popup
											detail:        hint.type ?? '',

											// is shown in the secondary popup under the "detail" line
											// see https://microsoft.github.io/monaco-editor/typedoc/interfaces/IMarkdownString.html
											documentation: {
												supportHtml: true,
												value:       hint.documentation
											},

											kind:          _Editors.getAutoCompletionHintKind(hint.type),
											insertText:    hint.replacement ?? hint.text
										}
									})
								);
							});

						} else {

							resolve([]);
						}
					});

					return {
						suggestions: await fetchPromise
					};
				}
			};

			// add identical provider to all languages and decide in the provider if completions should be shown
			for (let lang of monaco.languages.getLanguages()) {
				monaco.languages.registerCompletionItemProvider({ language: lang.id }, defaultCompletionProvider);
			}
		}
	},
	getAutoCompletionHintKind: (type) => {

		let lowercaseType = type.toLowerCase();

		// see https://microsoft.github.io/monaco-editor/typedoc/enums/languages.CompletionItemKind.html#Method
		switch (lowercaseType) {
			case "built-in function":
				return monaco.languages.CompletionItemKind.Function;

			case "method":
			case "global schema method":
				return monaco.languages.CompletionItemKind.Method;

			case "property":
				return monaco.languages.CompletionItemKind.Property;

			case "keyword":
				return monaco.languages.CompletionItemKind.Keyword;

			case "typeName":
				return monaco.languages.CompletionItemKind.Class;
		}

		return monaco.languages.CompletionItemKind.Function;
	},
	getPathForEntityAndPropertyName: (entity, propertyName) => {

		let path = `${entity.id}/${propertyName}`;

		switch (entity.type) {
			case 'SchemaMethod':
				path = `${entity.schemaNode?.isServiceClass ? 'Service' : 'Custom'}/${entity.schemaNode?.name}/${entity.name}/${propertyName}`	// must include propertyName because for methods "source" and "openAPIConfig" are created
				break;
			case 'SchemaNode':
				path = `${entity.isServiceClass ? 'Service' : 'Custom'}/${propertyName}`	// entity.name should be equal to propertyName
				break;
			case 'SchemaProperty':
				path = `${entity.schemaNode?.isServiceClass ? 'Service' : 'Custom'}/${entity.schemaNode?.name}/Property/${propertyName}`	// entity.name should be equal to propertyName
				break;
		}

		return path;

	},
	getModelURI: (entity, propertyName, extraInfo) => {

		let uri = monaco.Uri.from({
			scheme: 'file', // keeps history even after switching editors in code
			path:   _Editors.getPathForEntityAndPropertyName(entity, propertyName)
		});

		uri.structr_uuid     = entity.id;
		uri.structr_property = propertyName;

		for (let key in extraInfo) {
			uri[key]  = extraInfo[key];
		}

		return uri;
	},
	getTextForExistingEditor: (id, propertyName) => {
		return _Editors.getContainerForIdAndProperty(id, propertyName)?.instance?.getValue();
	},
	setTextForExistingEditor: (id, propertyName, text) => {
		return _Editors.getContainerForIdAndProperty(id, propertyName)?.instance?.setValue(text);
	},
	getMonacoEditor: (entity, propertyName, domElement, customConfig) => {

		let storageContainer = _Editors.getContainerForIdAndProperty(entity.id, propertyName);
		let editorText       = customConfig.value || entity[propertyName] || '';
		let language         = (customConfig.language === 'auto') ? _Editors.getMonacoEditorModeForContent(editorText, entity) : customConfig.language;
		let viewState        = _Editors.restoreViewState(entity.id, propertyName);

		let restoreModelConfig = _Editors.getSavedEditorOptions()?.restoreModels ?? false;
		let doRestoreModel     = (customConfig.preventRestoreModel !== true) && restoreModelConfig;

		if (doRestoreModel === false || !storageContainer.model) {

			_Editors.disposeModelForStorageContainer(storageContainer);

			// A bit hacky to transport additional configuration to deeper layers...
			let extraModelConfig = {
				isAutoscriptEnv:        customConfig.isAutoscriptEnv,
				forceAllowAutoComplete: customConfig.forceAllowAutoComplete
			}

			// find and dispose previously created models for same element (possibly done from definitionProvider)
			let modelUri = _Editors.getModelURI(entity, propertyName, extraModelConfig);
			let prevModel = monaco.editor.getModel(modelUri);
			prevModel?.dispose();

			storageContainer.model = monaco.editor.createModel(editorText, language, modelUri);
		}

		let monacoConfig = Object.assign(_Editors.getOurSavedEditorOptionsForEditor(), {
			model: storageContainer.model,
			value: editorText,
			language: language,
			readOnly: customConfig.readOnly,
			fixedOverflowWidgets: true
		});

		// dispose previously existing editors
		_Editors.disposeAllUnattachedEditors(customConfig.preventDisposeForIds);

		// also delete a possible previous editor for this id and propertyName to start fresh
		storageContainer?.instance?.dispose();

		// dispose previous disposables
		for (let disposable of storageContainer?.instanceDisposables ?? []) {
			disposable.dispose();
		}
		for (let disposable of storageContainer?.modelDisposables ?? []) {
			disposable.dispose();
		}

		let monacoInstance = monaco.editor.create(domElement, monacoConfig);

		domElement.dataset['monacoEntityId']           = entity.id;
		domElement.dataset['monacoEntityPropertyName'] = propertyName;

		storageContainer.instance            = monacoInstance;
		storageContainer.instanceDisposables = [];
		storageContainer.modelDisposables    = [];

		// save initial customConfig on object to be able to alter it later without re-creating the editor
		monacoInstance.customConfig = customConfig;

		if (viewState) {
			monacoInstance.restoreViewState(viewState);
		}

		let errorPropertyNameForLinting = _Editors.getErrorPropertyNameForLinting(entity, propertyName);
		if (customConfig.lint === true) {

			_Editors.updateMonacoLintingDecorations(entity, propertyName, errorPropertyNameForLinting, true);

			// onFocus handler
			storageContainer.instanceDisposables.push(monacoInstance.onDidFocusEditorText((e) => {
				_Editors.updateMonacoLintingDecorations(entity, propertyName, errorPropertyNameForLinting);
			}));
		}

		// change handler
		storageContainer.modelDisposables.push(storageContainer.model.onDidChangeContent((e) => {
			_Editors.defaultChangeHandler(e, entity, propertyName, errorPropertyNameForLinting);
		}));

		// cursor change handler
		storageContainer.instanceDisposables.push(monacoInstance.onDidChangeCursorPosition((e) => {
			_Editors.saveViewState(entity.id, propertyName);
		}));

		if (customConfig.saveFn) {

			storageContainer.instanceDisposables.push(monacoInstance.addAction({
				id: `editor-save-action-${entity.id}-${propertyName}`,
				label: customConfig.saveFnText || 'Save',
				keybindings: [
					monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS
				],
				precondition: null,
				keybindingContext: null,
				contextMenuGroupId: '1_modification',
				contextMenuOrder: 1.5,
				run: (editor) => {

					customConfig.saveFn(editor, entity);

					if (monacoInstance.customConfig.lint === true) {
						_Editors.updateMonacoLintingDecorations(entity, propertyName, errorPropertyNameForLinting);
					}
				}
			}));
		}

		storageContainer.instanceDisposables.push(monacoInstance.addAction({
			id: `editor-toggle-fullscreen-action-${entity.id}-${propertyName}`,
			label: 'Toggle Maximized',
			keybindings: [
				monaco.KeyMod.Shift | monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyF
			],
			precondition: null,
			keybindingContext: null,
			// uncomment the following lines to add to context menu
			// contextMenuGroupId: '1_modification',
			// contextMenuOrder: 1.5,
			run: (editor) => {

				_Editors.toggleFullscreen(editor);
			}
		}));

		return monacoInstance;
	},
	toggleFullscreen: (editor) => {

		let fullscreenClass = 'monaco-editor-maximized';
		let containerNode   = editor.getContainerDomNode();
		let toggle          = containerNode.classList.contains(fullscreenClass);

		containerNode.classList.toggle(fullscreenClass, !toggle);

		_Editors.resizeEditor(editor);
	},
	isAutoFocusAllowed: () => {

		// Only limitation currently: If we are in the code area and currently searching for something
		return !(Structr.isModuleActive(_Code) && _Code.search.inSearchBox());
	},
	focusEditor: (editor) => {

		let allowed = _Editors.isAutoFocusAllowed();

		if (allowed) {
			editor.focus();
		}

		return allowed;
	},
	defaultChangeHandler: (e, entity, propertyName, errorPropertyNameForLinting) => {

		let storageContainer = _Editors.getContainerForIdAndProperty(entity.id, propertyName);

		if (storageContainer.instance.customConfig.language === 'auto') {
			let newLang = _Editors.getMonacoEditorModeForContent(storageContainer.instance.getValue(), entity);
			monaco.editor.setModelLanguage(storageContainer.model, newLang);
		}

		if (storageContainer.instance.customConfig.lint === true) {
			_Editors.updateMonacoLintingDecorations(entity, propertyName, errorPropertyNameForLinting);
		}

		if (storageContainer.instance.customConfig.changeFn) {
			storageContainer.instance.customConfig.changeFn(storageContainer.instance, entity, propertyName);
		}
	},
	getMonacoEditorModeForContent: (content, entity) => {

		if (entity && entity.codeType === 'java') {
			return 'java';
		}

		let mode = 'text';

		if (content) {

			let trimmed = content.trim();

			if (trimmed.startsWith('python{')) {
				mode = 'python';
			} else if (trimmed.startsWith('R{')) {
				mode = 'r';
			} else if (trimmed.startsWith('{')) {
				mode = 'javascript';
			}
		}

		return mode;
	},
	updateMonacoEditorLanguage: (editor, newLanguage, entity) => {

		if (newLanguage === 'auto') {
			newLanguage = _Editors.getMonacoEditorModeForContent(editor.getValue(), entity);
		}
		monaco.editor.setModelLanguage(editor.getModel(), newLanguage);
	},
	addEscapeKeyHandlersToPreventPopupClose: (id, propertyName, editor) => {

		let contextActions = {
			suggestWidgetVisible:           () => { editor.trigger('keyboard', 'hideSuggestWidget'); },
			findWidgetVisible:              () => { editor.trigger('keyboard', 'closeFindWidget'); },
			referenceSearchVisible:         () => { editor.trigger('keyboard', 'closeReferenceSearch'); },
			markersNavigationVisible:       () => { editor.trigger('keyboard', 'closeMarkersNavigation'); },
			renameInputVisible:             () => { editor.trigger('keyboard', 'cancelRenameInput'); },
			// command palette should also be here... if I could only find out the correct "context" name for this thing
		};

		let container = _Editors.getContainerForIdAndProperty(id, propertyName);

		for (let [context, action] of Object.entries(contextActions)) {

			let commandDisposable = editor.addAction({
				label: '',
				id: context,
				keybindings: [monaco.KeyCode.Escape],
				keybindingContext: context,
				run: (editor) => {
					Structr.ignoreKeyUp = true;
					action();
				}
			});

			container.instanceDisposables.push(commandDisposable);
		}
	},
	getDefaultEditorOptionsForStorage: () => {
		/**
		 * This is almost identical to IEditorOptions (https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.IStandaloneEditorConstructionOptions.html)
		 * but adapted so we can seamlessly use it in our UI.
		 *
		 * The return value of this function must be transformed to be used with the editor
		 */
		return {
			// section identical to IEditorOptions
			roundedSelection: true,
			glyphMargin: true,
			scrollBeyondLastLine: true,
			readOnly: false,
			renderLineHighlight: 'all',
			folding: true,
			showFoldingControls: 'always',
			theme: "vs",
			wrappingIndent: 'none',
			mouseWheelZoom: false,
			renderWhitespace: false,
			wordWrap: 'off',
			autoIndent: 'advanced',
			tabSize: 4,
			detectIndentation: false,

			// section different from IEditorOptions
			minimapEnabled: true,
			indentation: 'tabs'
		};
	},
	getOurSavedEditorOptionsForEditor: () => {
		return _Editors.transformSavedOptionsForEditor(_Editors.getOurEditorOptionsForDOM());
	},
	getOurEditorOptionsForDOM: () => {
		return Object.assign(_Editors.getDefaultEditorOptionsForStorage(), _Editors.getSavedEditorOptions());
	},
	transformSavedOptionsForEditor: (savedOptions) => {

		savedOptions.minimap = {
			enabled: (savedOptions.minimapEnabled === true)
		};
		delete savedOptions.minimapEnabled;

		savedOptions.useSpaces    = (savedOptions.indentation === 'spaces');
		savedOptions.insertSpaces = (savedOptions.indentation === 'spaces');

		delete savedOptions.indentation;

		delete savedOptions.restoreModel;

		return savedOptions;
	},
	getSavedEditorOptions: () => {
		return LSWrapper.getItem(_Editors.keyEditorOptions, {});
	},
	setSavedEditorOptions: (options) => {
		LSWrapper.setItem(_Editors.keyEditorOptions, options);
	},
	serialize: () => {
		return JSON.stringify(_Editors.getSavedEditorOptions());
	},
	deserialize: (json) => {
		try {
			let serializedOptions = JSON.parse(json);

			_Editors.setSavedEditorOptions(Object.assign({}, serializedOptions));

			_Editors.updateAllEditorsWithEditorOptions();

		} catch (e) {
			console.log('Unable to parse editor configuration');
		}
	},
	updateSavedEditorOption: (optionName, value) => {

		let savedEditorOptions = _Editors.getSavedEditorOptions();
		savedEditorOptions[optionName] = value;

		_Editors.setSavedEditorOptions(Object.assign({}, savedEditorOptions));

		_Editors.updateAllEditorsWithEditorOptions();
	},
	updateAllEditorsWithEditorOptions: () => {

		let newOptions = _Editors.getOurSavedEditorOptionsForEditor();

		for (let id in _Editors.editors) {

			for (let propertyName in _Editors.editors[id]) {

				let monacoEditor = _Editors.editors[id][propertyName].instance;
				if (monacoEditor) {
					let domNode = monacoEditor.getDomNode();

					if (domNode !== null && domNode.offsetParent !== null) {

						monacoEditor.updateOptions(newOptions);
					}
				}
			}
		}
	},
	appendEditorOptionsElement: (element) => {

		let dropdown = _Helpers.createSingleDOMElementFromHTML(`
			<div class="editor-settings-popup dropdown-menu darker-shadow-dropdown dropdown-menu-large">
				<button class="btn dropdown-select hover:bg-gray-100 focus:border-gray-666 active:border-green" data-preferred-position-y="top" data-wants-fixed="true">
					${_Icons.getSvgIcon(_Icons.iconTextSettings, 16, 16, ['mr-2'])}
				</button>

				<div class="dropdown-menu-container" style="display: none;">
					<div class="font-bold pt-4 pb-2">Global Editor Settings</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Word Wrap</label>
						<select name="wordWrap" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>off</option>
							<option>on</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Wrapping Indent</label>
						<select name="wrappingIndent" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>none</option>
							<option>same</option>
							<option>indent</option>
							<option>deepIndent</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Indentation</label>
						<select name="indentation" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>tabs</option>
							<option>spaces</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Tab Size</label>
						<input name="tabSize" type="number" min="1" value="8" style="width: 3rem;">
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Auto-Indent</label>
						<select name="autoIndent" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>advanced</option>
							<option>none</option>
							<option>full</option>
							<option>brackets</option>
							<option>keep</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Auto-Surround</label>
						<select name="autoSurround" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>languageDefined</option>
							<option>never</option>
							<option>quotes</option>
							<option>brackets</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Auto-Closing Quotes</label>
						<select name="autoClosingQuotes" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>always</option>
							<option>languageDefined</option>
							<option>beforeWhitespace</option>
							<option>never</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Theme</label>
						<select name="theme" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option value="vs">Visual Studio</option>
							<option value="vs-dark">Visual Studio Dark</option>
							<option value="hc-black">High Contrast Dark</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Line Highlight Rendering</label>
						<select name="renderLineHighlight" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>all</option>
							<option>gutter</option>
							<option>line</option>
							<option>none</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label><input name="folding" type="checkbox"> Enable Code Folding</label>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Show Folding Controls</label>
						<select name="showFoldingControls" class="min-w-48 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							<option>always</option>
							<option>mouseover</option>
						</select>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label><input name="renderWhitespace" type="checkbox"> Render Whitespace</label>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label><input name="roundedSelection" type="checkbox"> Rounded selection</label>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label><input name="mouseWheelZoom" type="checkbox"> Mouse Wheel Zoom</label>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label><input name="scrollBeyondLastLine" type="checkbox"> Scroll beyond last line</label>
					</div>

					<div class="editor-setting flex items-center p-1">
						<label><input name="minimapEnabled" type="checkbox"> Show Mini Map</label>
					</div>

					<div class="font-bold pt-4 pb-2">Global Editor Behaviour Settings</div>

					<div class="editor-setting flex items-center p-1">
						<label data-comment="This means that <u>unsaved</u> changes are stored in-memory and such changes are restored even after opening other editors or user interfaces in the admin UI. They are only lost after a page reload or navigation event.<br><br>This may lead to confusing situations where the editor shows unsaved html/code and the html/code is not shown/run because it is not saved. Be aware of such situations!"><input name="restoreModels" type="checkbox"> Restore text models</label>
					</div>
				</div>
			</div>
		`);

		let savedOptions = _Editors.getOurEditorOptionsForDOM();

		// apply saved options
		for (let [key, value] of Object.entries(savedOptions)) {
			let settingControl = dropdown.querySelector(`[name="${key}"]`);
			if (settingControl) {
				if (settingControl.type === 'checkbox') {
					settingControl.checked = value;
				} else {
					settingControl.value = value;
				}
			}
		}

		for (let settingControlSelect of dropdown.querySelectorAll('select[name]')) {

			settingControlSelect.addEventListener('change', (e) => {
				_Editors.updateSavedEditorOption(settingControlSelect.name, settingControlSelect.value);

				_Helpers.blinkGreen(settingControlSelect.closest('.editor-setting'));
			});
		}

		for (let settingControlInput of dropdown.querySelectorAll('input[name]')) {

			settingControlInput.addEventListener('change', (e) => {
				let value = (settingControlInput.type === 'checkbox' ? settingControlInput.checked : settingControlInput.value);
				_Editors.updateSavedEditorOption(settingControlInput.name, value);

				_Helpers.blinkGreen(settingControlInput.closest('.editor-setting'));
			});
		}

		element.appendChild(dropdown);

		_Helpers.activateCommentsInElement(dropdown);
	}
};