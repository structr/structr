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
	basic: {
		wrapperClass: 'non-block-ui-wrapper',
		contentClass: 'non-block-ui-blocker-content',
		append: (messageHtml = '', overrideCss = {}) => {

			let content_max_zIndex = [...document.querySelectorAll(`.${_Dialogs.basic.contentClass}`)].reduce((content_zIndex, element) => Math.max(element.style.zIndex, content_zIndex), 9998) + 1;

			let contentElementCss = Object.assign({
				width: '30vw;'
			}, overrideCss);

			let overlay = _Helpers.createSingleDOMElementFromHTML(`
				<div class="${_Dialogs.basic.wrapperClass}">
					<div style="
						z-index: ${content_max_zIndex - 1};
						position: fixed;
						top: 0;
						left: 0;
						border: none;
						margin: 0;
						padding: 0;
						width: 100%;
						height: 100%;
						cursor: initial;
						background-color: var(--solid-black);
						opacity: 0.6;
					"></div>
					<div class="${_Dialogs.basic.contentClass}" style="
						z-index: ${content_max_zIndex};
						position: fixed;
						top: 40vh;
						left: 35vw;
						background-color: var(--gray-eee);
						box-shadow: 0 0 .1rem var(--gray-999);
					">
						${messageHtml}
					</div>
				</div>
			`);

			let contentElement = overlay.querySelector(`.${_Dialogs.basic.contentClass}`);

			Object.assign(contentElement.style, contentElementCss);

			document.body.appendChild(overlay);

			// if no message is provided, or more content is added in the caller, then the caller must handle this
			_Dialogs.basic.centerAll();

			// inner element is returned to allow adding elements there
			return contentElement;
		},
		centerAll: () => {

			for (let messageDiv of document.querySelectorAll(`.${_Dialogs.basic.contentClass}`)) {

				messageDiv.style.top  = `${(window.innerHeight - messageDiv.offsetHeight) / 2}px`;
				messageDiv.style.left = `${(window.innerWidth - messageDiv.offsetWidth) / 2}px`;
			}
		},
		removeBlockerAround: (element) => {

			if (element) {

				let wrapper = element.closest(`.${_Dialogs.basic.wrapperClass}`);

				if (wrapper) {

					_Helpers.fastRemoveElement(wrapper);
				}
			}
		},
	},
	loginDialog: {
		isOpen: () => {
			let loginElement = document.querySelector('#login');
			return (loginElement != null && loginElement.offsetParent !== null);
		},
		show: () => {

			if (_Dialogs.loginDialog.isOpen() === false) {

				StructrWS.reconnect();

				Structr.getActiveModule()?.unload?.();

				_Favorites.logoutAction();
				_Console.logoutAction();

				Structr.clearMain();

				// show login box
				_Dialogs.basic.append(Structr.templates.loginDialogMarkup, {
					width: ''
				});

				document.querySelector('#usernameField').focus();

				_Dialogs.basic.centerAll();

				document.querySelector('form#login-username-password').addEventListener('submit', (e) => {
					e.stopPropagation();
					e.preventDefault();

					Structr.doLogin({
						username: document.querySelector('#usernameField').value,
						password: document.querySelector('#passwordField').value
					});
					return false;
				});

				document.querySelector('form#login-two-factor').addEventListener('submit', (e) => {
					e.stopPropagation();
					e.preventDefault();

					Structr.doLogin({
						twoFactorCode: document.querySelector('#twoFactorCodeField').value,
						twoFactorToken: document.querySelector('#twoFactorTokenField').value
					});
					return false;
				});

				$('#logout_').text('Login');
			}
		},
		hide: () => {
			_Dialogs.basic.removeBlockerAround(document.querySelector('#login'));
		},
		appendErrorMessage: (msg) => {

			if (msg && _Dialogs.loginDialog.isOpen()) {

				_Dialogs.loginDialog.removeErrorMessages();

				let element = _Helpers.createSingleDOMElementFromHTML(`<div class="login-error-message w-full box-border icon-red mt-2 pr-4 text-right">${msg}</div>`);

				document.querySelector('#login').appendChild(element);

				window.setTimeout(() => {
					element.remove();
				}, 2500);
			}
		},
		removeErrorMessages: () => {
			[...document.querySelectorAll('#login .login-error-message')].map(m => m.remove());
		},
		showTwoFactor: (data) => {

			_Dialogs.loginDialog.removeErrorMessages();

			document.querySelector('#login-username-password').style.display = 'none';
			document.querySelector('#login-two-factor').style.display = 'block';

			document.querySelector('#usernameField').value = '';
			document.querySelector('#passwordField').value = '';

			document.querySelector('#twoFactorCodeField').focus();

			if (data.qrdata) {
				$('#login-two-factor #two-factor-qr-code').show();
				$('#login-two-factor img').attr('src', `data:image/png;base64, ${data.qrdata}`);
			}

			$('#twoFactorTokenField').val(data.token);

			window.setTimeout(_Dialogs.basic.centerAll, 0);
		},
		hideTwoFactor: () => {
			document.querySelector('#login-username-password').style.display = 'block';
			document.querySelector('#login-two-factor').style.display = 'none';

			document.querySelector('#twoFactorTokenField').value = '';
			document.querySelector('#twoFactorCodeField').value = '';
		}
	},
	configLoginDialog: {
		show: () => {

			// show login box
			_Dialogs.basic.append(_Config.templates.configLoginDialogMarkup, {
				width: ''
			});

			document.querySelector('#passwordField').focus();

			_Dialogs.basic.centerAll();
		}
	},
	tempInfoBox: {
		show: (messageHtml = '') => {

			let tempInfoMessage = `
				<div class="text-center">
					<div class="infoHeading"></div>
					<div class="infoMsg min-h-20">
						${messageHtml}
					</div>
					<div class="flex justify-end">
						<button class="closeButton hover:bg-gray-100 focus:border-gray-666 active:border-green">Close</button>
					</div>
				</div>
			`;
			let messageDiv  = _Dialogs.basic.append(tempInfoMessage, { padding: '1rem' });
			let closeButton = messageDiv.querySelector('.closeButton');

			closeButton.addEventListener('click', (e) => {
				_Dialogs.basic.removeBlockerAround(messageDiv);
			});

			return {
				headingEl: messageDiv.querySelector('.infoHeading'),
				messageEl: messageDiv.querySelector('.infoMsg'),
				closeButton: closeButton
			};
		},
	},
	confirmation: {
		showPromise: (text, defaultOption = true) => {

			return new Promise((resolve, reject) => {

				let confirmationMessage = `
					<div class="text-center">
						<div class="confirmationText mb-4">
							${text}
						</div>
						<button class="yesButton inline-flex items-center hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, ['icon-green', 'mr-2'])} Yes
						</button>
						<button class="noButton inline-flex items-center hover:bg-gray-100 hover:bg-gray-100 focus:border-gray-666 active:border-green">
							${_Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, ['icon-red', 'mr-2'])} No
						</button>
					</div>
				`;

				let messageDiv = _Dialogs.basic.append(confirmationMessage, { padding: '1rem' });

				let yesButton = messageDiv.querySelector('.yesButton');
				let noButton  = messageDiv.querySelector('.noButton');

				let answerFunction = (e, response) => {
					e.stopPropagation();

					_Dialogs.basic.removeBlockerAround(messageDiv);

					resolve(response);
				};

				yesButton.addEventListener('click', (e) => {
					answerFunction(e, true);
				});

				noButton.addEventListener('click', (e) => {
					answerFunction(e, false);
				});

				messageDiv.addEventListener('keyup', (e) => {
					if (e.key === 'Escape' || e.code === 'Escape' || e.keyCode === 27) {
						answerFunction(e, false);
					}
				});

				if (defaultOption === true) {
					yesButton.focus();
				} else {
					noButton.focus();
				}
			});
		},
	},
	spinner: {
		// spinner has unique id and if called with an existing spinner, the previous one is removed
		id: 'structr-loading-spinner',
		show: () => {

			_Dialogs.spinner.hide();

			let spinnerHTML = `
				<div id="${_Dialogs.spinner.id}">
					${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 36, 36)}
				</div>
			`;

			_Dialogs.basic.append(spinnerHTML, {
				backgroundColor : 'transparent',
				border          : 'none',
				boxShadow       : 'none'
			});

		},
		hide: () => {

			let spinner = document.getElementById(_Dialogs.spinner.id);
			_Dialogs.basic.removeBlockerAround(spinner);
		},
	},
	loadingMessage: {
		defaultId: 'default-loading-message',
		show: (title = 'Executing Task', text = 'Please wait until the operation has finished...', messageId = _Dialogs.loadingMessage.defaultId) => {

			_Dialogs.loadingMessage.hide(messageId);

			let loadingMessage = `
				<div id="${messageId}" class="text-center">
					<div class="flex items-center justify-center">
						${_Icons.getSvgIcon(_Icons.iconWaitingSpinner, 24, 24, 'mr-2')}<b>${title}</b>
					</div>
					<br>
					${text}
				</div>
			`;

			_Dialogs.basic.append(loadingMessage, { padding: '1rem' });
		},
		hide: (messageId = _Dialogs.loadingMessage.defaultId) => {

			let existingMessage = document.getElementById(messageId);
			_Dialogs.basic.removeBlockerAround(existingMessage);
		},
	},
	custom: {
		isMaximized:        false,
		dialogMaximizedKey: `structrDialogMaximized_${location.port}`,
		dialogDataKey:      `structrDialogData_${location.port}`,
		elements: {
			dialogMsg: null,
			dialogCancelButton: null,
			dialogSaveButton: null,
			saveAndClose: null,
			dialogBtn: null,
			dialogTitle: null,
			dialogText: null,
			dialogMeta: null
		},
		clearDialogElements: () => {

			if (_Dialogs.custom.elements.dialogBox) {
				_Dialogs.basic.removeBlockerAround(_Dialogs.custom.elements.dialogBox);
			}

			_Dialogs.custom.elements.dialogBox          = null;
			_Dialogs.custom.elements.dialogText         = null;
			_Dialogs.custom.elements.dialogMsg          = null;
			_Dialogs.custom.elements.dialogBtn          = null;
			_Dialogs.custom.elements.dialogTitle        = null;
			_Dialogs.custom.elements.dialogMeta         = null;
			_Dialogs.custom.elements.dialogCancelButton = null;
			_Dialogs.custom.elements.dialogSaveButton   = null;
			_Dialogs.custom.elements.saveAndClose       = null;
		},
		updateGlobalDialogVariables: () => {

			_Dialogs.custom.elements.dialogBox          = document.querySelector('#dialogBox');
			_Dialogs.custom.elements.dialogText         = _Dialogs.custom.elements.dialogBox.querySelector('.dialogText');
			_Dialogs.custom.elements.dialogMsg          = _Dialogs.custom.elements.dialogBox.querySelector('.dialogMsg');
			_Dialogs.custom.elements.dialogBtn          = _Dialogs.custom.elements.dialogBox.querySelector('.dialogBtn');
			_Dialogs.custom.elements.dialogTitle        = _Dialogs.custom.elements.dialogBox.querySelector('.dialogTitle');
			_Dialogs.custom.elements.dialogMeta         = _Dialogs.custom.elements.dialogBox.querySelector('.dialogMeta');
			_Dialogs.custom.elements.dialogCancelButton = _Dialogs.custom.elements.dialogBox.querySelector('.closeButton');
			_Dialogs.custom.elements.dialogSaveButton   = _Dialogs.custom.elements.dialogBox.querySelector('.save');
		},
		getDialogBoxElement:   () => _Dialogs.custom.elements.dialogBox,
		getDialogTitleElement: () => _Dialogs.custom.elements.dialogTitle,
		getDialogTextElement:  () => _Dialogs.custom.elements.dialogText,
		getDialogMetaElement:  () => _Dialogs.custom.elements.dialogMeta,
		getDialogBoxElement:   () => _Dialogs.custom.elements.dialogBox,
		isDialogOpen: () => {
			return (_Dialogs.custom.elements.dialogBox && _Dialogs.custom.elements.dialogBox.offsetParent);
		},
		setDialogSize: (windowWidth, windowHeight, dialogWidth, dialogHeight) => {

			let horizontalOffset = 130;

			let dialogTextWrapperElement = _Dialogs.custom.elements.dialogBox.querySelector('.dialogTextWrapper');
			if (dialogTextWrapperElement) {

				dialogTextWrapperElement.style.width  = `calc(${dialogWidth}px - 3rem)`;
				dialogTextWrapperElement.style.height = `${dialogHeight - horizontalOffset}px`;
			}

			// needed for maximized dialog (currently assumes there is only one dialog!)
			let blockPageElement = document.querySelector(`.${_Dialogs.basic.contentClass}`);
			if (blockPageElement) {

				let left = (windowWidth - dialogWidth) / 2;
				let top  = (windowHeight - dialogHeight) / 2;

				blockPageElement.style.width = `${dialogWidth}px`;
				blockPageElement.style.top   = `${top}px`;
				blockPageElement.style.left  = `${left}px`;
			}
		},
		getDialogDimensions: (marginLeft, marginTop) => {

			let winW = $(window).width();
			let winH = $(window).height();

			let width  = Math.min(900, winW - marginLeft);
			let height = Math.min(600, winH - marginTop);

			return {
				width:  width,
				height: height,
				left:   (winW - width) / 2,
				top:    (winH - height) / 2
			};
		},
		resizeDialog: () => {

			_Dialogs.custom.isMaximized = LSWrapper.getItem(_Dialogs.custom.dialogMaximizedKey);

			if (_Dialogs.custom.isDialogOpen()) {

				if (_Dialogs.custom.isMaximized) {

					_Dialogs.custom.maximizeDialogButtonAction();

				} else {

					// Calculate dimensions of dialog
					if (_Dialogs.custom.isDialogOpen() && !_Dialogs.loginDialog.isOpen()) {

						_Dialogs.custom.setDialogSize($(window).width(), $(window).height(), Math.min(900, $(window).width() - 24), Math.min(600, $(window).height() - 24));

						if (_Dialogs.custom.getMinimizeDialogButton()) _Dialogs.custom.getMinimizeDialogButton().style.display = 'none';
						if (_Dialogs.custom.getMaximizeDialogButton()) _Dialogs.custom.getMaximizeDialogButton().style.display = 'block';
					}
				}
			}

			_Dialogs.basic.centerAll();
		},
		openDialog: (dialogTitleText = '', callbackCancel, customClasses = []) => {

			_Dialogs.custom.blockUI();

			_Dialogs.custom.clearDialogElements();
			_Dialogs.custom.updateGlobalDialogVariables();

			_Dialogs.custom.getMinimizeDialogButton().addEventListener('click', _Dialogs.custom.minimizeDialogButtonAction);
			_Dialogs.custom.getMaximizeDialogButton().addEventListener('click', _Dialogs.custom.maximizeDialogButtonAction);
			_Dialogs.custom.getCloseDialogButton().addEventListener('click', _Dialogs.custom.clickDialogCancelButton);

			_Dialogs.custom.getDialogBoxElement().classList.add(...["dialog", ...customClasses]);

			_Dialogs.custom.getDialogTitleElement().textContent = dialogTitleText;

			let newCancelButton = _Dialogs.custom.updateOrCreateDialogCloseButton();

			newCancelButton.addEventListener('click', (e) => {
				e.stopPropagation();

				_Dialogs.custom.dialogCancelBaseAction();

				if (callbackCancel) {
					window.setTimeout(callbackCancel, 100);
				}
			});

			Structr.resize();

			let dimensions = _Dialogs.custom.getDialogDimensions(24, 24);
			dimensions.text = dialogTitleText;
			LSWrapper.setItem(_Dialogs.custom.dialogDataKey, JSON.stringify(dimensions));

			return {
				dialogText: _Dialogs.custom.getDialogTextElement(),
				dialogMeta: _Dialogs.custom.getDialogMetaElement()
			};
		},
		showMeta: () => {

			let meta = _Dialogs.custom.getDialogMetaElement();

			if (meta) {
				meta.style.display = null;
			}
		},
		blockUI: (elements) => {

			_Dialogs.custom.clearDialogElements();

			if (elements) {

				let dialog = _Dialogs.basic.append();
				dialog.append(...elements);
				return dialog;

			} else {

				let dialogHTML = `
					<div id="dialogBox" class="dialog">
						<i title="Fullscreen Mode" id="maximizeDialog" class="window-icon minmax">${_Icons.getSvgIcon(_Icons.iconMaximizeDialog, 18, 18)}</i>
						<i title="Window Mode" id="minimizeDialog" class="window-icon minmax">${_Icons.getSvgIcon(_Icons.iconMinimizeDialog, 18, 18)}</i>
						<i title="Close" id="closeDialog" class="window-icon close">${_Icons.getSvgIcon(_Icons.iconCrossIcon, 18, 18)}</i>

						<h2 class="dialogTitle"></h2>

						<div class="dialogTextWrapper">
							<div class="dialogText"></div>
						</div>

						<div class="dialogMsg"></div>

						<div class="dialogMeta"></div>

						<div class="dialogBtn flex"></div>
					</div>
				`;
				return _Dialogs.basic.append(dialogHTML, { width: '' });
			}
		},
		getMinimizeDialogButton: () => _Dialogs.custom.getDialogBoxElement().querySelector('#minimizeDialog'),
		getMaximizeDialogButton: () => _Dialogs.custom.getDialogBoxElement().querySelector('#maximizeDialog'),
		getCloseDialogButton: () => _Dialogs.custom.getDialogBoxElement().querySelector('#closeDialog'),
		maximizeDialogButtonAction: () => {

			// Calculate dimensions of dialog
			if (_Dialogs.custom.isDialogOpen() && !_Dialogs.loginDialog.isOpen()) {
				_Dialogs.custom.setDialogSize($(window).width(), $(window).height(), $(window).width() - 24, $(window).height() - 24);
			}

			_Dialogs.custom.isMaximized = true;
			if (_Dialogs.custom.getMinimizeDialogButton()) _Dialogs.custom.getMinimizeDialogButton().style.display = 'block';
			if (_Dialogs.custom.getMaximizeDialogButton()) _Dialogs.custom.getMaximizeDialogButton().style.display = 'none';

			LSWrapper.setItem(_Dialogs.custom.dialogMaximizedKey, '1');

			Structr.getActiveModule()?.dialogSizeChanged?.();
		},
		minimizeDialogButtonAction: () => {

			_Dialogs.custom.isMaximized = false;
			LSWrapper.removeItem(_Dialogs.custom.dialogMaximizedKey);
			Structr.resize();

			Structr.getActiveModule()?.dialogSizeChanged?.();
		},
		dialogCancelBaseAction: () => {

			_Dialogs.custom.clearDialogElements();

			Structr.focusSearchField();

			LSWrapper.removeItem(_Dialogs.custom.dialogDataKey);
		},
		clickDialogCancelButton: () => {

			if (_Dialogs.custom.elements.dialogCancelButton && _Dialogs.custom.elements.dialogCancelButton.offsetParent && !_Dialogs.custom.elements.dialogCancelButton.disabled) {
				_Dialogs.custom.elements.dialogCancelButton.click();
			}
		},
		clickSaveButton: () => {

			if (_Dialogs.custom.elements.dialogSaveButton && _Dialogs.custom.elements.dialogSaveButton.offsetParent && !_Dialogs.custom.elements.dialogSaveButton.disabled) {
				_Dialogs.custom.elements.dialogSaveButton.click();
			}
		},
		clickSaveAndCloseButton: () => {

			if (_Dialogs.custom.elements.dialogSaveButton && _Dialogs.custom.elements.dialogSaveButton.offsetParent && !_Dialogs.custom.elements.dialogSaveButton.disabled) {
				_Dialogs.custom.elements.dialogSaveButton.click();
			}
		},
		checkSaveOrCloseOnEscape: () => {

			if (_Dialogs.custom.isDialogOpen() && _Dialogs.custom.elements.dialogSaveButton && _Dialogs.custom.elements.dialogSaveButton.offsetParent && !_Dialogs.custom.elements.dialogSaveButton.disabled) {

				let saveBeforeExit = confirm('Save changes?');
				if (saveBeforeExit) {
					_Dialogs.custom.clickSaveButton();

					window.setTimeout(_Dialogs.custom.clickDialogCancelButton, 500);
				}

			} else {

				_Dialogs.custom.clickDialogCancelButton();
			}
		},
		restoreDialog: () => {

			window.setTimeout(() => {

				_Dialogs.custom.blockUI(Structr.dialogContainerOffscreen.children);
				_Dialogs.custom.clearDialogElements();
				_Dialogs.custom.updateGlobalDialogVariables();
				Structr.resize();

			}, 1000);
		},
		updateOrCreateDialogCloseButton: () => {

			if (_Dialogs.custom.elements.dialogCancelButton) {
				// removes attached event handlers but leaves the HTML intact
				_Dialogs.custom.elements.dialogCancelButton.replaceWith(_Dialogs.custom.elements.dialogCancelButton.cloneNode(true));
			} else {
				_Dialogs.custom.elements.dialogBtn.insertAdjacentHTML('afterbegin', '<button class="closeButton hover:bg-gray-100 focus:border-gray-666 active:border-green">Close</button>');
			}
			_Dialogs.custom.elements.dialogCancelButton = _Dialogs.custom.elements.dialogBtn.querySelector('.closeButton');

			return _Dialogs.custom.elements.dialogCancelButton;
		},
		replaceDialogCloseButton: (button, inPlace = true) => {

			if (!_Dialogs.custom.elements.dialogCancelButton) {
				_Dialogs.custom.elements.dialogBtn.appendChild(button);
			} else {
				if (inPlace) {
					_Dialogs.custom.elements.dialogCancelButton.replaceWith(button);
				} else {
					_Helpers.fastRemoveElement(_Dialogs.custom.elements.dialogCancelButton);
					_Dialogs.custom.elements.dialogBtn.appendChild(button);
				}
			}

			_Dialogs.custom.elements.dialogCancelButton = button;
		},
		updateOrCreateDialogSaveButton: (defaultClasses = ['hover:bg-gray-100', 'focus:border-gray-666', 'active:border-green']) => {

			if (_Dialogs.custom.elements.dialogSaveButton) {
				_Dialogs.custom.elements.dialogSaveButton.replaceWith(_Dialogs.custom.elements.dialogSaveButton.cloneNode(true));
			} else {
				_Dialogs.custom.elements.dialogBtn.insertAdjacentHTML('beforeend', `<button id="dialogSaveButton" disabled="disabled" class="disabled ${defaultClasses.join(' ')}">Save</button>`);
			}

			_Dialogs.custom.elements.dialogSaveButton = _Dialogs.custom.elements.dialogBtn.querySelector('#dialogSaveButton');

			return _Dialogs.custom.elements.dialogSaveButton;
		},
		updateOrCreateDialogSaveAndCloseButton: (defaultClasses = ['hover:bg-gray-100', 'focus:border-gray-666', 'active:border-green']) => {

			if (_Dialogs.custom.elements.saveAndClose) {
				_Dialogs.custom.elements.saveAndClose.replaceWith(_Dialogs.custom.elements.saveAndClose.cloneNode(true));
			} else {
				_Dialogs.custom.elements.dialogBtn.insertAdjacentHTML('beforeend', `<button id="dialogSaveAndCloseButton" disabled="disabled" class="disabled ${defaultClasses.join(' ')}">Save and close</button>`);
			}

			_Dialogs.custom.elements.saveAndClose = _Dialogs.custom.elements.dialogBtn.querySelector('#dialogSaveAndCloseButton');

			return _Dialogs.custom.elements.saveAndClose;
		},
		setDialogSaveButton: (button) => {
			_Dialogs.custom.elements.dialogSaveButton = button;
		},
		appendCustomDialogButton: (buttonHtml) => {

			let button = _Helpers.createSingleDOMElementFromHTML(buttonHtml);
			_Dialogs.custom.elements.dialogBtn.appendChild(button);

			return button;
		},
		prependCustomDialogButton: (buttonHtml) => {

			let button = _Helpers.createSingleDOMElementFromHTML(buttonHtml);
			_Dialogs.custom.elements.dialogBtn.prepend(button);

			return button;
		},
		showAndHideInfoBoxMessage: (msg, msgClass, stayTime = 2000, fadeoutTime = 1000) => {

			if (_Dialogs.custom.elements.dialogMsg) {

				_Helpers.fastRemoveAllChildren(_Dialogs.custom.elements.dialogMsg);

				let newDiv = _Helpers.createSingleDOMElementFromHTML(`
					<div class="infoBox ${msgClass} flex items-center p-2">
						${_Icons.getSvgIcon(_Icons.getSvgIconForMessageClass(msgClass), 16, 16, ['mr-2'])}
						<div>
							${msg}
						</div>
					</div>
				`);

				_Dialogs.custom.elements.dialogMsg.appendChild(newDiv);

				window.setTimeout(() => {
					$(newDiv).fadeOut(fadeoutTime, () => { newDiv.remove(); });
				}, stayTime);
			}
		}
	}
};