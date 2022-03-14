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
let _Dialogs = {

	title: 'Basic',

	getDialogConfigForEntity: (entity) => {

		let registeredDialogs = {
			'DEFAULT_DOM_NODE': { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.defaultDomDialog },
			'A':                { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.aDialog },
			'Button':           { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.buttonDialog },
			'Content':          { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.contentDialog },
			'Div':              { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.divDialog },
			'File':             { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.fileDialog },
			'Image':            { id: 'general', title: 'Advanced',    appendDialogForEntityToContainer: _Dialogs.fileDialog },
			'Folder':           { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.folderDialog },
			'Input':            { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.inputDialog },
			'LDAPGroup':        { id: 'general', title: 'LDAP Config', appendDialogForEntityToContainer: _Dialogs.ldapGroupDialog, condition: () => { return Structr.isModulePresent('ldap-client'); } },
			'Option':           { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.optionDialog },
			'Page':             { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.pageDialog },
			'Template':         { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.contentDialog },
			'User':             { id: 'general', title: 'Basic',       appendDialogForEntityToContainer: _Dialogs.userDialog }
		};

		let dialogConfig = registeredDialogs[entity.type];

		if (!dialogConfig && entity.isDOMNode) {
			dialogConfig = registeredDialogs['DEFAULT_DOM_NODE'];
		}

		return dialogConfig;
	},

	findAndAppendCustomTypeDialog: (entity, mainTabs, contentEl) => {

		let dialogConfig = _Dialogs.getDialogConfigForEntity(entity);

		if (dialogConfig) {

			let callback = dialogConfig.appendDialogForEntityToContainer;

			if (dialogConfig.condition === undefined || (typeof dialogConfig.condition === 'function' && dialogConfig.condition())) {

				// call method with the same callback object for initial callback and show callback
				_Entities.appendPropTab(entity, mainTabs, contentEl, dialogConfig.id, dialogConfig.title, true, callback, undefined, callback);

				return true;
			}
		}

		return false;
	},
	showCustomProperties: async (el, entity) => {

		return new Promise((resolve, reject) => {

			let customContainer = $('div#custom-properties-container', el);

			_Schema.getTypeInfo(entity.type, (typeInfo) => {

				_Entities.listProperties(entity, 'custom', customContainer, typeInfo, (properties) => {

					// make container visible when custom properties exist
					if (Object.keys(properties).length > 0) {
						$('div#custom-properties-parent').removeClass("hidden");
					}

					$('input.dateField', customContainer).each(function(i, input) {
						_Entities.activateDatePicker($(input));
					});

					resolve();
				});
			});
		});
	},
	showShowHideConditionOptions: (el, entity) => {

		let showConditionsContainer = $('.show-hide-conditions-container', el);

		if (showConditionsContainer.length) {

			showConditionsContainer.html(_Dialogs.templates.visibilityPartial({ entity: entity }));

			_Dialogs.populateInputFields(showConditionsContainer, entity);
			_Dialogs.registerSimpleInputChangeHandlers(showConditionsContainer, entity);

			let showConditionsInput  = $('input#show-conditions', showConditionsContainer);
			let showConditionsSelect = $('select#show-conditions-templates', showConditionsContainer);
			let hideConditionsInput  = $('input#hide-conditions', showConditionsContainer);
			let hideConditionsSelect = $('select#hide-conditions-templates', showConditionsContainer);

			showConditionsSelect.on('change', () => {
				showConditionsInput.val(showConditionsSelect.val());
				showConditionsInput[0].dispatchEvent(new Event('change'));
			});
			hideConditionsSelect.on('change', () => {
				hideConditionsInput.val(hideConditionsSelect.val());
				hideConditionsInput[0].dispatchEvent(new Event('change'));
			});
		}
	},
	showChildContentEditor:(el, entity) => {

		if (entity && entity.children && entity.children.length === 1 && entity.children[0].type === 'Content') {

			let textContentContainer = $('.show-text-content-container', el);
			if (textContentContainer.length) {

				textContentContainer.html(_Dialogs.templates.contentPartial());

				let child = entity.children[0];

				let populateDialog = (contentEl) => {
					_Dialogs.populateInputFields(textContentContainer, contentEl);
					_Dialogs.registerSimpleInputChangeHandlers(textContentContainer, contentEl, true);
				};

				if (!child.content) {
					Command.get(child.id, 'id,type,content', (loadedChild) => { populateDialog(loadedChild); });
				} else {
					populateDialog(child);
				}
			}
		}
	},
	showRepeaterOptions: (el, entity) => {

		let repeaterConfigContainer = $('.repeater-config-container', el);

		if (repeaterConfigContainer.length) {

			repeaterConfigContainer.html(_Dialogs.templates.repeaterPartial({ entity: entity }));

			_Dialogs.populateInputFields(repeaterConfigContainer, entity);
			_Dialogs.registerSimpleInputChangeHandlers(repeaterConfigContainer, entity);
		}
	},
	showRenderingOptions: (el, entity) => {

		// Disable render options on certain elements
		if (['html', 'body', 'head', 'title', 'style', 'meta', 'link', 'script', 'base'].includes(entity.tag)) {
			return;
		}

		let renderingOptionsContainer = $('.rendering-options-container', el);

		if (renderingOptionsContainer.length) {

			renderingOptionsContainer.html(_Dialogs.templates.renderingOptions({ entity: entity }));

			_Dialogs.populateInputFields(renderingOptionsContainer, entity);
			_Dialogs.registerSimpleInputChangeHandlers(renderingOptionsContainer, entity);

			let renderingModeSelect           = $('select#rendering-mode-select', el);
			renderingModeSelect.select2();

			Command.getProperty(entity.id, 'data-structr-rendering-mode', (result) => {
				renderingModeSelect.val(result);
				renderingModeSelect.trigger('change');
			});

			renderingModeSelect.on('change', () => {
				let renderingMode = renderingModeSelect.val() === '' ? null : renderingModeSelect.val();
				_Entities.setPropertyWithFeedback(entity, 'data-structr-rendering-mode', renderingMode, renderingModeSelect, null);
			});
		}
	},
	getValueFromFormElement: (el) => {

		if (el.tagName === 'SELECT' && el.multiple === true) {
			return [].map.call(el.selectedOptions, (o) => o.value);
		} else if (el.tagName === 'INPUT' && el.type === 'date') {
			return new Date(el.value);
		} else if (el.tagName === 'INPUT' && el.type === 'checkbox') {
			return el.checked;
		} else if (el.tagName === 'INPUT' && el.type === 'radio') {
			if (el.checked === true) {
				return el.value;
			} else {
				return null;
			}
		}

		return el.value;
	},
	registerSimpleInputChangeHandlers: (el, entity, emptyStringInsteadOfNull) => {

		for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

			inputEl.addEventListener('change', () => {

				let key      = inputEl.name;
				let oldVal   = entity[key];
				let newVal   = _Dialogs.getValueFromFormElement(inputEl);
				let isChange = (oldVal !== newVal) && !((oldVal === null || oldVal === undefined) && newVal === '');

				if (isChange) {

					let blinkElement = (inputEl.type === 'checkbox') ? $(inputEl).parent() : null;

					_Entities.setPropertyWithFeedback(entity, key, newVal || (emptyStringInsteadOfNull ? '' : null), $(inputEl), blinkElement);
				}
			});
		}
	},
	populateInputFields: (el, entity) => {

		for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

			let val = entity[inputEl.name];
			if (val) {
				if (inputEl.type === 'checkbox') {
					inputEl.checked = val;
				} else {
					inputEl.value = val;
				}
			}
		}
	},
	focusInput: (el, selector) => {

		if (selector) {
			$(selector, el).focus().select();
		} else {
			$('input:first', el).focus().select();
		}
	},
	setNull: (id, key, input) => {

		Command.setProperty(id, key, null, false, () => {
			input.val(null);
			blinkGreen(input);
			Structr.showAndHideInfoBoxMessage('Property "' + key + '" has been set to null.', 'success', 2000, 1000);
		});
	},

	// ----- custom dialogs -----
	ldapGroupDialog: (el, entity) => {

		el.html(_Dialogs.templates.ldapGroup({ group: entity }));

		let dnInput     = $('input#ldap-group-dn');
		let pathInput   = $('input#ldap-group-path');
		let filterInput = $('input#ldap-group-filter');
		let scopeInput  = $('input#ldap-group-scope');

		_Dialogs.registerSimpleInputChangeHandlers(el, entity);

		// dialog logic here..
		$('i#clear-ldap-group-dn').on('click', () => { _Dialogs.setNull(entity.id, 'distinguishedName', dnInput); });
		$('i#clear-ldap-group-path').on('click', () => { _Dialogs.setNull(entity.id, 'path', pathInput); });
		$('i#clear-ldap-group-filter').on('click', () => { _Dialogs.setNull(entity.id, 'filter', filterInput); });
		$('i#clear-ldap-group-scope').on('click', () => { _Dialogs.setNull(entity.id, 'scope', scopeInput); });

		$('button#ldap-sync-button').on('click', async () => {

			let response = await fetch(Structr.rootUrl + entity.type + '/' + entity.id + '/update', {
				method: 'POST'
			});

			if (response.ok) {
				Structr.showAndHideInfoBoxMessage('Updated LDAP group successfully', 'success', 2000, 200);
			} else {
				Structr.showAndHideInfoBoxMessage('LDAP group could not be updated', 'warning', 5000, 200);
			}
		});

		_Dialogs.focusInput(el);
	},
	fileDialog: (el, entity) => {

		el.html(_Dialogs.templates.fileOptions({ file: entity, title: _Dialogs.title }));

		if (Structr.isModulePresent('text-search')) {

			$('#content-extraction').removeClass('hidden');

			$('button#extract-structure-button').on('click', async () => {

				Structr.showAndHideInfoBoxMessage('Extracting structure..', 'info', 2000, 200);

				let response = await fetch(Structr.rootUrl + entity.type + '/' + entity.id + '/extractStructure', {
					method: 'POST'
				});

				if (response.ok) {
					Structr.showAndHideInfoBoxMessage('Structure extracted, see Contents area.', 'success', 2000, 200);
				}
			});
		}

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);
		Structr.activateCommentsInElement(el);

		_Dialogs.focusInput(el);
	},
	folderDialog: (el, entity) => {

		el.html(_Dialogs.templates.folderOptions({ file: entity, title: _Dialogs.title }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);
		Structr.activateCommentsInElement(el);
	},
	aDialog: (el, entity) => {

		Command.get(entity.id, null, (aHtmlProperties) => {

			el.html(_Dialogs.templates.aOptions({ entity: entity, a: aHtmlProperties, title: _Dialogs.title }));

			_Dialogs.populateInputFields(el, aHtmlProperties);
			_Dialogs.registerSimpleInputChangeHandlers(el, aHtmlProperties);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showRepeaterOptions(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

			// child content
			_Dialogs.showChildContentEditor(el, entity);

		}, '_html_');
	},
	buttonDialog: (el, entity) => {

		Command.get(entity.id, null, (buttonHtmlProperties) => {

			el.html(_Dialogs.templates.buttonOptions({ entity: entity, button: buttonHtmlProperties, title: _Dialogs.title }));

			_Dialogs.populateInputFields(el, buttonHtmlProperties);
			_Dialogs.registerSimpleInputChangeHandlers(el, buttonHtmlProperties);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showRepeaterOptions(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

			// child content
			_Dialogs.showChildContentEditor(el, entity);

		}, '_html_');
	},
	inputDialog: (el, entity) => {

		Command.get(entity.id, null, (inputHtmlProperties) => {

			el.html(_Dialogs.templates.inputOptions({ entity: entity, input: inputHtmlProperties, title: _Dialogs.title }));

			_Dialogs.populateInputFields(el, inputHtmlProperties);
			_Dialogs.registerSimpleInputChangeHandlers(el, inputHtmlProperties);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

		}, '_html_');
	},
	divDialog: (el, entity) => {

		Command.get(entity.id, null, (divHtmlProperties) => {

			el.html(_Dialogs.templates.divOptions({ entity: entity, title: _Dialogs.title }));

			_Dialogs.populateInputFields(el, divHtmlProperties);
			_Dialogs.registerSimpleInputChangeHandlers(el, divHtmlProperties);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showRepeaterOptions(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

		}, '_html_');
	},
	userDialog: (el, entity) => {

		el.html(_Dialogs.templates.userOptions({ entity: entity, user: entity, title: _Dialogs.title }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);

		$('button#set-password-button').on('click', (e) => {
			let input = $('input#password-input');
			_Entities.setPropertyWithFeedback(entity, 'password', input.val(), input);
		});

		_Dialogs.focusInput(el);

		_Dialogs.showCustomProperties(el, entity);
	},
	pageDialog: async (el, entity) => {

		el.html(_Dialogs.templates.pageOptions({ entity: entity, page: entity, title: _Dialogs.title }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);

		_Dialogs.focusInput(el);

		await _Dialogs.showCustomProperties(el, entity);

		Structr.activateCommentsInElement(el);
	},
	defaultDomDialog: (el, entity) => {

		Command.get(entity.id, null, (htmlProperties) => {

			el.html(_Dialogs.templates.defaultDOMOptions({ entity: entity, title: _Dialogs.title }));

			_Dialogs.populateInputFields(el, htmlProperties);
			_Dialogs.registerSimpleInputChangeHandlers(el, htmlProperties);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showRepeaterOptions(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

			// child content (optional)
			_Dialogs.showChildContentEditor(el, entity);

		}, '_html_');
	},
	contentDialog: (el, entity) => {

		el.html(_Dialogs.templates.contentOptions({ entity: entity, title: _Dialogs.title }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);

		_Dialogs.focusInput(el);

		_Dialogs.showCustomProperties(el, entity);
		_Dialogs.showRepeaterOptions(el, entity);
		_Dialogs.showShowHideConditionOptions(el, entity);

	},
	optionDialog: (el, entity) => {

		Command.get(entity.id, null, (divHtmlProperties) => {

			el.html(_Dialogs.templates.optionOptions({ entity: entity, title: _Dialogs.title }));

			let data = Object.assign({}, divHtmlProperties, entity);

			_Dialogs.populateInputFields(el, data);
			_Dialogs.registerSimpleInputChangeHandlers(el, data);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showRepeaterOptions(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

			_Dialogs.showChildContentEditor(el, entity);

		}, '_html_');
	},

	templates: {
		aOptions: config => `
			<div id="div-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label>CSS Class</label>
						<input type="text" id="class-input" name="_html_class" />
					</div>
					<div class="option-tile">
						<label>HTML ID</label>
						<input type="text" id="id-input" name="_html_id" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile double">
						<label>HREF attribute</label>
						<input type="text" id="href-input" name="_html_href" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile double">
						<label>Style</label>
						<input type="text" id="style-input" name="_html_style" />
					</div>
				</div>
			
				<div class="row repeater-config-container"></div>
			
				<div class="row show-hide-conditions-container"></div>
			
				<div class="row show-text-content-container"></div>
			
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
				<div class="row rendering-options-container"></div>
			</div>
		`,
		buttonOptions: config => `
			<div id="div-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="class-input">CSS Class</label>
						<input type="text" id="class-input" name="_html_class" />
					</div>
					<div class="option-tile">
						<label for="id-input">HTML ID</label>
						<input type="text" id="id-input" name="_html_id" />
					</div>
				</div>
			
				<div class="row">
					<div class="option-tile">
						<label for="title-input">Title</label>
						<input type="text" id="title-input" name="_html_title" />
					</div>
					<div class="option-tile">
						<label for="type-input">Type</label>
						<input type="text" id="type-input" name="_html_type" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile double">
						<label for="style-input">Style</label>
						<input type="text" id="style-input" name="_html_style" />
					</div>
				</div>
			
				<div class="row repeater-config-container"></div>
			
				<div class="row show-hide-conditions-container"></div>
			
				<div class="row show-text-content-container"></div>
			
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
			</div>
		`,
		contentOptions: config => `
			<div id="default-dom-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile double">
						<label for="name-input">Name</label>
						<input type="text" id="name-input" autocomplete="off" name="name" />
					</div>
				</div>
			
				<div class="row repeater-config-container"></div>
			
				<div class="row show-hide-conditions-container"></div>
			
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
			</div>
		`,
		contentPartial: config => `
			<div class="option-tile double">
				<label for="content-input">Text Content</label>
				<textarea id="content-input" name="content" rows="5"></textarea>
			</div>
		`,
		defaultDOMOptions: config => `
			<div id="default-dom-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="class-input">CSS Class</label>
						<input type="text" id="class-input" name="_html_class"/>
					</div>
					<div class="option-tile">
						<label for="id-input">HTML ID</label>
						<input type="text" id="id-input" name="_html_id" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile double">
						<label for="style-input">Style</label>
						<input type="text" id="style-input" name="_html_style" />
					</div>
				</div>
			
				<div class="row repeater-config-container"></div>
				<div class="row show-hide-conditions-container"></div>
				<div class="row show-text-content-container"></div>
				<div class="row rendering-options-container"></div>
			
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
			</div>
		`,
		divOptions: config => `
			<div id="div-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="class-input">CSS Class</label>
						<input type="text" id="class-input" name="_html_class"/>
					</div>
					<div class="option-tile">
						<label for="id-input">HTML ID</label>
						<input type="text" id="id-input" name="_html_id" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile double">
						<label for="style-input">Style</label>
						<input type="text" id="style-input" name="_html_style" />
					</div>
				</div>
			
				<div class="row repeater-config-container"></div>
			
				<div class="row show-hide-conditions-container"></div>
			
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
				<div class="row rendering-options-container"></div>
			</div>
		`,
		fileOptions: config => `
			<div id="file-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="name-input">Name</label>
						<input type="text" id="name-input" autocomplete="off" name="name" />
					</div>
					<div class="option-tile">
						<label for="content-type-input">Content Type</label>
						<input type="text" id="content-type-input" autocomplete="off" name="contentType" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile">
						<label for="cache-for-seconds-input" class="block">Cache for n seconds</label>
						<input type="text" id="cache-for-seconds-input" value="" name="cacheForSeconds" />
					</div>
					<div class="option-tile">
			
						<label class="block">Options</label>
			
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="isTemplate" id="isTemplate" />
							<label for="isTemplate">Is template (dynamic file)</label>
						</div>
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="dontCache" id="dontCache" />
							<label for="dontCache">Caching disabled</label>
						</div>
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="includeInFrontendExport" id="includeInFrontendExport" />
							<label for="includeInFrontendExport" data-comment-config="{insertAfter:true}" data-comment="If checked this file/folder is exported in the deployment process. If a parent folder has this flag enabled, it will automatically be exported and the flag does not need to be set.">Include in frontend export</label>
						</div>
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="useAsJavascriptLibrary" id="useAsJavascriptLibrary" />
							<label for="useAsJavascriptLibrary" data-comment-config="{insertAfter:true}" data-comment="If checked this file can be included via <code>$.includeJs(fileName)</code> in any other server-side JavaScript context.<br><br>File must have content-type <code>text/javascript</code> or <code>application/javascript</code>">Use As Javascript Library</label>
						</div>
					</div>
				</div>
				<div id="content-extraction" class="row hidden">
					<h3>Content Extraction</h3>
					<div class="row">
						<p>Extract text content from this document or image and store it in a StructuredDocument node with StructuredTextNode children.</p>
						<button type="button" class="action" id="extract-structure-button">Extract document content</button>
					</div>
				</div>
			</div>
		`,
		folderOptions: config => `
			<div id="file-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="name-input">Name</label>
						<input type="text" id="name-input" autocomplete="off" name="name" />
					</div>
					<div class="option-tile">
						<label class="block">Options</label>
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="includeInFrontendExport" id="includeInFrontendExport" />
							<label for="includeInFrontendExport" data-comment-config="{insertAfter:true}" data-comment="If checked this file/folder is exported in the deployment process. If a parent folder has this flag enabled, it will automatically be exported and the flag does not need to be set.">Include in frontend export</label>
						</div>
					</div>
				</div>
			</div>
		`,
		inputOptions: config => `
			<div id="div-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="class-input">CSS Class</label>
						<input type="text" id="class-input" name="_html_class" />
					</div>
					<div class="option-tile">
						<label for="id-input">HTML ID</label>
						<input type="text" id="id-input" name="_html_id" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile">
						<label for="type-input">Type</label>
						<input type="text" id="type-input" name="_html_type" />
					</div>
					<div class="option-tile">
						<label for="placeholder-input">Placeholder</label>
						<input type="text" id="placeholder-input" name="_html_placeholder" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile double">
						<label for="style-input">Style</label>
						<input type="text" id="style-input" name="_html_style" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile">
						<label for="title-input">Title</label>
						<input type="text" id="title-input" name="_html_title" />
					</div>
				</div>
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
			
				<div class="row show-hide-conditions-container"></div>
			</div>
		`,
		ldapGroup: config => `
			<div id="ldap-group-config">
				<h3>Synchronize this group using distinguished name (prioritized if set)</h3>
				<div class="row">
					<input type="text" size="80" id="ldap-group-dn" placeholder="Distinguished Name" name="distinguishedName" />
					<i class="nullIcon sprite sprite-cross_small_grey" title="Clear value" id="clear-ldap-group-dn"></i>
				</div>
				<h3>Synchronize this group using path, filter and scope (if distinguished name not set above)</h3>
				<div class="row">
					<input type="text" size="80" id="ldap-group-path" placeholder="Path" name="path" />
					<i class="nullIcon sprite sprite-cross_small_grey" title="Clear value" id="clear-ldap-group-path"></i>
				</div>
				<div class="row">
					<input type="text" size="80" id="ldap-group-filter" placeholder="Filter" name="filter" />
					<i class="nullIcon sprite sprite-cross_small_grey" title="Clear value" id="clear-ldap-group-filter"></i>
				</div>
				<div class="row">
					<input type="text" size="80" id="ldap-group-scope" placeholder="Scope" name="scope" />
					<i class="nullIcon sprite sprite-cross_small_grey" title="Clear value" id="clear-ldap-group-scope"></i>
				</div>
				<div class="row">
					<button type="button" class="action" id="ldap-sync-button">Synchronize now</button>
				</div>
				<div class="row">
					<a href="/structr/config" target="_blank">Open Structr configuration</a>
				</div>
			</div>
		`,
		optionOptions: config => `
			<div id="div-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="class-input">CSS Class</label>
						<input type="text" id="class-input" name="_html_class"/>
					</div>
					<div class="option-tile">
						<label for="id-input">HTML ID</label>
						<input type="text" id="id-input" name="_html_id" />
					</div>
				</div>
				<div class="row">
					<div class="option-tile double">
						<label for="style-input">Style</label>
						<input type="text" id="style-input" name="_html_style" />
					</div>
				</div>
			
				<div class="row repeater-config-container"></div>
			
				<div class="row">
			
					<div class="option-tile">
						<label for="selected-input">Selected</label>
						<input type="text" id="selected-input" name="_html_selected" />
					</div>
					<div class="option-tile">
						<label for="selected-values-input">Selected Values Expression</label>
						<input type="text" id="selected-values-input" name="selectedValues" />
					</div>
				</div>
			
				<div class="row">
					<div class="option-tile">
						<label for="value-input">Value</label>
						<input type="text" id="value-input" name="_html_value" />
					</div>
				</div>
			
				<div class="row show-hide-conditions-container"></div>
			
				<div class="row show-text-content-container"></div>
			
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
			
				<div class="row rendering-options-container"></div>
			</div>
		`,
		pageOptions: config => `
			<div id="div-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="name-input">Name</label>
						<input type="text" id="name-input" name="name">
					</div>
					<div class="option-tile">
						<label for="content-type-input">Content Type</label>
						<input type="text" id="content-type-input" name="contentType">
					</div>
				</div>
			
				<div class="row">
					<div class="option-tile">
						<label for="category-input">Category</label>
						<input type="text" id="category-input" name="category">
					</div>
					<div class="option-tile">
						<label for="show-on-error-codes-input">Show on Error Codes</label>
						<input type="text" id="show-on-error-codes-input" name="showOnErrorCodes">
					</div>
				</div>
			
				<div class="row">
					<div class="option-tile">
						<label for="position-input">Position </label>
						<input type="text" id="position-input" name="position" />
					</div>
					<div class="option-tile">
						<label class="block">Options</label>
						<div class="mb-2 flex items-center">
							<label for="dont-cache-checkbox" data-comment="Especially important for dynamic pages which are visible to public users.">
								<input type="checkbox" name="dontCache" id="dont-cache-checkbox"> Caching disabled
							</label>
						</div>
						<div class="mb-2 flex items-center">
							<label for="page-creates-raw-data-checkbox">
								<input type="checkbox" name="pageCreatesRawData" id="page-creates-raw-data-checkbox"> Use binary encoding for output
							</label>
						</div>
					</div>
				</div>
			
				<div id="custom-properties-parent" class="row hidden">
					<h3>Custom Attributes</h3>
					 <div id="custom-properties-container"></div>
				</div>
			</div>
		`,
		renderingOptions: config => `
			<div class="row options-rendering"><h3>Rendering Options</h3></div>
			<div class="row">
				<div class="option-tile">
					<label for="rendering-mode-select" data-comment="Select rendering mode for this element to activate lazy loading.">Select rendering mode for this element to activate lazy loading.</label>
					<select class="select2" id="rendering-mode-select" name="data-structr-rendering-mode">
						<option value="">Eager (default)</option>
						<option value="load">When page has finished loading</option>
						<option value="delayed">With a delay after page has finished loading</option>
						<option value="visible">When element becomes visible</option>
						<option value="periodic">With periodic updates</option>
					</select>
				</div>
				<div class="option-tile">
					<label for="rendering-delay-or-interval">Delay or interval in milliseconds</label>
					<input type="number" id="rendering-delay-or-interval" name="data-structr-delay-or-interval">
				</div>
			</div>
		`,
		repeaterPartial: config => `
			<div class="option-tile">
				<label for="function-query-input">Function Query</label>
				<input type="text" id="function-query-input" name="functionQuery" />
			</div>
			<div class="option-tile">
				<label for="data-key-input">Data Key</label>
				<input type="text" id="data-key-input" name="dataKey" />
			</div>
		`,
		userOptions: config => `
			<div id="div-options" class="quick-access-options">
				<div class="row">
					<div class="option-tile">
						<label for="name-input">Name</label>
						<input type="text" id="name-input" autocomplete="off" name="name">
					</div>
					<div class="option-tile">
						<label for="e-mail-input">eMail</label>
						<input type="text" id="e-mail-input" autocomplete="off" name="eMail">
					</div>
				</div>
			
				<div class="row">
			
					<div class="option-tile">
						<label for="password-input">Password</label>
						<input type="password" id="password-input" autocomplete="new-password" value="****** HIDDEN ******">
						<button class="action" type="button" id="set-password-button">Set Password</button>
					</div>
			
					<div class="option-tile">
			
						<label class="block">Options</label>
			
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="isAdmin" id="isAdmin">
							<label for="isAdmin">Is Admin User</label>
						</div>
			
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="skipSecurityRelationships" id="skipSecurityRelationships">
							<label for="skipSecurityRelationships">Skip Security Relationships</label>
						</div>
			
						<div class="mb-2 flex items-center">
							<input type="checkbox" name="isTwoFactorUser" id="isTwoFactorUser">
							<label for="isTwoFactorUser">Enable Two-Factor Authentication for this User</label>
						</div>
					</div>
				</div>
			
				<div id="custom-properties-parent" class="hidden">
					<h3>Custom Attributes</h3>
					<div class="row">
						 <div id="custom-properties-container"></div>
					</div>
				</div>
			</div>
		`,
		visibilityPartial: config => `
			<div class="option-tile">
				<label for="show-conditions">Show Conditions</label>
				<input id="show-conditions" type="text" name="showConditions" />
				<select id="show-conditions-templates">
					<option value="" disabled selected>Select...</option>
					<option value="">(none)</option>
					<option>true</option>
					<option>false</option>
					<option>me.isAdmin</option>
					<option>empty(current)</option>
					<option>not(empty(current))</option>
				</select>
			</div>
			<div class="option-tile">
				<label for="hide-conditions">Hide Conditions</label>
				<input id="hide-conditions" type="text" name="hideConditions" />
				<select id="hide-conditions-templates">
					<option value="" disabled selected>Select...</option>
					<option value="">(none)</option>
					<option>true</option>
					<option>false</option>
					<option>me.isAdmin</option>
					<option>empty(current)</option>
					<option>not(empty(current))</option>
				</select>
			</div>
		`
	}
};