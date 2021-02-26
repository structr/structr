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
var _Icons = {
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
	active_template_icon: 'icon/layout_yellow.png',
	comp_templ_icon: 'icon/layout_yellow.png',
	icon_shared_template: 'icon/layout_yellow.png',
	comment_icon: 'icon/comment.png',
	repeater_icon: 'icon/bricks.png',
	brick_icon: 'icon/brick.png',
	comp_icon: 'icon/brick_yellow.png',
	microphone_icon: 'icon/icon_microphone.png',
	add_file_icon: 'icon/page_white_add.png',
	add_site_icon: 'icon/page_white_add.png',
	add_page_icon: 'icon/page_add.png',
	ajax_loader_1: 'img/ajax-loader.gif',
	ajax_loader_2: 'img/al.gif',
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
	edition_basic_icon: 'icon/medal_bronze_2.png',
	edition_small_business_icon: 'icon/medal_silver_2.png',
	edition_enterprise_icon: 'icon/medal_gold_2.png',
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


	getFullSpriteClass: function (key) {

		return 'sprite ' + _Icons.getSpriteClassOnly(key);

	},
	updateSpriteClassTo: function (el, newSpriteClass) {
		el.classList.forEach(function(cls) {
			if (cls.indexOf('sprite-') === 0) {
				el.classList.remove(cls);
			}
		});
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
			case _Icons.comp_templ_icon:              return 'sprite-layout_yellow';
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
			case _Icons.edition_basic_icon:           return 'sprite-medal_bronze_2';
			case _Icons.edition_small_business_icon:  return 'sprite-medal_silver_2';
			case _Icons.edition_enterprise_icon:      return 'sprite-medal_gold_2';
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
	getImageOrIcon: function(image) {
		return (image.contentType && image.contentType.startsWith('image/svg') ? _Icons.getImageMarkup(image.path) : (image.tnSmall ? _Icons.getImageMarkup(image.tnSmall.path) : '<i class="icon sprite sprite-image" />'));
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
	getFileIconClass: function(file) {

		var fileName = file.name;
		var contentType = file.contentType;

		var result = 'fa-file-o';

		if (contentType) {

			switch (contentType) {

				case 'text/plain':
					result = 'fa-file-text-o';
					break;

				case 'application/pdf':
				case 'application/postscript':
					result = 'fa-file-pdf-o';
					break;

				case 'application/x-pem-key':
				case 'application/pkix-cert+pem':
				case 'application/x-iwork-keynote-sffkey':
					result = 'fa-key';
					break;

				case 'application/x-trash':
					result = 'fa-trash-o';
					break;

				case 'application/octet-stream':
					result = 'fa-terminal';
					break;

				case 'application/x-shellscript':
				case 'application/javascript':
				case 'application/xml':
				case 'text/html':
				case 'text/xml':
					result = 'fa-file-code-o';
					break;

				case 'application/java-archive':
				case 'application/zip':
				case 'application/rar':
				case 'application/x-bzip':
					result = 'fa-file-archive-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
				case 'application/vnd.oasis.opendocument.text':
				case 'application/msword':
					result = 'fa-file-word-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet':
				case 'application/vnd.oasis.opendocument.spreadsheet':
				case 'application/vnd.ms-excel':
					result = 'fa-file-excel-o';
					break;

				case 'application/vnd.openxmlformats-officedocument.presentationml.presentation':
					result = 'fa-file-powerpoint-o';
					break;

				case 'image/jpeg':
					result = 'fa-picture-o';
					break;

				case 'application/vnd.oasis.opendocument.chart':
					result = 'fa-line-chart';
					break;

				default:
					if (contentType.startsWith('image/')) {
						result = 'fa-file-image-o';
					} else if (contentType.startsWith('text/')) {
						result = 'fa-file-text-o';
					}
			}

			if (fileName && fileName.contains('.')) {

				var fileExtensionPosition = fileName.lastIndexOf('.') + 1;
				var fileExtension = fileName.substring(fileExtensionPosition);

				// add file extension css class to control colors
				result = fileExtension + ' ' + result;
			}
		}

		return result;
	},

	getSpinnerImageAsData: function () {
		return "data:image/gif;base64,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";
	},

	getIconForEdition: function (edition) {
		switch (edition) {
			case 'Enterprise':
				return _Icons.edition_enterprise_icon;

			case 'Small Business':
				return _Icons.edition_small_business_icon;

			case 'Basic':
				return _Icons.edition_basic_icon;

			case 'Community':
			default:
				return _Icons.edition_community_icon;
		}
	},

	getHtmlForIcon: function (icon) {
		return '<i class="' + _Icons.getFullSpriteClass(icon) + '"></i>';
	}
};
