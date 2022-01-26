/*
 * Copyright (C) 2010-2021 Structr GmbH
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

require.config({ paths: { 'vs': 'js/lib/monaco-editor/min/vs' }});
require(['vs/editor/editor.main'], () => {
	_Editors.setupMonacoAutoCompleteOnce();
});

let _Editors = {
	keyEditorOptions:         'structrEditorOptions_' + location.port,
	keyEditorViewStatePrefix: 'structrViewStatePrefix_' + location.port,
	editors: {
		/*	id: {
			property: {
				instance: x,
				model: y,
				viewState: z,
				editorDisposables: [],
				modelDisposables: [],
				decorations: []
			}
		}	*/
	},
	getContainerForIdAndProperty: (id, propertyName) => {

		_Editors.editors[id]               = _Editors.editors?.[id] ?? {};
		_Editors.editors[id][propertyName] = _Editors.editors[id]?.[propertyName] ?? {};

		return _Editors.editors[id][propertyName];
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

		container?.instance?.dispose();
		delete container?.instance;

		// dispose previous disposables
		for (let disposable of container?.instanceDisposables ?? []) {
			disposable.dispose();
		}
		delete container?.instanceDisposables;
	},
	disposeEditorModel: (id, propertyName) => {

		let container = _Editors.getContainerForIdAndProperty(id, propertyName);
		_Editors.disposeModelForStorageContainer(container);
	},
	disposeModelForStorageContainer: (container) => {

		container?.model?.dispose();
		delete container?.model;

		for (let disposable of container?.modelDisposables ?? []) {
			disposable.dispose();
		}
		delete container?.modelDisposables;
	},
	nukeEditorsById: (id) => {

		for (let propertyName in _Editors.editors?.[id] ?? {}) {

			let container = _Editors.getContainerForIdAndProperty(id, propertyName);

			container?.instance.dispose();
			container?.model.dispose();

			// dispose previous disposables
			for (let disposable of container?.instanceDisposables ?? []) {
				disposable.dispose();
			}
			for (let disposable of container?.modelDisposables ?? []) {
				disposable.dispose();
			}
		}

		delete _Editors.editors?.[id];
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
	updateMonacoLintingDecorations: async (entity, propertyName, errorPropertyName) => {

		let storageContainer         = _Editors.getContainerForIdAndProperty(entity.id, propertyName);
		storageContainer.decorations = storageContainer?.decorations ?? [];
		let newErrorEvents           = await _Editors.getScriptErrors(entity, errorPropertyName);
		storageContainer.decorations = storageContainer.instance.deltaDecorations(storageContainer.decorations, newErrorEvents);
	},
	getScriptErrors: async function(entity, errorAttributeName) {

		let schemaType = entity?.type ?? '';
		let response   = await fetch(rootUrl + '_runtimeEventLog?type=Scripting&seen=false&' + Structr.getRequestParameterName('pageSize') + '=100');

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
						(!id || (entity.id && id === entity.id)) &&
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
						let key = entity.id + '.' + entity.name + ':' + fromLine + ':' + fromCol + toLine + ':' + toCol;
						if (!keys[key]) {

							keys[key] = true;

							events.push({
								range: new monaco.Range(fromLine, fromCol, toLine, toCol),
								options: {
									isWholeLine: (fromLine === toLine &&  fromCol === toCol),
									className: 'monaco-editor-warning-line',
									glyphMarginClassName: _Icons.getFullSpriteClass(_Icons.error_icon) + ' force-sprite-size',
									glyphMarginHoverMessage: {
										value: 'Scripting error: ' + message,
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
	resizeVisibleEditors: () => {

		requestAnimationFrame(() => {

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

		// first set to 0 to allow the parent box to auto-calc its height
		monacoEditor.getDomNode().style.height            = 0;
		monacoEditor.getDomNode().style.width             = 0;
		monacoEditor.getDomNode().firstChild.style.height = 0;
		monacoEditor.getDomNode().firstChild.style.width  = 0;

		monacoEditor.getDomNode().style.overflow          = 'hidden';
		monacoEditor.getDomNode().style.height            = '100%';
		monacoEditor.getDomNode().style.width             = '100%';
		monacoEditor.getDomNode().firstChild.style.width  = '100%';
		monacoEditor.getDomNode().firstChild.style.height = '100%';

		// let editor auto-layout
		monacoEditor.layout();
	},
	setupMonacoAutoCompleteOnce: () => {

		if (window.monacoAutoCompleteSetupComplete !== true) {

			window.monacoAutoCompleteSetupComplete = true;

			// experimental: remove all code completion except tokens (for javascript)
			monaco.languages.typescript.javascriptDefaults.setCompilerOptions({
				noLib: true,
				allowNonTsExtensions: true
			});

			let defaultCompletionProvider = {
				triggerCharacters: ['.', '('],
				provideCompletionItems: async (model, position, token) => {

					let textBefore = model.getValueInRange({startLineNumber: 0, startColumn: 0, endLineNumber: position.lineNumber, endColumn: position.column});
					let textAfter  = model.getValueInRange({startLineNumber: position.lineNumber, startColumn: position.column, endLineNumber: position.lineNumber+1, endColumn: 0});
					// let word       = model.getWordUntilPosition(position);

					let isAutoscriptEnv        = model.uri.isAutoscriptEnv || false;
					let forceAllowAutoComplete = model.uri.forceAllowAutoComplete || false;
					let modelLanguage          = model.getLanguageId();
					let contentType            = (modelLanguage === 'javascript') ? 'text/javascript' : 'text';

					let doAutocomplete = forceAllowAutoComplete || modelLanguage === 'javascript' || isAutoscriptEnv === true;

					let fetchPromise = new Promise((resolve, reject) => {

						if (doAutocomplete) {

							Command.autocomplete(model.uri.structr_uuid, isAutoscriptEnv, textBefore, textAfter, position.lineNumber, position.column, contentType, (result) => {
								resolve(
									result.map((hint) => {
										return {
											label: hint.text,
											insertText: hint.text,
											documentation: hint.displayText,
											kind: monaco.languages.CompletionItemKind.Property, // see https://microsoft.github.io/monaco-editor/api/enums/monaco.languages.completionitemkind.html
											// range: xxx
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
				monaco.languages.registerCompletionItemProvider(lang.id, defaultCompletionProvider);
			}
		}
	},
	getModelURI: (id, propertyName, extraInfo) => {

		let uri = monaco.Uri.from({
			scheme: 'file', // keeps history even after switching editors in code
			path: '/' + id + '/' + propertyName,
		});

		uri.structr_uuid     = id;
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
		let language         = (customConfig.language === 'auto') ? _Editors.getMonacoEditorModeForContent(editorText) : customConfig.language;
		let viewState        = _Editors.restoreViewState(entity.id, propertyName);

		let restoreModelConfig = _Editors.getSavedEditorOptions()?.restoreModels ?? false;
		let doRestoreModel     = (customConfig.preventRestoreModel !== true) && restoreModelConfig;

		if (doRestoreModel === false || !storageContainer.model) {

			_Editors.disposeModelForStorageContainer(storageContainer);

			// A bit hacky to transport additional configuration to deeper layers...
			let extraModelConfig = {
				isAutoscriptEnv: customConfig.isAutoscriptEnv,
				forceAllowAutoComplete: customConfig.forceAllowAutoComplete
			}

			storageContainer.model = monaco.editor.createModel(editorText, language, _Editors.getModelURI(entity.id, propertyName, extraModelConfig));
		}

		// dispose previously existing editors
		_Editors.disposeAllEditors([entity.id]);

		let monacoConfig = Object.assign(_Editors.getOurSavedEditorOptionsForEditor(), {
			model: storageContainer.model,
			value: editorText,
			language: language,
		});

		// dispose previous editor
		storageContainer?.instance?.dispose();

		// dispose previous disposables
		for (let disposable of storageContainer?.instanceDisposables ?? []) {
			disposable.dispose();
		}
		for (let disposable of storageContainer?.modelDisposables ?? []) {
			disposable.dispose();
		}

		let monacoInstance = monaco.editor.create(domElement.get(0), monacoConfig);

		storageContainer.instance            = monacoInstance;
		storageContainer.instanceDisposables = [];
		storageContainer.modelDisposables    = [];

		// save initial customConfig on object to be able to alter it later without re-creating the editor
		monacoInstance.customConfig = customConfig;

		if (viewState) {
			monacoInstance.restoreViewState(viewState);
		}

		let errorPropertyNameForLinting = _Code.getErrorPropertyNameForLinting(entity, propertyName);

		if (customConfig.lint === true) {

			_Editors.updateMonacoLintingDecorations(entity, propertyName, errorPropertyNameForLinting);

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
				id: 'editor-save-action-' + entity.id + '-' + propertyName,
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

		return monacoInstance;
	},
	defaultChangeHandler: (e, entity, propertyName, errorPropertyNameForLinting) => {

		let storageContainer = _Editors.getContainerForIdAndProperty(entity.id, propertyName);

		if (storageContainer.instance.customConfig.language === 'auto') {
			let newLang = _Editors.getMonacoEditorModeForContent(storageContainer.instance.getValue());
			monaco.editor.setModelLanguage(storageContainer.model, newLang);
		}

		if (storageContainer.instance.customConfig.lint === true) {
			_Editors.updateMonacoLintingDecorations(entity, propertyName, errorPropertyNameForLinting);
		}

		if (storageContainer.instance.customConfig.changeFn) {
			storageContainer.instance.customConfig.changeFn(storageContainer.instance, entity, propertyName);
		}
	},
	getMonacoEditorModeForContent: function(content) {
		return (content && content.trim().indexOf('{') === 0) ? 'javascript' : 'text';
	},
	updateMonacoEditorLanguage: (editor, newLanguage) => {
		if (newLanguage === 'auto') {
			newLanguage = _Editors.getMonacoEditorModeForContent(editor.getValue());
		}
		monaco.editor.setModelLanguage(editor.getModel(), newLanguage);
	},
	enableSpeechToTextForEditor: (editor, buttonArea) => {

		// Experimental speech recognition, works only in Chrome 25+
		if (typeof(webkitSpeechRecognition) === 'function') {

			buttonArea.insertAdjacentHTML('beforeend', `<button class="speechToText"><i class="${_Icons.getFullSpriteClass(_Icons.microphone_icon)}"></i></button>`);

			let speechToTextButton = buttonArea.querySelector('.speechToText');
			let speechBtn          = $(speechToTextButton);

			_Speech.init(speechBtn, function(interim, finalResult) {

				if (_Speech.isCommand('save', interim)) {

					dialogSaveButton.click();

				} else if (_Speech.isCommand('saveAndClose', interim)) {

					_Speech.toggleStartStop(speechBtn, function() {
						buttonArea.querySelector('#saveAndClose').click();
					});

				} else if (_Speech.isCommand('close', interim)) {

					_Speech.toggleStartStop(speechBtn, function() {
						buttonArea.querySelector('.closeButton').click();
					});

				} else if (_Speech.isCommand('stop', interim)) {

					_Speech.toggleStartStop(speechBtn, function() {
						//
					});

				} else if (_Speech.isCommand('clearAll', interim)) {

					editor.setValue('');
					editor.focus();
					editor.execCommand('goDocEnd');

				} else if (_Speech.isCommand('deleteLastParagraph', interim)) {

					var text = editor.getValue();
					editor.setValue(text.substring(0, text.lastIndexOf('\n')));
					editor.focus();
					editor.execCommand('goDocEnd');

				} else if (_Speech.isCommand('deleteLastSentence', interim)) {

					var text = editor.getValue();
					editor.setValue(text.substring(0, text.lastIndexOf('.')+1));
					editor.focus();
					editor.execCommand('goDocEnd');

				} else if (_Speech.isCommand('deleteLastWord', interim)) {

					var text = editor.getValue();
					editor.setValue(text.substring(0, text.lastIndexOf(' ')));
					editor.focus();
					editor.execCommand('goDocEnd');

				} else if (_Speech.isCommand('deleteLine', interim)) {

					editor.execCommand('deleteLine');

				} else if (_Speech.isCommand('deleteLineLeft', interim)) {

					editor.execCommand('deleteLineLeft');

				} else if (_Speech.isCommand('deleteLineRight', interim)) {

					editor.execCommand('killLine');

				} else if (_Speech.isCommand('lineUp', interim)) {

					editor.execCommand('goLineUp');

				} else if (_Speech.isCommand('lineDown', interim)) {

					editor.execCommand('goLineDown');

				} else if (_Speech.isCommand('wordLeft', interim)) {

					editor.execCommand('goWordLeft');

				} else if (_Speech.isCommand('wordRight', interim)) {

					editor.execCommand('goWordRight');

				} else if (_Speech.isCommand('left', interim)) {

					editor.execCommand('goCharLeft');

				} else if (_Speech.isCommand('right', interim)) {

					editor.execCommand('goCharRight');

				} else {

					editor.replaceSelection(interim);
				}
			});
		}
	},
	getDefaultEditorOptionsForStorage: () => {
		/**
		 * This is almost identical to IEditorOptions (https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.IStandaloneEditorConstructionOptions.html)
		 * but adapated so we can seamlessly use it in our UI.
		 *
		 * The return value of this function must be transformed to be used with the editor
		 */
		return {
			// section identical to IEditorOptions
			roundedSelection: true,
			glyphMargin: true,
			scrollBeyondLastLine: false,
			readOnly: false,
			renderLineHighlight: 'all',
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

		let dropdown = Structr.createSingleDOMElementFromHTML(`<div class="editor-settings-popup dropdown-menu darker-shadow-dropdown dropdown-menu-large">
				<button class="btn dropdown-select">${_Icons.getSvgIcon('text-settings')}</button>
				<div class="dropdown-menu-container" style="opacity: 0; visibility: hidden; ">
					<div class="font-bold pt-4 pb-2">Global Editor Settings</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Word Wrap</label>
						<select name="wordWrap" class="min-w-48">
							<option>off</option>
							<option>on</option>
						</select>
					</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Wrapping Indent</label>
						<select name="wrappingIndent" class="min-w-48"><option>none</option><option>same</option><option>indent</option><option>deepIndent</option></select>
					</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Indentation</label>
						<select name="indentation" class="min-w-48"><option>tabs</option><option>spaces</option></select>
					</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Tab Size</label>
						<input name="tabSize" type="number" min="1" value="8" style="width: 3rem;">
					</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Auto-Indent</label>
						<select name="autoIndent" class="min-w-48"><option>advanced</option><option>none</option><option>full</option><option>brackets</option><option>keep</option></select>
					</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Theme</label>
						<select name="theme" class="min-w-48">
							<option value="vs">Visual Studio</option>
							<option value="vs-dark">Visual Studio Dark</option>
							<option value="hc-black">High Contrast Dark</option>
						</select>
					</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Line Highligh Rendering</label>
						<select name="renderLineHighlight" class="min-w-48"><option>all</option><option>gutter</option><option>line</option><option>none</option></select>
					</div>
					<div class="editor-setting flex items-center p-1">
						<label class="flex-grow">Show Folding Controls</label>
						<select name="showFoldingControls" class="min-w-48"><option>always</option><option>mouseover</option></select>
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
						<label data-comment="This means that <u>unsaved</u> changes are stored in-memory and such changes are restored even after opening other editors or user interfaces in the admin UI. The are only lost after a page reload or navigation event."><input name="restoreModels" type="checkbox"> Restore text models</label>
					</div>
				</div>
			</div>`);

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

				blinkGreen(settingControlSelect.closest('.editor-setting'));
			});
		}

		for (let settingControlInput of dropdown.querySelectorAll('input[name]')) {

			settingControlInput.addEventListener('change', (e) => {
				let value = (settingControlInput.type === 'checkbox' ? settingControlInput.checked : settingControlInput.value);
				_Editors.updateSavedEditorOption(settingControlInput.name, value);

				blinkGreen(settingControlInput.closest('.editor-setting'));
			});
		}

		element.appendChild(dropdown);

		Structr.activateCommentsInElement(dropdown);
	}
};