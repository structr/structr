/*
 * Copyright (C) 2010-2023 Structr GmbH
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

		if (!dialogConfig && entity.isUser) {
			dialogConfig = registeredDialogs['User'];
		}

		return dialogConfig;
	},
	findAndAppendCustomTypeDialog: (entity, mainTabs, contentEl) => {

		let dialogConfig = _Dialogs.getDialogConfigForEntity(entity);

		if (dialogConfig) {

			if (dialogConfig.condition === undefined || (typeof dialogConfig.condition === 'function' && dialogConfig.condition())) {

				let wrapperFn = (tabCntnt) => {
					dialogConfig.appendDialogForEntityToContainer($(tabCntnt), entity);
				}

				// call method with the same callback object for initial callback and show callback
				let tabContent = _Entities.appendPropTab(entity, mainTabs, contentEl, dialogConfig.id, dialogConfig.title, true, wrapperFn, true);

				wrapperFn(tabContent);

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
	showShowHideConditionOptions: (el) => {

		let showConditionsInput  = $('input#show-conditions', el);
		let showConditionsSelect = $('select#show-conditions-templates', el);
		let hideConditionsInput  = $('input#hide-conditions', el);
		let hideConditionsSelect = $('select#hide-conditions-templates', el);

		showConditionsSelect.on('change', () => {
			showConditionsInput.val(showConditionsSelect.val());
			showConditionsInput[0].dispatchEvent(new Event('change'));
		});
		hideConditionsSelect.on('change', () => {
			hideConditionsInput.val(hideConditionsSelect.val());
			hideConditionsInput[0].dispatchEvent(new Event('change'));
		});
	},
	showChildContentEditor:(el, entity) => {

		let textContentContainer = el.find('#child-content-editor')[0];

		if (entity && entity.children && entity.children.length === 1 && entity.children[0].type === 'Content') {

			if (textContentContainer) {

				textContentContainer.classList.remove('hidden');

				let child    = entity.children[0];
				let modelObj = StructrModel.obj(child.id) ?? child;

				let populateDialog = (child) => {
					_Dialogs.populateInputFields($(textContentContainer), child);
					_Dialogs.registerDeferredSimpleInputChangeHandlers($(textContentContainer), child, true);
				};

				if (!modelObj.content) {
					Command.get(modelObj.id, 'id,type,content', populateDialog);
				} else {
					populateDialog(modelObj);
				}
			}
		}
	},
	showRenderingOptions: (el, entity) => {

		// Disable render options on certain elements
		if (['html', 'body', 'head', 'title', 'style', 'meta', 'link', 'script', 'base'].includes(entity.tag)) {
			return;
		}

		let renderingOptionsContainer = $('#rendering-options-container', el);

		if (renderingOptionsContainer.length) {

			renderingOptionsContainer.removeClass('hidden');

			let renderingModeSelect           = $('select#rendering-mode-select', renderingOptionsContainer);
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
	registerDeferredSimpleInputChangeHandlers: (el, entity, emptyStringInsteadOfNull) => {

		_Dialogs.registerSimpleInputChangeHandlers(el, entity, emptyStringInsteadOfNull, true);
	},
	registerSimpleInputChangeHandlers: (el, entity, emptyStringInsteadOfNull, isDeferredChangeHandler = false) => {

		for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

			let shouldDeferChangeHandler = inputEl.dataset['deferChangeHandler'];

			if (shouldDeferChangeHandler !== 'true' || (shouldDeferChangeHandler === 'true' && isDeferredChangeHandler === true) ) {

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
		}
	},
	populateInputFields: (el, entity) => {

		for (let inputEl of el[0].querySelectorAll('textarea[name], input[name]')) {

			let val = entity[inputEl.name];
			if (val != undefined && val != null) {
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
			_Helpers.blinkGreen(input);
			Structr.dialogSystem.showAndHideInfoBoxMessage('Property "' + key + '" has been set to null.', 'success', 2000, 1000);
		});
	},
	addHtmlPropertiesToEntity: (entity, callback) => {

		Command.get(entity.id, null, (htmlProperties) => {

			StructrModel.update(Object.assign(htmlProperties, entity));

			callback(entity);
		}, '_html_');
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
		$('.clear-ldap-group-dn', el).on('click', () => { _Dialogs.setNull(entity.id, 'distinguishedName', dnInput); });
		$('.clear-ldap-group-path', el).on('click', () => { _Dialogs.setNull(entity.id, 'path', pathInput); });
		$('.clear-ldap-group-filter', el).on('click', () => { _Dialogs.setNull(entity.id, 'filter', filterInput); });
		$('.clear-ldap-group-scope', el).on('click', () => { _Dialogs.setNull(entity.id, 'scope', scopeInput); });

		$('button#ldap-sync-button').on('click', async () => {

			let response = await fetch(Structr.rootUrl + entity.type + '/' + entity.id + '/update', {
				method: 'POST'
			});

			if (response.ok) {
				Structr.dialogSystem.showAndHideInfoBoxMessage('Updated LDAP group successfully', 'success', 2000, 200);
			} else {
				Structr.dialogSystem.showAndHideInfoBoxMessage('LDAP group could not be updated', 'warning', 5000, 200);
			}
		});

		_Dialogs.focusInput(el);
	},
	fileDialog: (el, entity) => {

		el.html(_Dialogs.templates.fileOptions({ file: entity }));

		if (Structr.isModulePresent('text-search')) {

			$('#content-extraction').removeClass('hidden');

			$('button#extract-structure-button').on('click', async () => {

				Structr.dialogSystem.showAndHideInfoBoxMessage('Extracting structure..', 'info', 2000, 200);

				let response = await fetch(Structr.rootUrl + entity.type + '/' + entity.id + '/extractStructure', {
					method: 'POST'
				});

				if (response.ok) {
					Structr.dialogSystem.showAndHideInfoBoxMessage('Structure extracted, see Contents area.', 'success', 2000, 200);
				}
			});
		}

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);
		_Helpers.activateCommentsInElement(el[0]);

		_Dialogs.focusInput(el);
	},
	folderDialog: (el, entity) => {

		el.html(_Dialogs.templates.folderOptions({ file: entity }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);
		_Helpers.activateCommentsInElement(el[0]);
	},
	aDialog: (el, entity) => {

		_Dialogs.addHtmlPropertiesToEntity(entity,(enrichedEntity) => {

			el.html(_Dialogs.templates.aOptions({ entity: enrichedEntity }));

			_Dialogs.populateInputFields(el, enrichedEntity);
			_Dialogs.registerSimpleInputChangeHandlers(el, enrichedEntity);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);

			_Dialogs.showShowHideConditionOptions(el, entity);

			_Dialogs.showRenderingOptions(el, entity);

			_Dialogs.showChildContentEditor(el, entity);

		});
	},
	buttonDialog: (el, entity) => {

		_Dialogs.addHtmlPropertiesToEntity(entity,(enrichedEntity) => {

			el.html(_Dialogs.templates.buttonOptions({ entity: enrichedEntity }));

			_Dialogs.populateInputFields(el, enrichedEntity);
			_Dialogs.registerSimpleInputChangeHandlers(el, enrichedEntity);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

			_Dialogs.showChildContentEditor(el, entity);

		});
	},
	inputDialog: (el, entity) => {

		_Dialogs.addHtmlPropertiesToEntity(entity,(enrichedEntity) => {

			el.html(_Dialogs.templates.inputOptions({ entity: enrichedEntity }));

			_Dialogs.populateInputFields(el, enrichedEntity);
			_Dialogs.registerSimpleInputChangeHandlers(el, enrichedEntity);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

		});
	},
	divDialog: (el, entity) => {

		_Dialogs.addHtmlPropertiesToEntity(entity,(enrichedEntity) => {

			el.html(_Dialogs.templates.divOptions({ entity: enrichedEntity }));

			_Dialogs.populateInputFields(el, enrichedEntity);
			_Dialogs.registerSimpleInputChangeHandlers(el, enrichedEntity);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

		});
	},
	userDialog: (el, entity) => {

		el.html(_Dialogs.templates.userOptions({ entity: entity, user: entity }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);
		_Helpers.activateCommentsInElement(el[0]);

		$('button#set-password-button').on('click', (e) => {
			let input = $('input#password-input');
			_Entities.setPropertyWithFeedback(entity, 'password', input.val(), input);
		});

		_Dialogs.focusInput(el);

		_Dialogs.showCustomProperties(el, entity);
	},
	pageDialog: async (el, entity) => {

		el.html(_Dialogs.templates.pageOptions({ entity: entity, page: entity }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);

		_Pages.previews.configurePreview(entity, el[0]);

		_Dialogs.focusInput(el);

		await _Dialogs.showCustomProperties(el, entity);

		_Helpers.activateCommentsInElement(el[0]);
	},
	defaultDomDialog: (el, entity) => {

		_Dialogs.addHtmlPropertiesToEntity(entity,(enrichedEntity) => {

			el.html(_Dialogs.templates.defaultDOMOptions({ entity: enrichedEntity }));

			_Dialogs.populateInputFields(el, enrichedEntity);
			_Dialogs.registerSimpleInputChangeHandlers(el, enrichedEntity);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

			_Dialogs.showChildContentEditor(el, entity);
		});
	},
	contentDialog: (el, entity) => {

		el.html(_Dialogs.templates.contentOptions({ entity: entity }));

		_Dialogs.populateInputFields(el, entity);
		_Dialogs.registerSimpleInputChangeHandlers(el, entity);

		_Dialogs.focusInput(el);

		_Dialogs.showCustomProperties(el, entity);
		_Dialogs.showShowHideConditionOptions(el, entity);

	},
	optionDialog: (el, entity) => {

		_Dialogs.addHtmlPropertiesToEntity(entity,(enrichedEntity) => {

			el.html(_Dialogs.templates.optionOptions({ entity: enrichedEntity }));

			_Dialogs.populateInputFields(el, enrichedEntity);
			_Dialogs.registerSimpleInputChangeHandlers(el, enrichedEntity);

			_Dialogs.focusInput(el);

			_Dialogs.showCustomProperties(el, entity);
			_Dialogs.showShowHideConditionOptions(el, entity);
			_Dialogs.showRenderingOptions(el, entity);

			_Dialogs.showChildContentEditor(el, entity);
		});
	},

	templates: {
		nameTile: config => `
			<div class="option-tile ${(config.doubleWide === true ? 'col-span-2' : '')}">
				<label class="block mb-2" for="name-input">Name</label>
				<input type="text" id="name-input" autocomplete="off" name="name">
			</div>
		`,
		htmlClassTile: config => `
			<div class="option-tile">
				<label class="block mb-2" for="class-input">CSS Class</label>
				<input type="text" id="class-input" name="_html_class">
			</div>
		`,
		htmlIdTile: config => `
			<div class="option-tile">
				<label class="block mb-2" for="id-input">HTML ID</label>
				<input type="text" id="id-input" name="_html_id">
			</div>
		`,
		htmlStyleTile: config => `
			<div class="option-tile col-span-2">
				<label class="block mb-2" for="style-input">Style</label>
				<input type="text" id="style-input" name="_html_style">
			</div>
		`,
		htmlTitleTile: config => `
			<div class="option-tile">
				<label class="block mb-2" for="title-input">Title</label>
				<input type="text" id="title-input" name="_html_title">
			</div>
		`,
		htmlTypeTile: config => `
			<div class="option-tile">
				<label class="block mb-2" for="type-input">Type</label>
				<input type="text" id="type-input" name="_html_type">
			</div>
		`,
		aOptions: config => `
			<div id="div-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(Object.assign({ doubleWide: true }, config))}

					${_Dialogs.templates.htmlClassTile(config)}

					${_Dialogs.templates.htmlIdTile(config)}

					<div class="option-tile col-span-2">
						<label class="block mb-2" for="href-input">HREF attribute</label>
						<input type="text" id="href-input" name="_html_href">
					</div>

					${_Dialogs.templates.htmlStyleTile(config)}

					${_Dialogs.templates.repeaterPartial(config)}

					${_Dialogs.templates.visibilityPartial(config)}

					${_Dialogs.templates.contentPartial(config)}

				</div>

				${_Dialogs.templates.renderingOptions(config)}

				${_Dialogs.templates.customPropertiesPartial(config)}
			</div>
		`,
		buttonOptions: config => `
			<div id="div-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(Object.assign({ doubleWide: true }, config))}

					${_Dialogs.templates.htmlClassTile(config)}

					${_Dialogs.templates.htmlIdTile(config)}

					${_Dialogs.templates.htmlTitleTile(config)}

					${_Dialogs.templates.htmlTypeTile(config)}

					${_Dialogs.templates.htmlStyleTile(config)}

					${_Dialogs.templates.repeaterPartial(config)}

					${_Dialogs.templates.visibilityPartial(config)}

					${_Dialogs.templates.contentPartial()}
				</div>

				${_Dialogs.templates.customPropertiesPartial(config)}
			</div>
		`,
		contentOptions: config => `
			<div id="default-dom-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(Object.assign({ doubleWide: true }, config))}

					${_Dialogs.templates.repeaterPartial(config)}

					${_Dialogs.templates.visibilityPartial(config)}

				</div>

				${_Dialogs.templates.customPropertiesPartial(config)}
			</div>
		`,
		contentPartial: config => `
			<div id="child-content-editor" class="option-tile col-span-2 hidden">
				<label class="block mb-2" for="content-input">Text Content</label>
				<textarea id="content-input" name="content" data-defer-change-handler="true"></textarea>
			</div>
		`,
		customPropertiesPartial: config => `
			<div id="custom-properties-parent" class="hidden">
				<h3>Custom Attributes</h3>
				<div id="custom-properties-container"></div>
			</div>
		`,
		defaultDOMOptions: config => `
			<div id="default-dom-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(Object.assign({ doubleWide: true }, config))}

					${_Dialogs.templates.htmlClassTile(config)}

					${_Dialogs.templates.htmlIdTile(config)}

					${_Dialogs.templates.htmlStyleTile(config)}

					${_Dialogs.templates.repeaterPartial(config)}

					${_Dialogs.templates.visibilityPartial(config)}

					${_Dialogs.templates.contentPartial()}

				</div>

				${_Dialogs.templates.renderingOptions(config)}

				${_Dialogs.templates.customPropertiesPartial(config)}
			</div>
		`,
		divOptions: config => `
			<div id="div-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(Object.assign({ doubleWide: true }, config))}

					${_Dialogs.templates.htmlClassTile(config)}

					${_Dialogs.templates.htmlIdTile(config)}

					${_Dialogs.templates.htmlStyleTile(config)}

					${_Dialogs.templates.repeaterPartial(config)}

					${_Dialogs.templates.visibilityPartial(config)}

				</div>

				${_Dialogs.templates.renderingOptions(config)}

				${_Dialogs.templates.customPropertiesPartial(config)}

			</div>
		`,
		includeInFrontendExport: config => `
			<div class="mb-2 flex items-center">
				<input type="checkbox" name="includeInFrontendExport" id="includeInFrontendExport">
				<label for="includeInFrontendExport" data-comment-config='{"insertAfter":true}' data-comment="If checked this file/folder is exported in the deployment process. If a parent folder has this flag enabled, it will automatically be exported and the flag does not need to be set.">Include in frontend export</label>
			</div>
		`,
		fileOptions: config => `
			<div id="file-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(config)}

					<div class="option-tile">
						<label class="block mb-2" for="content-type-input">Content Type</label>
						<input type="text" id="content-type-input" autocomplete="off" name="contentType">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="cache-for-seconds-input" class="block">Cache for n seconds</label>
						<input type="text" id="cache-for-seconds-input" value="" name="cacheForSeconds">
					</div>

					<div class="option-tile">

						<label class="block mb-2">Options</label>

						<div class="mb-2 flex items-center">
							<input type="checkbox" name="isTemplate" id="isTemplate">
							<label for="isTemplate">Is template (dynamic file)</label>
						</div>

						<div class="mb-2 flex items-center">
							<input type="checkbox" name="dontCache" id="dontCache">
							<label for="dontCache">Caching disabled</label>
						</div>

						${_Dialogs.templates.includeInFrontendExport(config)}

						<div class="mb-2 flex items-center">
							<input type="checkbox" name="useAsJavascriptLibrary" id="useAsJavascriptLibrary">
							<label for="useAsJavascriptLibrary" data-comment-config='{"insertAfter":true}' data-comment="If checked this file can be included via <code>$.includeJs(fileName)</code> in any other server-side JavaScript context.<br><br>File must have content-type <code>text/javascript</code> or <code>application/javascript</code>">Use As Javascript Library</label>
						</div>
					</div>
				</div>

				<div id="content-extraction" class="hidden">
					<h3>Content Extraction</h3>
					<div>
						<p>Extract text content from this document or image and store it in a StructuredDocument node with StructuredTextNode children.</p>
						<button type="button" class="action" id="extract-structure-button">Extract document content</button>
					</div>
				</div>
			</div>
		`,
		folderOptions: config => `
			<div id="file-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(config)}

					<div class="option-tile">

						<label class="block mb-2">Options</label>

						${_Dialogs.templates.includeInFrontendExport(config)}

					</div>
				</div>
			</div>
		`,
		inputOptions: config => `
			<div id="div-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(Object.assign({ doubleWide: true }, config))}

					${_Dialogs.templates.htmlClassTile(config)}

					${_Dialogs.templates.htmlIdTile(config)}

					${_Dialogs.templates.htmlTypeTile(config)}

					<div class="option-tile">
						<label class="block mb-2" for="placeholder-input">Placeholder</label>
						<input type="text" id="placeholder-input" name="_html_placeholder">
					</div>

					${_Dialogs.templates.htmlStyleTile(config)}

					${_Dialogs.templates.htmlTitleTile(config)}

					${_Dialogs.templates.visibilityPartial(config)}

				</div>

				${_Dialogs.templates.customPropertiesPartial(config)}
			</div>
		`,
		ldapGroup: config => `
			<div id="ldap-group-config">
				<h3>Synchronize this group using distinguished name (prioritized if set)</h3>

				<div class="mb-3">
					<input type="text" size="80" id="ldap-group-dn" placeholder="Distinguished Name" name="distinguishedName">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-dn', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
				</div>

				<h3>Synchronize this group using path, filter and scope (if distinguished name not set above)</h3>

				<div class="mb-3">
					<input type="text" size="80" id="ldap-group-path" placeholder="Path" name="path">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-path', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
				</div>

				<div class="mb-3">
					<input type="text" size="80" id="ldap-group-filter" placeholder="Filter" name="filter">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-filter', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
				</div>

				<div class="mb-3">
					<input type="text" size="80" id="ldap-group-scope" placeholder="Scope" name="scope">
					${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, _Icons.getSvgIconClassesForColoredIcon(['clear-ldap-group-scope', 'icon-lightgrey', 'cursor-pointer']), 'Clear value')}
				</div>

				<div class="mb-3">
					<button type="button" class="action" id="ldap-sync-button">Synchronize now</button>
				</div>

				<div>
					<a href="/structr/config" target="_blank">Open Structr configuration</a>
				</div>
			</div>
		`,
		optionOptions: config => `
			<div id="div-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(Object.assign({ doubleWide: true }, config))}

					${_Dialogs.templates.htmlClassTile(config)}

					${_Dialogs.templates.htmlIdTile(config)}

					${_Dialogs.templates.htmlStyleTile(config)}

					${_Dialogs.templates.repeaterPartial(config)}

					<div class="option-tile">
						<label class="block mb-2" for="selected-input">Selected</label>
						<input type="text" id="selected-input" name="_html_selected">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="selected-values-input">Selected Values Expression</label>
						<input type="text" id="selected-values-input" name="selectedValues">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="value-input">Value</label>
						<input type="text" id="value-input" name="_html_value">
					</div>

					<div><!-- occupy space in grid UI --></div>

					${_Dialogs.templates.visibilityPartial(config)}

					${_Dialogs.templates.contentPartial()}
				</div>

				${_Dialogs.templates.renderingOptions(config)}

				${_Dialogs.templates.customPropertiesPartial(config)}

			</div>
		`,
		pageOptions: config => `
			<div id="div-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(config)}

					<div class="option-tile">
						<label class="block mb-2" for="content-type-input">Content Type</label>
						<input type="text" id="content-type-input" name="contentType">
					</div>

					<div class="option-tile">
						<label  class="mb-2"for="category-input">Category</label>
						<input type="text" id="category-input" name="category">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="show-on-error-codes-input">Show on Error Codes</label>
						<input type="text" id="show-on-error-codes-input" name="showOnErrorCodes">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="position-input">Position</label>
						<input type="text" id="position-input" name="position">
					</div>

					<div class="option-tile">

						<label class="block mb-2">Options</label>

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

						<div class="mb-2 flex items-center">
							<label for="_auto-refresh" data-comment="Auto-refresh page preview on changes (if page preview is active)">
								<input id="_auto-refresh" type="checkbox" ${(LSWrapper.getItem(_Pages.autoRefreshDisabledKey + config.entity.id) ? '' : ' checked="checked"')}> Auto-refresh
							</label>
						</div>

					</div>

					<div class="option-tile">
						<label class="block mb-2" for="_details-object-id" data-comment="UUID of detail object to append to preview URL">Preview Detail Object</label>
						<input id="_details-object-id" type="text" value="${(LSWrapper.getItem(_Pages.detailsObjectIdKey + config.entity.id) ? LSWrapper.getItem(_Pages.detailsObjectIdKey + config.entity.id) : '')}">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="_request-parameters" data-comment="Request parameters to append to preview URL">Preview Request Parameters</label>
						<div class="flex items-baseline">
							<code>?</code>
							<input id="_request-parameters" type="text" value="${(LSWrapper.getItem(_Pages.requestParametersKey + config.entity.id) ? LSWrapper.getItem(_Pages.requestParametersKey + config.entity.id) : '')}">
						</div>
					</div>
				</div>

				${_Dialogs.templates.customPropertiesPartial(config)}

			</div>
		`,
		renderingOptions: config => `
			<div id="rendering-options-container" class="hidden">
				<h3>Rendering Options</h3>

				<div class="grid grid-cols-2 gap-8">

					<div class="option-tile">
						<label class="block mb-3" for="rendering-mode-select" data-comment="Select rendering mode for this element to activate lazy loading.">Select rendering mode for this element to activate lazy loading.</label>
						<select class="select2" id="rendering-mode-select" name="data-structr-rendering-mode">
							<option value="">Eager (default)</option>
							<option value="load">When page has finished loading</option>
							<option value="delayed">With a delay after page has finished loading</option>
							<option value="visible">When element becomes visible</option>
							<option value="periodic">With periodic updates</option>
						</select>
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="rendering-delay-or-interval">Delay or interval in milliseconds</label>
						<input type="number" id="rendering-delay-or-interval" name="data-structr-delay-or-interval">
					</div>
				</div>
			</div>
		`,
		repeaterPartial: config => `
			<div class="option-tile">
				<label class="block mb-2" for="function-query-input">Function Query</label>
				<input type="text" id="function-query-input" name="functionQuery">
			</div>

			<div class="option-tile">
				<label class="block mb-2" for="data-key-input">Data Key</label>
				<input type="text" id="data-key-input" name="dataKey">
			</div>
		`,
		userOptions: config => `
			<div id="div-options" class="quick-access-options">

				<div class="grid grid-cols-2 gap-8">

					${_Dialogs.templates.nameTile(config)}

					<div class="option-tile">
						<label class="block mb-2" for="e-mail-input">eMail</label>
						<input type="text" id="e-mail-input" autocomplete="off" name="eMail">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="password-input">Password</label>
						<input type="password" id="password-input" autocomplete="new-password" value="****** HIDDEN ******">
						<button class="action" type="button" id="set-password-button">Set Password</button>
					</div>

					<div class="option-tile">

						<label class="block mb-2">Options</label>

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

					<div class="option-tile">
						<label class="block mb-2" for="password-attempts-input" data-comment="The number of failed login attempts for this user. Depending on the configuration a user is blocked after a certain number of failed login attempts. The user must then reset their password (if allowed via the configuration) or this counter must be reset by an admin.<br><br>Before that threshold is reached, the counter is reset on each successful login.">Failed Login Attempts</label>
						<input type="text" id="password-attempts-input" name="passwordAttempts">
					</div>

					<div class="option-tile">
						<label class="block mb-2" for="confirmation-key" data-comment="Used for self-registration and password reset. If a confirmation key is set, log in via password is prevented.">Confirmation Key</label>
						<input type="text" id="confirmation-key" name="confirmationKey">
					</div>
				</div>

				${_Dialogs.templates.customPropertiesPartial(config)}

			</div>
		`,
		showHideConditionTemplates: config => `
			<option value="" disabled selected>Example ${config.type} conditions...</option>
			<option value="">(none)</option>
			<option>true</option>
			<option>false</option>
			<option>me.isAdmin</option>
			<option>empty(current)</option>
			<option>not(empty(current))</option>
		`,
		visibilityPartial: config => `
			<div class="option-tile">
				<label class="block mb-2" for="show-conditions">Show Conditions</label>
				<div class="flex">
					<input id="show-conditions" type="text" name="showConditions">
					<select id="show-conditions-templates" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
						${_Dialogs.templates.showHideConditionTemplates({ type: 'show' })}
					</select>
				</div>
			</div>

			<div class="option-tile">
				<label class="block mb-2" for="hide-conditions">Hide Conditions</label>
				<div class="flex">
					<input id="hide-conditions" type="text" name="hideConditions">
					<select id="hide-conditions-templates" class="hover:bg-gray-100 focus:border-gray-666 active:border-green">
						${_Dialogs.templates.showHideConditionTemplates({ type: 'hide' })}
					</select>
				</div>
			</div>
		`
	}
};