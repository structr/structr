var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

var animating = animating || undefined;

(function() {
	'use strict';

	var _s, _callbacks, _onActivate , _onDeactivate, _onNewSelectionGroup, _onDeleteSelectionGroup, _onClearSelectionGroup, _onToggleLasso;
	var _hasDragged = false;
	var _timeout = 0;
	var _activeStateSelections = [];

	Graphbrowser.Modules.SelectionTools = function(sigmaInstance, callbacks){
		var self = this;
		_s = sigmaInstance;
		_callbacks = callbacks;
		_hasDragged = false;
		self.name = 'selectionTools';
		self.isActive = false;
	};

	Graphbrowser.Modules.SelectionTools.prototype.types = ["restoreListener"];

	Graphbrowser.Modules.SelectionTools.prototype.init = function(settings) {
		var self = this;

		if(settings){
			settings.onActivate !== undefined ? _onActivate = settings.onActivate : _onActivate = undefined;
			settings.onDeactivate !== undefined ? _onDeactivate = settings.onDeactivate : _onDeactivate = undefined;
			settings.onNewSelectionGroup !== undefined ? _onNewSelectionGroup = settings.onNewSelectionGroup : _onNewSelectionGroup = undefined;
			settings.onDeleteSelectionGroup !== undefined ? _onDeleteSelectionGroup = settings.onDeleteSelectionGroup : _onDeleteSelectionGroup = undefined;
			settings.onClearSelectionGroup !== undefined ? _onClearSelectionGroup = settings.onClearSelectionGroup : _onClearSelectionGroup = undefined;
			settings.onToggleLasso !== undefined ? _onToggleLasso = settings.onToggleLasso : _onToggleLasso = undefined;
		}		
		else
			throw new Error('"Graph-Browser-SelectionTools: No settings found!"');

		$(document).on('click', '#selectiontools-controlElement',  function() {
			if(self.isActive){
				if(_onDeactivate !== undefined)
					_onDeactivate();
			}
			else{
				if(_onActivate !== undefined)
					_onActivate();				
			}	
		});

		$(document).on('click', '#selectiontools-btnCreateNewSelectionGroup',  function(){
			self.createNewSelectionGroup();
			if(_onNewSelectionGroup !== undefined)
				_onNewSelectionGroup();
		});

		$(document).on('click', '#selectiontools-btnDeleteSelectedSelectionGroup', function(){
			self.deleteSelection();
			if(_onDeleteSelectionGroup !== undefined)
				_onDeleteSelectionGroup();
		});

		$(document).on('click', '#selectiontools-btnToggleLasso', function(){
			self.toggleLasso();
			if(_onToggleLasso !== undefined)
				_onToggleLasso();
		});

		$(document).on('click', '#selectiontools-btnClearSelectedSelection', function(){
			self.onClearSelectionGroup();
			if(_onClearSelectionGroup !== undefined)
				_onClearSelectionGroup();
		});
	};

	Graphbrowser.Modules.SelectionTools.prototype.activate = function() {
		var self = this;
		self.isActive = true;

		_s.renderers[0].bind('render', function(e) {
			_s.renderers[0].halo({
				nodes: _callbacks.sigmaPlugins.activeState.nodes()
			});
		});

		_callbacks.sigmaPlugins.lasso.bind('selectedNodes', function (event) {
			window.clearTimeout(_timeout);
			_timeout = setTimeout(function() {				
				var sw = $('input[name="selectionButtons"]:checked').val();
				if(sw === undefined)
					return;
				var selectedView = sw.split('.');
				
				if(_activeStateSelections[selectedView[0]].hidden){
					self.onHideSelectionGroup(selectedView[0]);
				}
				if(_activeStateSelections[selectedView[0]].fixed){
					self.setSelectionFixed(selectedView[0]);
				}

				if(event.data.length <= 0)
					return;

				_activeStateSelections[selectedView[0]].nodes = event.data;
				
				self.toggleLasso();
				self.updateGroupRadioButtons();
				_callbacks.refreshSigma(false, false);
				_timeout = 0;
			}, 10);
		});

		self.updateActiveState();
		_callbacks.refreshSigma(false, false);
	};

	Graphbrowser.Modules.SelectionTools.prototype.deactivate = function() {
		var self = this;
		self.isActive = false;

		if(_callbacks.sigmaPlugins.lasso.isActive){
			self.toggleLasso();
		}

		_s.renderers[0].unbind('render');
		_callbacks.sigmaPlugins.activeState.dropNodes();
		_callbacks.refreshSigma(true);
	};

	Graphbrowser.Modules.SelectionTools.prototype.onHideSelectionGroup = function(group, status) {	
		if(status === undefined)
			status = !_activeStateSelections[group].hidden;
		_activeStateSelections[group].hidden = status;
		_callbacks.hideNodes(_activeStateSelections[group].nodes, status);
	};

	Graphbrowser.Modules.SelectionTools.prototype.setSelectionFixed = function(group, status) {	
		if(status === undefined)
			status = !_activeStateSelections[group].fixed;
		_activeStateSelections[group].fixed = status;
		$.each(_activeStateSelections[group].nodes, function(i, node){
			$.each(_s.graph.nodes(), function(key, gnode){
				if(node.id === gnode.id){
					gnode.fixed = _activeStateSelections[group].fixed;
					node.fixed = _activeStateSelections[group].fixed;
				}
			});	
		});
	};

	Graphbrowser.Modules.SelectionTools.prototype.onClearSelectionGroup = function() {
		var self = this;
		var selectedView = $('input[name="selectionButtons"]:checked').val();
		if(selectedView === undefined)
			return;
		selectedView = selectedView.split('.');
		
		self.onHideSelectionGroup(selectedView[0], false);
		self.setSelectionFixed(selectedView[0], false);
		_callbacks.sigmaPlugins.activeState.dropNodes();	
		_activeStateSelections[selectedView[0]].nodes = undefined;
		self.updateGroupRadioButtons();
		_callbacks.refreshSigma(false, false);
	};

	Graphbrowser.Modules.SelectionTools.prototype.onGroupSelectionChanged = function() {
		var self = this;
		if(self.isActive === false){
			return;
		}
		var selectedView = $('input[name="selectionButtons"]:checked').val();
		selectedView = selectedView.split('.');

		if(selectedView === undefined || _activeStateSelections[selectedView[0]].nodes === undefined){
			_callbacks.sigmaPlugins.activeState.dropNodes();
			_callbacks.refreshSigma(false, false)
			return;
		}

		self.updateActiveState();
		_callbacks.refreshSigma(false, false)
	};

	Graphbrowser.Modules.SelectionTools.prototype.updateActiveState = function(data) {
		if(!self.isActive)
			return;
		var selectedView = $('input[name="selectionButtons"]:checked').val();
		if(selectedView === undefined)
			return;
		selectedView = selectedView.split('.');

		if(_activeStateSelections[selectedView[0]].nodes === undefined){
			return;
		}

		var an = [];
		$.each(_activeStateSelections[selectedView[0]].nodes, function(i, node) {
			an.push(node.id);
		});
		_callbacks.sigmaPlugins.activeState.dropNodes();
		_callbacks.sigmaPlugins.activeState.addNodes(an);
		_callbacks.refreshSigma(false, false);
	};


	Graphbrowser.Modules.SelectionTools.prototype.toggleLasso = function() {
		if(!_callbacks.sigmaPlugins.lasso.isActive){
			if(animating){
				return;
			}
			$('#selectiontools-btnToggleLasso').toggleClass('btn-danger btn-info');
			_callbacks.sigmaPlugins.lasso.activate();
		}
		else{
			$('#selectiontools-btnToggleLasso').toggleClass('btn-info btn-danger');
			_callbacks.sigmaPlugins.lasso.deactivate();
		}
	};


	Graphbrowser.Modules.SelectionTools.prototype.updateGroupRadioButtons = function() {
		var self = this;
		$('#selectiontools-selectionTable-groupSelectionItems').empty();

		//<div class="radio"></div>

		$.each(_activeStateSelections, function(key, selection){					
			$('#selectiontools-selectionTable-groupSelectionItems').append(
				'<tr>'
				+ '<td class="selectiontools-selectionTable-selectionGroup-radioButton"><label><input type="radio"  name="selectionButtons" value="' + key +  '.' + selection.id + '"><span id="selectiontools-selectionTable-groupSelectionItems-item-' + selection.id + '">&nbsp' + selection.name + '</span></label></td>'
				+ '<td class="selectiontools-selectionTable-selectionGroup-fixate"><label><input type="checkbox" name="selectiontools-selectionTable-selectionFixed" value="' + key +  '.' + selection.id + '"></label></td>'	
				+ '<td class="selectiontools-selectionTable-selectionGroup-hide"><label><input type="checkbox" name="selectiontools-selectionTable-selectionHidden" value="' + key +  '.' + selection.id + '"></label></td>'	
				+ '</tr>'
				);

			if(_activeStateSelections[key].hidden)
				$('input[name="selectiontools-selectionTable-selectionHidden"][value="' + key +  '.' + selection.id +'"]').prop('checked', true);			

			if(_activeStateSelections[key].fixed)
				$('input[name="selectiontools-selectionTable-selectionFixed"][value="' + key +  '.' + selection.id +'"]').prop('checked', true);			

			$('input[name="selectiontools-selectionTable-selectionHidden"][value="' + key +  '.' + selection.id +'"]').change(function(){
				self.onHideSelectionGroup(key);
			});

			$('input[name="selectiontools-selectionTable-selectionFixed"][value="' + key +  '.' + selection.id +'"]').change(function(){
				self.setSelectionFixed(key);
			});

			$('#selectiontools-selectionTable-groupSelectionItems-item-' + selection.id).bind('dblclick', function(){self.changeBtnLabelForRename(selection, self, _activeStateSelections[key].hidden)});
		});
		
		$('input[name="selectionButtons"]').change(function(){
			self.onGroupSelectionChanged();
		});
	};

	Graphbrowser.Modules.SelectionTools.prototype.changeBtnLabelForRename = function(selection, context, hidden) {
		$('#selectiontools-selectionTable-groupSelectionItems-item-' + selection.id).replaceWith('<input size="10" placeholder="' + selection.name + '" id="input-id' + selection.id + '"value="' + selection.name + '"></input>');

		$('#input-id' + selection.id).bind("enterKey",function(e){
			selection.name = $(this).val();
			$(this).replaceWith('<span id="selectiontools-selectionTable-groupSelectionItems-item-' + selection.id + '">&nbsp' + selection.name + '</span>');
			$('#selectiontools-selectionTable-groupSelectionItems-item-' + selection.id).bind('dblclick', function(){context.changeBtnLabelForRename(selection, context, hidden)});
		}).keyup(function(e){
			if(e.keyCode === 13)
				$(this).trigger("enterKey");			
		});
	};

	Graphbrowser.Modules.SelectionTools.prototype.clearSelectionControl = function() {
		$('.').remove();
	};

	Graphbrowser.Modules.SelectionTools.prototype.createNewSelectionGroup = function(setSelected) {
		var self = this;

		var newId = (parseInt(Math.random() * (5000 - 1))).toString(); 
		_activeStateSelections.push({id: newId, name: 'Auswahl'+ (_activeStateSelections.length + 1), nodes: [], hidden: false, fixed: false});
		self.updateGroupRadioButtons();
		
		$('input[name="selectionButtons"]').each(function (){
			var val = $(this).val().split('.');
			if(val[1] === newId){
				$(this).prop('checked', true);
				return false;
			}
		});
		self.updateActiveState();		
	};

	Graphbrowser.Modules.SelectionTools.prototype.deleteSelection = function() {
		var self = this;
		var selectedView = $('input[name="selectionButtons"]:checked').val();

		if(selectedView === undefined)
			return;
		selectedView = selectedView.split('.');
		_activeStateSelections.splice(selectedView[0], 1);
		self.updateGroupRadioButtons();
		_callbacks.sigmaPlugins.activeState.dropNodes();
		_callbacks.refreshSigma(false, false);
	};

	Graphbrowser.Modules.SelectionTools.prototype.onRestore = function() {
		var self = this;
		_activeStateSelections = [];
		self.createNewSelectionGroup();
		if(self.isActive){
			_callbacks.sigmaPlugins.activeState.dropNodes();
			self.updateActiveState();
		}
	};
	
}).call(window);