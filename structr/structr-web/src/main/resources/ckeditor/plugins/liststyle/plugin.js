/*
Copyright (c) 2003-2010, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.html or http://ckeditor.com/license
*/

(function(){CKEDITOR.plugins.liststyle={init:function(a){a.addCommand('numberedListStyle',new CKEDITOR.dialogCommand('numberedListStyle'));CKEDITOR.dialog.add('numberedListStyle',this.path+'dialogs/liststyle.js');a.addCommand('bulletedListStyle',new CKEDITOR.dialogCommand('bulletedListStyle'));CKEDITOR.dialog.add('bulletedListStyle',this.path+'dialogs/liststyle.js');a.addMenuGroup('list',108);if(a.addMenuItems)a.addMenuItems({numberedlist:{label:a.lang.list.numberedTitle,group:'list',command:'numberedListStyle'},bulletedlist:{label:a.lang.list.bulletedTitle,group:'list',command:'bulletedListStyle'}});if(a.contextMenu)a.contextMenu.addListener(function(b,c){if(!b)return null;if(b.getAscendant('ol'))return{numberedlist:CKEDITOR.TRISTATE_OFF};if(b.getAscendant('ul'))return{bulletedlist:CKEDITOR.TRISTATE_OFF};});}};CKEDITOR.plugins.add('liststyle',CKEDITOR.plugins.liststyle);})();
