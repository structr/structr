/*
 * Copyright (C) 2010-2025 Structr GmbH
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
let _Icons = {

	typeIconClass: 'typeIcon',
	typeIconClassNoChildren: 'typeIcon-nochildren',

	preloadSVGIcons: () => {
		// this should be super fast because we inserted a preload rule to the index.html
		fetch('icon/sprites.svg').then(response => {
			return response.text();
		}).then(svgPreload => {
			document.body.insertAdjacentHTML('afterbegin', svgPreload);
		});
	},

	/* fake icon to use as placeholder */
	nonExistentEmptyIcon:       'this-is-empty',

	/* SVG-in-CSS icons */
	collapsedClass:             'svg-collapsed',
	expandedClass:              'svg-expanded',
	monacoGlyphMarginClassName: 'monaco-editor-warning-glyph',

	/* SVG element icons */
	iconEditionEnterprise:    'edition-enterprise',
	iconEditionSmallBusiness: 'edition-small-business',
	iconEditionBasic:         'edition-basic',
	iconEditionCommunity:     'edition-community',

	iconTerminal:             'terminal',
	iconNotificationBell:     'notification-bell',


	iconDOMTreePage:          'dom-page',
	iconDOMTreeElement:       'dom-block',
	iconDOMContentElement:    'dom-content',
	iconDOMTemplateElement:   'dom-template',
	iconDOMCommentElement:    'dom-comment',
	iconDOMTreeActiveElement: 'dom-active-element',

	iconSecurityRegularUser:        'user-normal',
	iconSecurityBlockedRegularUser: 'user-blocked',
	iconSecurityAdminUser:          'user-normal',	// possibly 'user-admin' but color-change to red is preferred at the moment
	iconSecurityBlockedAdminUser:   'user-blocked',	// possibly 'user-admin-blocked' but color-change to red is preferred at the moment
	iconSecurityLDAPUser:           'user-ldap',
	iconSecurityGroup:              'user-group',
	iconSecurityLDAPGroup:          'user-group-ldap',
	iconGroupAdd:                   'group-add',
	iconUserAdd:                    'user-add',

	iconFilesStack:           'file-stack',
	iconCreateFile:           'file_add',
	iconCreateFolder:         'folder_add',
	iconFavoritesFolder:      'folder_star',
	iconCropImage:            'image-crop',
	iconFolderOpen:           'folder-open-icon',
	iconFolderClosed:         'folder-closed-icon',
	iconMountedFolderOpen:    'folder-link-open-icon',
	iconMountedFolderClosed:  'folder-link-closed-icon',
	iconFileTypeImage:        'file-image',
	iconFileTypeEmpty:        'file-empty',
	iconFileTypePDF:          'file-pdf',
	iconFileTypeCertificate:  'file-certificate',
	iconFileTypeBinary:       'file-terminal',
	iconFileTypeXML:          'file-xml',
	iconFileTypeCSV:          'file-csv',
	iconFileTypeArchive:      'file-archive',
	iconFileTypeWord:         'file-word',
	iconFileTypeExcel:        'file-excel',
	iconFileTypePresentation: 'file-presentation',
	iconFileTypeText:         'file-text',
	iconFileTypeScripting:    'file-code',

	iconSchemaNodeDefault:       'file-code',
	iconSchemaMethods:           'code-icon',
	iconSchemaNodeSchemaMethod:  'circle-empty',
	iconSchemaNodeStaticMethod:  'static-method',
	iconSchemaNodeLifecycleMethod: 'lifecycle-method',
	iconSchemaNodeJavaMethod:    'circle-empty',
	iconSchemaViews:             'tv-icon',
	iconSchemaView:              'view-icon',
	iconSchemaRelationship:      'chain-link',
	iconSchemaPropertyMagic:     'magic_wand',
	iconSchemaPropertyArray:     'array-property',
	iconSchemaPropertyNumeric:   'numeric-property',
	iconSchemaPropertyBoolean:   'boolean-property',
	iconSchemaPropertyCypher:    'database-icon',
	iconSchemaPropertyDate:      'date-property',
	iconSchemaPropertyDouble:    'double-property',
	iconSchemaPropertyEnum:      'enum-property',
	iconSchemaPropertyFunction:  'function-property',
	iconSchemaPropertyString:    'string-property',
	iconSchemaPropertyEncrypted: 'encrypted-property',
	iconScriptWrapped:           'curly-braces-wrap-js',
	iconRecentlyUsed:            'folder_clock',

	iconFlowSymbol:              'circle-empty',

	iconStructrLogo:         'structr-logo',
	iconHamburgerMenu:       'hamburger-icon',
	iconCrossIcon:           'close-dialog-x',
	iconMaximizeDialog:      'maximize-dialog-arrows',
	iconMinimizeDialog:      'minimize-dialog-arrows',
	iconMagicWand:           'magic_wand',
	iconPencilEdit:          'pencil_edit',
	iconFolderRemove:        'folder-remove',
	iconTrashcan:            'trashcan',
	iconCheckmarkBold:       'checkmark_bold',
	iconKebabMenu:           'kebab_icon',
	iconVisibilityKey:       'visibility-lock-key',
	iconVisibilityOpen:      'visibility-lock-open',
	iconVisibilityLocked:    'visibility-lock-locked',
	iconAddToFavorites:      'favorite-star',
	iconRemoveFromFavorites: 'favorite-star-remove',
	iconClone:               'duplicate',
	iconAdd:                 'circle_plus',
	iconListAdd:             'list_add',
	iconInfo:                'info-icon',
	iconWarning:             'warning-triangle',
	iconInvertSelection:     'arrows-shuffle',
	iconDatetime:            'datetime-icon',
	iconTypeVisibility:      'eye-in-square',
	iconEyeOpen:             'eye_open',
	iconEyeStrikeThrough:    'eye_strikethrough',
	iconGlobe:               'globe-icon',
	iconOpenInNewPage:       'link_external',
	iconListWithCog:         'list-cog',
	iconLightBulb:           'light-bulb',
	iconSearch:              'magnifying-glass',
	iconFilterFunnel:        'filter-funnel',
	iconMicrophone:          'microphone-icon',
	iconNetwork:             'network-icon',
	iconRefreshArrows:       'refresh-arrows',
	iconClipboardPencil:     'clipboard-pencil',
	iconResetArrow:          'reset-arrow',
	iconPasswordReset:       'reset-password-templates',
	iconRunButton:           'run_button',
	iconSettingsCog:         'settings-cog',
	iconSettingsWrench:      'wrench',
	iconSliders:             'sliders-icon',
	iconSnapshots:           'snapshots-icon',
	iconSwagger:             'swagger-logo-bw',
	iconTextSettings:        'text-settings',
	iconUIConfigSettings:    'ui_configuration_settings',
	iconExportAsCSV:         'database-download-icon',
	iconImportFromCSV:       'database-upload-icon',
	iconStructrSSmall:       'structr-s-small',
	iconChevronLeft:         'chevron-left',
	iconChevronRight:        'chevron-right',
	iconChevronLeftFilled:   'chevron-left-filled',
	iconChevronRightFilled:  'chevron-right-filled',
	iconDatabase:            'database-icon-color',
	iconErrorRedFilled:      'error-sign-icon-filled',
	iconWarningYellowFilled: 'warning-sign-icon-filled',
	iconSuccessGreenFilled:  'success-sign-icon-filled',
	iconInfoBlueFilled:      'info-sign-icon-filled',
	iconWaitingSpinner:      'waiting-spinner',
	iconHistory:             'history',

	iconLogoAuth0:           'logo-auth0',
	iconLogoMicrosoft:       'logo-microsoft',
	iconLogoFacebook:        'logo-facebook',
	iconLogoGithub:          'logo-github',
	iconLogoGoogle:          'logo-google',
	iconLogoLinkedIn:        'logo-linkedin',

	getSvgIcon: (href, width = 16, height = 16, optionalClasses, title = '') => {
		return _Icons.getSvgIconWithID(null, href, width, height, optionalClasses, title);
	},
	getSvgIconWithID: (id, href, width = 16, height = 16, optionalClasses = [], title = '') => {

		let classString = '';

		if (Array.isArray(optionalClasses)) {
			classString = optionalClasses.join(' ');
		} else if (typeof optionalClasses === 'string') {
			classString = optionalClasses;
		} else {
			// ignore
		}

		return `<svg ${id ? `id="${id}"` : ''} width="${width}" height="${height}" class="${classString}"><title>${title}</title><use href="#${href}"></use></svg>`;
	},
	getMenuSvgIcon: (id, width = 16, height = 16, optionalClasses = [], title = '') => {

		if (Array.isArray(optionalClasses)) {
			optionalClasses.push('mr-2');
		} else if (typeof optionalClasses === 'string') {
			optionalClasses += optionalClasses + ' mr-2';
		}

		return _Icons.getSvgIcon(id, width, height, optionalClasses, title);
	},
	updateSvgIconInElement: (element, from, to) => {

		let use = element.querySelector(`use[*|href="#${from}"]`);

		if (!use) {
			return false;
		}

		use.setAttribute('href', '#' + to);
		return true;
	},
	replaceSvgElementWithRawSvg: (element, html) => {

		let dummy = document.createElement('div');
		dummy.innerHTML = html;
		let newSvgIcon = dummy.firstChild;
		element.replaceWith(newSvgIcon);

		return newSvgIcon;
	},
	getSvgIconFromSvgElement: (element) => {

		let use = element.querySelector(`use[*|href]`);

		if (!use) {
			return false;
		}

		return use.getAttribute('href').slice(1);
	},
	hasSvgIcon: (element, icon) => {

		let use = element.querySelector(`use[*|href="#${icon}"]`);

		return (use !== null);
	},
	getSvgIconClassesForColoredIcon: (customClasses = []) => {
		return [...customClasses].concat(['opacity-80', 'hover:opacity-100', 'cursor-pointer']);
	},
	getSvgIconClassesNonColorIcon: (customClasses = []) => {
		return [...customClasses].concat(['icon-inactive', 'hover:icon-active', 'cursor-pointer']);
	},
	getImageOrIcon: (image) => {

		if (image.contentType && image.contentType.startsWith('image/svg')) {
			return `<img class="icon" src="${image.path}">`;
		}

		if (image.tnSmall) {
			return `<img class="icon" src="${image.tnSmall.path}">`;
		}

		return _Icons.getSvgIcon(_Icons.iconFileTypeImage);
	},
	getFileIconSVG: (file) => {

		let contentType = file.contentType;
		let result      = _Icons.iconFileTypeEmpty;

		if (contentType) {

			switch (contentType) {

				case 'application/pdf':
				case 'application/postscript':
					result = _Icons.iconFileTypePDF;
					break;

				case 'application/x-pem-key':
				case 'application/pkix-cert+pem':
				case 'application/x-iwork-keynote-sffkey':
					result = _Icons.iconFileTypeCertificate;
					break;

				case 'application/octet-stream':
					result = _Icons.iconFileTypeBinary;
					break;

				case 'application/xml':
				case 'text/xml':
					result = _Icons.iconFileTypeXML;
					break;

				case 'text/csv':
					result = _Icons.iconFileTypeCSV;
					break;

				case 'application/x-shellscript':
				case 'application/javascript':
				case 'text/html':
					result = _Icons.iconFileTypeScripting;
					break;

				case 'application/java-archive':
				case 'application/zip':
				case 'application/rar':
				case 'application/x-bzip':
					result = _Icons.iconFileTypeArchive;
					break;

				case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
				case 'application/vnd.oasis.opendocument.text':
				case 'application/msword':
					result = _Icons.iconFileTypeWord;
					break;

				case 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet':
				case 'application/vnd.oasis.opendocument.spreadsheet':
				case 'application/vnd.ms-excel':
					result = _Icons.iconFileTypeExcel;
					break;

				case 'application/vnd.openxmlformats-officedocument.presentationml.presentation':
				case 'application/vnd.oasis.opendocument.chart':
					result = _Icons.iconFileTypePresentation;
					break;

				default:
					if (contentType.startsWith('image/')) {
						result = _Icons.iconFileTypeImage;
					} else if (contentType.startsWith('text/')) {
						result = _Icons.iconFileTypeText;
					}
			}
		}

		return result;
	},
	getAccessControlIconId: (entity) => {

		let iconId = _Icons.iconVisibilityKey;

		if (true === entity.visibleToPublicUsers && true === entity.visibleToAuthenticatedUsers) {

			iconId = _Icons.iconVisibilityOpen;

		} else if (false === entity.visibleToPublicUsers &&  false === entity.visibleToAuthenticatedUsers) {

			iconId = _Icons.iconVisibilityLocked;
		}

		return iconId;
	},
	getFolderIconSVG: (d) => {
		return (d.isMounted) ? _Icons.iconMountedFolderClosed : _Icons.iconFolderClosed
	},
	getIconForEdition: (edition) => {
		switch (edition) {
			case 'Enterprise':
				return _Icons.iconEditionEnterprise;

			case 'Small Business':
				return _Icons.iconEditionSmallBusiness;

			case 'Basic':
				return _Icons.iconEditionBasic;

			case 'Community':
			default:
				return _Icons.iconEditionCommunity;
		}
	},
	getIconForPrincipal: (principal) => {

		// admin group

		if (principal.isGroup) {

			if (principal.isAdmin) {
				return _Icons.getSvgIcon(_Icons.iconSecurityGroup, 16, 16, ['typeIcon', 'icon-red', 'mr-2']);
			} else if (principal.type === 'LDAPGroup') {
				return _Icons.getSvgIcon(_Icons.iconSecurityLDAPGroup, 16, 16, ['typeIcon', 'icon-orange', 'mr-2']);
			} else {
				return _Icons.getSvgIcon(_Icons.iconSecurityGroup, 16, 16, ['typeIcon', 'mr-2']);
			}

		} else {

			if (principal.isAdmin) {

				if (principal.blocked) {
					return _Icons.getSvgIcon(_Icons.iconSecurityBlockedAdminUser, 16, 16, ['typeIcon', 'icon-red', 'mr-2']);
				} else {
					return _Icons.getSvgIcon(_Icons.iconSecurityAdminUser, 16, 16, ['typeIcon', 'icon-red', 'mr-2']);
				}

			} else if (principal.type === 'LDAPUser') {

				return _Icons.getSvgIcon(_Icons.iconSecurityLDAPUser, 16, 16, ['typeIcon', 'icon-orange', 'mr-2']);

			} else {

				if (principal.blocked) {
					return _Icons.getSvgIcon(_Icons.iconSecurityBlockedRegularUser, 16, 16, ['typeIcon', 'mr-2']);
				} else {
					return _Icons.getSvgIcon(_Icons.iconSecurityRegularUser, 16, 16, ['typeIcon', 'mr-2']);
				}
			}
		}
	},
	getIconForSchemaNodeType: (entity) => {

		let icon              = _Icons.iconSchemaNodeDefault;
		let additionalClasses = ['flex-shrink-0'];
		let title             = '';

		switch (entity.type) {

			case 'SchemaMethod':

				switch (entity.codeType) {

					case 'java':
						icon = _Icons.iconSchemaNodeSchemaMethod;
						additionalClasses.push('icon-red');
						title = 'Java method';
						break;

					default:
						additionalClasses.push('icon-blue');

						if (entity.isStatic) {

							icon = _Icons.iconSchemaNodeStaticMethod;
							title = 'Static method';

						} else {

							let isLifeCycleMethod = LifecycleMethods.isLifecycleMethod(entity);
							if (isLifeCycleMethod) {
								icon = _Icons.iconSchemaNodeLifecycleMethod;
								title = 'Lifecycle method';
							} else {
								icon = _Icons.iconSchemaNodeSchemaMethod;
								title = 'Custom method';
							}
						}
						break;
				}
				break;

			case 'SchemaProperty':
				icon = _Icons.getIconForSchemaPropertyType(entity.propertyType);
				break;

			case 'SchemaView':
				icon = _Icons.iconSchemaView;
				break;

			case 'SchemaRelationshipNode':
				icon = _Icons.iconSchemaRelationship;
				break;
		}

		return _Icons.getSvgIcon(icon, 16, 24, additionalClasses, title);
	},
	getIconForSchemaPropertyType: (propertyType) => {

		switch (propertyType) {

			case 'Custom':
			case "IdNotion":
			case "Notion":
				return _Icons.iconSchemaPropertyMagic;

			case "IntegerArray":
			case "StringArray":
			case "DateArray":
			case "EnumArray":
			case "LongArray":
			case "BooleanArray":
			case "DoubleArray":
				return _Icons.iconSchemaPropertyArray;

			case 'Integer':
			case "Long":
				return _Icons.iconSchemaPropertyNumeric;

			case 'Date':
			case 'ZonedDateTime':
				return _Icons.iconSchemaPropertyDate;

			case 'Boolean':      return _Icons.iconSchemaPropertyBoolean;
			case "Cypher":       return _Icons.iconSchemaPropertyCypher;
			case "Double":       return _Icons.iconSchemaPropertyDouble;
			case "Enum":         return _Icons.iconSchemaPropertyEnum;
			case "Function":     return _Icons.iconSchemaPropertyFunction;
			case 'String':       return _Icons.iconSchemaPropertyString;
			case 'Encrypted':    return _Icons.iconSchemaPropertyEncrypted;
			default:             return _Icons.iconSchemaRelationship;
		}

		return _Icons.iconSchemaPropertyString;
	},
	getSvgIconForContentNode: (content, initialClasses = [], title) => {

		let isComment    = (content.type === 'Comment');
		let isTemplate   = (content.type === 'Template');
		let isComponent  = content.sharedComponentId || (content.syncedNodesIds && content.syncedNodesIds.length);
		let isActiveNode = (typeof content.isActiveNode === "function") ? content.isActiveNode() : false;

		if (isComment) {

			return _Icons.getSvgIcon(_Icons.iconDOMCommentElement, 16, 16, ['icon-grey', ...initialClasses], title);

		} else if (isTemplate) {

			if (isComponent) {
				return _Icons.getSvgIcon(_Icons.iconDOMTemplateElement, 16, 16, ['icon-grey', 'fill-yellow', ...initialClasses], title);
			} else if (isActiveNode) {
				return _Icons.getSvgIcon(_Icons.iconDOMTemplateElement, 16, 16, ['icon-grey', 'fill-yellow', ...initialClasses], content.getActiveNodeInfoAsString());
			} else {
				return _Icons.getSvgIcon(_Icons.iconDOMTemplateElement, 16, 16, ['icon-grey', 'fill-transparent', ...initialClasses], title);
			}

		} else {

			if (isComponent) {
				return _Icons.getSvgIcon(_Icons.iconDOMContentElement, 16, 16, ['icon-grey', 'fill-yellow', ...initialClasses], title);
			} else if (isActiveNode) {
				return _Icons.getSvgIcon(_Icons.iconDOMContentElement, 16, 16, ['icon-grey', 'fill-yellow', ...initialClasses], title);
			} else {
				return _Icons.getSvgIcon(_Icons.iconDOMContentElement, 16, 16, ['icon-grey', 'fill-transparent', ...initialClasses], title);
			}
		}
	},
	getSvgIconForElementNode: (element, initialClasses = [], title) => {
		let isComponent  = element.sharedComponentId || (element.syncedNodesIds && element.syncedNodesIds.length);
		let isActiveNode = (typeof element.isActiveNode === "function") ? element.isActiveNode() : false;

		if (isActiveNode) {

			return _Icons.getSvgIcon(_Icons.iconDOMTreeActiveElement, 16, 16, ['typeIcon', ...initialClasses], element.getActiveNodeInfoAsString());

		} else if (isComponent) {

			return _Icons.getSvgIcon(_Icons.iconDOMTreeElement, 16, 16, ['typeIcon', 'icon-grey', 'fill-yellow', ...initialClasses], title);

		} else {

			return _Icons.getSvgIcon(_Icons.iconDOMTreeElement, 16, 16, ['typeIcon', 'icon-grey', 'fill-transparent', ...initialClasses], title);
		}
	},
	getSvgIconForMessageClass: (messageClass) => {

		switch (messageClass) {
			case MessageBuilder.types.warning:
				return _Icons.iconWarning;
			case MessageBuilder.types.error:
				return _Icons.iconCrossIcon;
			case MessageBuilder.types.success:
				return _Icons.iconCheckmarkBold;
			case MessageBuilder.types.info:
				return _Icons.iconInfo;
		}

		return _Icons.iconCrossIcon;
	}
};