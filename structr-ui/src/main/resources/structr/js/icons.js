/*
 * Copyright (C) 2010-2022 Structr GmbH
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
let _Icons = {

	preloadSVGIcons: () => {
		// this should be super fast because we inserted a preload rule to the index.html
		fetch('icon/sprites.svg').then(response => {
			return response.text();
		}).then(svgPreload => {
			document.body.insertAdjacentHTML('afterbegin', svgPreload);
		});
	},

	application_form_add: 'icon/application_form_add.png',
	add_icon: 'icon/add.png',
	delete_icon: 'icon/delete.png',
	delete_disabled_icon: 'icon/delete_gray.png',
	add_brick_icon: 'icon/brick_add.png',
	delete_brick_icon: 'icon/brick_delete.png',
	edit_icon: 'icon/pencil.png',
	wrench_icon: 'icon/wrench.png',
	collapsed_icon: 'icon/tree_arrow_right.png',
	expanded_icon: 'icon/tree_arrow_down.png',
	link_icon: 'icon/link.png',
	key_icon: 'icon/key.png',
	key_add_icon: 'icon/key_add.png',
	cross_icon: 'icon/cross.png',
	tick_icon: 'icon/tick.png',
	grey_cross_icon: 'icon/cross_small_grey.png',
	page_white_stack_icon: 'icon/page_white_stack.png',
	eye_icon: 'icon/eye.png',
	database_icon: 'icon/database.png',
	database_table_icon: 'icon/database_table.png',
	database_add_icon: 'icon/database_add.png',
	database_gear_icon: 'icon/database_gear.png',
	view_detail_icon: 'icon/application_view_detail.png',
	calendar_icon: 'icon/calendar.png',
	add_grey_icon: 'icon/add_grey.png',
	page_icon: 'icon/page.png',
	page_white_icon: 'icon/page_white.png',
	button_icon: 'icon/button.png',
	clone_icon: 'icon/page_copy.png',
	group_icon: 'icon/group.png',
	group_add_icon: 'icon/group_add.png',
	group_link_icon: 'icon/group_link.png',
	user_icon: 'icon/user.png',
	user_gray_icon: 'icon/user_gray.png',
	user_green_icon: 'icon/user_green.png',
	user_orange_icon: 'icon/user_orange.png',
	user_red_icon: 'icon/user_red.png',
	user_suit_icon: 'icon/user_suit.png',
	user_add_icon: 'icon/user_add.png',
	user_delete_icon: 'icon/user_delete.png',
	compress_icon: 'icon/compress.png',
	accept_icon: 'icon/accept.png',
	push_file_icon: 'icon/page_white_get.png',
	pull_file_icon: 'icon/page_white_put.png',
	exec_icon: 'icon/control_play_blue.png',
	exec_blue_icon: 'icon/control_play.png',
	arrow_undo_icon: 'icon/arrow_undo.png',
	information_icon: 'icon/information.png',
	help_icon: 'icon/help.png',
	refresh_icon: 'icon/arrow_refresh.png',
	error_icon: 'icon/error.png',
	exclamation_icon: 'icon/exclamation.png',
	pull_page_icon: 'icon/pull_page.png',
	wand_icon: 'icon/wand.png',
	toggle_icon: 'icon/arrow_switch.png',
	widget_icon: 'icon/layout.png',
	folder_icon: 'icon/folder.png',
	add_folder_icon: 'icon/folder_add.png',
	delete_folder_icon: 'icon/folder_delete.png',
	add_widget_icon: 'icon/layout_add.png',
	content_icon: 'icon/page_white.png',
	active_content_icon: 'icon/page_yellow.png',
	delete_content_icon: 'icon/page_white_delete.png',
	template_icon: 'icon/layout_content.png',
	comp_templ_icon: 'icon/layout_yellow.png',
	active_template_icon: 'icon/layout_yellow.png',
	icon_shared_template: 'icon/layout_yellow.png',
	comment_icon: 'icon/comment.png',
	repeater_icon: 'icon/bricks.png',
	brick_icon: 'icon/brick.png',
	comp_icon: 'icon/brick_yellow.png',
	microphone_icon: 'icon/icon_microphone.png',
	add_file_icon: 'icon/page_white_add.png',
	add_site_icon: 'icon/page_white_add.png',
	add_page_icon: 'icon/page_add.png',
	structr_logo_small: 'icon/structr_icon_16x16.png',
	minification_dialog_js_icon: 'icon/script_lightning.png',
	minification_dialog_css_icon: 'icon/script_palette.png',
	minification_trigger_icon: 'icon/briefcase.png',
	search_icon: 'icon/zoom.png',
	star_icon: 'icon/star.png',
	star_delete_icon: 'icon/star_delete.png',
	image_icon: 'icon/image.png',
	arrow_up_down: 'icon/arrow_up_down.png',
	floppy_icon: 'icon/disk.png',
	book_icon: 'icon/book_open.png',
	edition_community_icon: 'icon/tux.png',
	import_icon: 'icon/table_lightning.png',
	hamburger_icon: 'icon/hamburger_white.png',
	connect_icon: 'icon/connect.png',
	disconnect_icon: 'icon/disconnect.png',
	folder_connect_icon: 'icon/folder_connect.png',
	folder_disconnect_icon: 'icon/folder_disconnect.png',
	database_error_icon: 'icon/database_error.png',
	package_icon: 'icon/package.png',
	report_icon: 'icon/report.png',
	find_icon: 'icon/find.png',
	world_icon: 'icon/world.png',
	clock_icon: 'icon/clock.png',
	folder_star_icon: 'icon/folder_star.png',

	collapsedClass: 'svg-collapsed',
	expandedClass: 'svg-expanded',

	jstree_fake_icon: 'this-is-empty',

	getSvgIcon: (id, width = 16, height = 16, optionalClasses, title = '') => {

		/**
		 * SVG Icons are all loaded in index.html and can be used anywhere using this function
		 **/
		let classString = '';

		if (Array.isArray(optionalClasses)) {
			classString = optionalClasses.join(' ');
		} else if (typeof optionalClasses === 'string') {
			classString = optionalClasses;
		} else {
			// ignore
		}

		return `<svg width="${width}" height="${height}" class="${classString}">
			<title>${title}</title>
			<use xlink:href="#${id}"></use>
		</svg>`;
	},

	updateSvgIconInElement: (element, from, to) => {

		let use = element.querySelector(`use[*|href="#${from}"]`);

		if (!use) {
			return false;
		}

		use.setAttribute('xlink:href', '#' + to);
		return true;
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

	getFullSpriteClass: (key) => {

		return 'sprite ' + _Icons.getSpriteClassOnly(key);
	},

	updateSpriteClassTo: (el, newSpriteClass) => {

		el.classList.remove(...[...el.classList].filter((c) => c.startsWith('sprite-')));
		el.classList.add(newSpriteClass);
	},
	getSpriteClassOnly: function (key) {

		switch (key) {
			case _Icons.application_form_add:         return 'sprite-application_form_add';
			case _Icons.add_icon:                     return 'sprite-add';
			case _Icons.delete_icon:                  return 'sprite-delete';
			case _Icons.delete_disabled_icon:         return 'sprite-delete_gray';
			case _Icons.add_brick_icon:               return 'sprite-brick_add';
			case _Icons.delete_brick_icon:            return 'sprite-brick_delete';
			case _Icons.edit_icon:                    return 'sprite-pencil';
			case _Icons.wrench_icon:                  return 'sprite-wrench';
			case _Icons.collapsed_icon:               return 'sprite-tree_arrow_right';
			case _Icons.expanded_icon:                return 'sprite-tree_arrow_down';
			case _Icons.link_icon:                    return 'sprite-link';
			case _Icons.key_icon:                     return 'sprite-key';
			case _Icons.key_add_icon:                 return 'sprite-key_add';
			case _Icons.cross_icon:                   return 'sprite-cross';
			case _Icons.tick_icon:                    return 'sprite-tick';
			case _Icons.grey_cross_icon:              return 'sprite-cross_small_grey';
			case _Icons.page_white_stack_icon:        return 'sprite-page_white_stack';
			case _Icons.eye_icon:                     return 'sprite-eye';
			case _Icons.database_icon:                return 'sprite-database';
			case _Icons.database_table_icon:          return 'sprite-database_table';
			case _Icons.database_add_icon:            return 'sprite-database_add';
			case _Icons.view_detail_icon:             return 'sprite-application_view_detail';
			case _Icons.calendar_icon:                return 'sprite-calendar';
			case _Icons.add_grey_icon:                return 'sprite-add_grey';
			case _Icons.page_icon:                    return 'sprite-page';
			case _Icons.page_white_icon:              return 'sprite-page_white';
			case _Icons.button_icon:                  return 'sprite-button';
			case _Icons.clone_icon:                   return 'sprite-page_copy';
			case _Icons.group_icon:                   return 'sprite-group';
			case _Icons.group_add_icon:               return 'sprite-group_add';
			case _Icons.group_link_icon:              return 'sprite-group_link';
			case _Icons.user_icon:                    return 'sprite-user';
			case _Icons.user_gray_icon:               return 'sprite-user_gray';
			case _Icons.user_green_icon:              return 'sprite-user_green';
			case _Icons.user_orange_icon:             return 'sprite-user_orange';
			case _Icons.user_red_icon:                return 'sprite-user_red';
			case _Icons.user_suit_icon:               return 'sprite-user_suit';
			case _Icons.user_add_icon:                return 'sprite-user_add';
			case _Icons.user_delete_icon:             return 'sprite-user_delete';
			case _Icons.compress_icon:                return 'sprite-compress';
			case _Icons.accept_icon:                  return 'sprite-accept';
			case _Icons.push_file_icon:               return 'sprite-page_white_get';
			case _Icons.pull_file_icon:               return 'sprite-page_white_put';
			case _Icons.exec_blue_icon:               return 'sprite-control_play_blue';
			case _Icons.exec_icon:                    return 'sprite-control_play';
			case _Icons.arrow_undo_icon:              return 'sprite-arrow_undo';
			case _Icons.information_icon:             return 'sprite-information';
			case _Icons.help_icon:                    return 'sprite-help';
			case _Icons.refresh_icon:                 return 'sprite-arrow_refresh';
			case _Icons.error_icon:                   return 'sprite-error';
			case _Icons.exclamation_icon:             return 'sprite-exclamation';
			case _Icons.pull_page_icon:               return 'sprite-pull_page';
			case _Icons.wand_icon:                    return 'sprite-wand';
			case _Icons.toggle_icon:                  return 'sprite-arrow_switch';
			case _Icons.widget_icon:                  return 'sprite-layout';
			case _Icons.folder_icon:                  return 'sprite-folder';
			case _Icons.add_folder_icon:              return 'sprite-folder_add';
			case _Icons.add_widget_icon:              return 'sprite-layout_add';
			case _Icons.content_icon:                 return 'sprite-page_white';
			case _Icons.active_content_icon:          return 'sprite-page_yellow';
			case _Icons.delete_content_icon:          return 'sprite-page_white_delete';
			case _Icons.template_icon:                return 'sprite-layout_content';
			case _Icons.active_template_icon:         return 'sprite-layout_yellow';
			case _Icons.icon_shared_template:         return 'sprite-layout_yellow';
			case _Icons.comment_icon:                 return 'sprite-comment';
			case _Icons.repeater_icon:                return 'sprite-bricks';
			case _Icons.brick_icon:                   return 'sprite-brick';
			case _Icons.comp_icon:                    return 'sprite-brick_yellow';
			case _Icons.microphone_icon:              return 'sprite-icon_microphone';
			case _Icons.add_file_icon:                return 'sprite-page_white_add';
			case _Icons.add_site_icon:                return 'sprite-page_white_add';
			case _Icons.add_page_icon:                return 'sprite-page_add';
			case _Icons.structr_logo_small:           return 'sprite-structr_icon_16x16';
			case _Icons.minification_dialog_js_icon:  return 'sprite-script_lightning';
			case _Icons.minification_dialog_css_icon: return 'sprite-script_palette';
			case _Icons.minification_trigger_icon:    return 'sprite-briefcase';
			case _Icons.search_icon:                  return 'sprite-zoom';
			case _Icons.star_icon:                    return 'sprite-star';
			case _Icons.star_delete_icon:             return 'sprite-star_delete';
			case _Icons.image_icon:                   return 'sprite-image';
			case _Icons.arrow_up_down:                return 'sprite-arrow_up_down';
			case _Icons.floppy_icon:                  return 'sprite-disk';
			case _Icons.book_icon:                    return 'sprite-book_open';
			case _Icons.edition_community_icon:       return 'sprite-tux';
			case _Icons.import_icon:                  return 'sprite-table_lightning';
			case _Icons.hamburger_icon:               return 'sprite-hamburger_white';
			case _Icons.connect_icon:                 return 'sprite-connect';
			case _Icons.disconnect_icon:              return 'sprite-disconnect';
			case _Icons.folder_connect_icon:          return 'sprite-folder_connect';
			case _Icons.folder_disconnect_icon:       return 'sprite-folder_disconnect';
			case _Icons.database_error_icon:          return 'sprite-database_error';
			case _Icons.package_icon:                 return 'sprite-package';
			case _Icons.report_icon:                  return 'sprite-report';
			case _Icons.find_icon:                    return 'sprite-find';
			case _Icons.world_icon:                   return 'sprite-world';
			case _Icons.clock_icon:                   return 'sprite-clock';
			case _Icons.folder_star_icon:             return 'sprite-folder_star';
			case _Icons.delete_folder_icon:           return 'sprite-folder_delete';

			default:                                  return 'sprite-error';
		}

	},
	getImageOrIcon: (image) => {
		if (image.contentType && image.contentType.startsWith('image/svg')) {
			return _Icons.getImageMarkup(image.path);
		}

		if (image.tnSmall) {
			return _Icons.getImageMarkup(image.tnSmall.path);
		}

		return _Icons.getSvgIcon('file-image');
	},
	getImageMarkup: function (path) {
		return '<img class="icon" src="' + path + '">';
	},
	getMinificationIcon: function(file) {
		switch(file.type) {
			case 'MinifiedCssFile':
				return _Icons.minification_dialog_css_icon;
			case 'MinifiedJavaScriptFile':
				return _Icons.minification_dialog_js_icon;
			default:
				return _Icons.error_icon;
		}
	},
	getFileIconSVG: (file) => {

		// let fileName    = file.name;
		let contentType = file.contentType;
		let result      = 'file-empty';

		if (contentType) {

			switch (contentType) {

				case 'application/pdf':
				case 'application/postscript':
					result = 'file-pdf';
					break;

				case 'application/x-pem-key':
				case 'application/pkix-cert+pem':
				case 'application/x-iwork-keynote-sffkey':
					result = 'file-certificate';
					break;

				case 'application/octet-stream':
					result = 'file-terminal';
					break;

				case 'application/x-shellscript':
				case 'application/javascript':
				case 'application/xml':
				case 'text/html':
				case 'text/xml':
					result = 'file-code';
					break;

				case 'application/java-archive':
				case 'application/zip':
				case 'application/rar':
				case 'application/x-bzip':
					result = 'file-archive';
					break;

				case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
				case 'application/vnd.oasis.opendocument.text':
				case 'application/msword':
					result = 'file-word';
					break;

				case 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet':
				case 'application/vnd.oasis.opendocument.spreadsheet':
				case 'application/vnd.ms-excel':
					result = 'file-excel';
					break;

				case 'application/vnd.openxmlformats-officedocument.presentationml.presentation':
				case 'application/vnd.oasis.opendocument.chart':
					result = 'file-presentation';
					break;

				default:
					if (contentType.startsWith('image/')) {
						result = 'file-image';
					} else if (contentType.startsWith('text/')) {
						result = 'file-text';
					}
			}

			// if (fileName && fileName.contains('.')) {
			//
			// 	let fileExtensionPosition = fileName.lastIndexOf('.') + 1;
			// 	let fileExtension = fileName.substring(fileExtensionPosition);
			//
			// 	// add file extension css class to control colors
			// 	result = fileExtension + ' ' + result;
			// }
		}

		return _Icons.getSvgIcon(result);
	},

	getIconForEdition: (edition) => {
		switch (edition) {
			case 'Enterprise':
				return 'edition-enterprise';

			case 'Small Business':
				return 'edition-small-business';

			case 'Basic':
				return 'edition-basic';

			case 'Community':
			default:
				return 'edition-community';
		}
	},

	getHtmlForIcon: function (icon) {
		return '<i class="' + _Icons.getFullSpriteClass(icon) + '"></i>';
	}
};
