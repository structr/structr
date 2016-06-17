var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _onFinished, _nodeList, _parentNode, _parentTitle, _callbackOnFinished, _callbackOnChooseNodes;

	Graphbrowser.Modules.NewNodesPicker = function(sigmaInstance){
		var self = this;
		self.name = 'newNodePicker';
		_s = sigmaInstance;
	}

	Graphbrowser.Modules.NewNodesPicker.prototype.types= ['nodePicker'];

	Graphbrowser.Modules.NewNodesPicker.prototype.init = function(settings){
		var self = this;

		if(settings){
			(settings.onFinished !== undefined && typeof settings.onFinished === 'function') ? _callbackOnFinished = settings.onFinished : _callbackOnFinished = undefined;
			(settings.onChooseNodes !== undefined && typeof settings.onChooseNodes === 'function') ? _callbackOnChooseNodes = settings.onChooseNodes : _callbackOnChooseNodes = undefined;
				
		}
		else
			throw new Error("Graph-Browser-NewNodePicker: No settings found");
		
		$(document).on('click', '#newnodepicker-chooseNodes-markAllSelected', function(){
			self.onMarkAllSelected();
		});

		$(document).on('click', '#newnodepicker-chooseNodes-reverseSelection', function(){
			self.onReverseSelection();
		});

		$(document).on('click', '#newnodepicker-chooseNodes-submit', function(){
			self.onSelected();
		});
	};


	Graphbrowser.Modules.NewNodesPicker.prototype.pickNodes = function(nodeList, onFinished, parentNode, parentTitle){
		var self = this;
		if(nodeList.length <= 0){
			return;
		}

		_nodeList = nodeList;
		_onFinished = onFinished;
		_parentNode = parentNode;
		_parentTitle = parentTitle;
		var nodeTypes = [];
		
		if(parentTitle){
			$('#newnodepicker-chooseNodes-parentNodeTitle').replaceWith('<h3 id="newnodepicker-chooseNodes-parentNodeTitle" class="newnodepicker-chooseNodes-parentNodeTitle">' + parentTitle + '</h3>');
		}

		$('#newnodepicker-chooseNodes-items').empty();

		$.each(_nodeList, function(i, node){
			if(nodeTypes.indexOf(node.nodeType) < 0){
				nodeTypes.push(node.nodeType);
				$('#newnodepicker-chooseNodes-items').append('<div id="newnodepicker-chooseNodes-itemType-'+ node.nodeType + '" class="newnodepicker-chooseNodes-itemType newnodepicker-chooseNodes-itemType-'+ node.nodeType + '">'
				 + '<h4 id="newnodepicker-chooseNodes-itemType-header-'+ node.nodeType + '" class="newnodepicker-chooseNodes-itemType-header newnodepicker-chooseNodes-itemType-header-' + node.nodeType + '">' + node.nodeType + '</h4></div>'
				);

				$('#newnodepicker-chooseNodes-itemType-header-' + node.nodeType).on('click', function(){
					self.onMarkTypeSelected(node.nodeType);
				});
			}
			var alreadyInTheList = false;
			$("input[name='selectedNodeItems[]']").each(function (){
				if($(this).val() === node.id){
					alreadyInTheList = true;
				}
			});

			if(alreadyInTheList)
				return true;

			else if(_s.graph.nodes(node.id)){
				$('#newnodepicker-chooseNodes-itemType-'+ node.nodeType).append('<div class="checkbox newnodepicker-chooseNodes-item newnodepicker-chooseNodes-item-'+ node.nodeType + '"><label class="newnodepicker-chooseNodes-item-label"><input type="checkbox" disabled name="selectedNodeItems[]" value="' + node.nodeType + '.' + node.id +  '">' + node.label + '</label></div>');
			}

			else if(!alreadyInTheList && !_s.graph.nodes(node.id)){
				$('#newnodepicker-chooseNodes-itemType-'+ node.nodeType).append('<div class="checkbox newnodepicker-chooseNodes-item newnodepicker-chooseNodes-item-'+ node.nodeType + '"><label><input type="checkbox" name="selectedNodeItems[]" value="' + node.nodeType + '.' + node.id +  '">' + node.label + '</label></div>');
			}
		});

		if(_callbackOnChooseNodes != undefined)
			_callbackOnChooseNodes(_parentNode, _parentTitle);
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.onSelected = function(){
		var self = this;
		$("input[name='selectedNodeItems[]']:not(:checked)").each(function (){
			var val = $(this).val().split('.');
			_nodeList = _nodeList.filter(function (el) {
				return el.id !== val[1];
			});
		});

		if(_callbackOnFinished !== undefined)
			_callbackOnFinished(_parentNode, _parentTitle);
		if(_onFinished !== undefined)
			_onFinished(_nodeList, _parentNode);
		self.clear();
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.onMarkAllSelected = function(){
		$("input[name='selectedNodeItems[]']").each(function (){
			if($(this).prop('disabled') === true)
				return true;
			$(this).prop('checked', true);
		});
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.onMarkTypeSelected = function(type){
		$("input[name='selectedNodeItems[]']").each(function (){
			var val = $(this).val().split('.');
			if($(this).prop('disabled') === true)
				return true;
			if(val[0] === type){
				$(this).prop('checked', !$(this).prop('checked'));
			}
		});
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.onReverseSelection = function(){
		$("input[name='selectedNodeItems[]']").each(function (){
			if($(this).prop('disabled') === true)
				return true;
			$(this).prop('checked', !$(this).prop('checked'));
		});
	};

	Graphbrowser.Modules.NewNodesPicker.prototype.clear = function(){
		_onFinished = undefined;
		_nodeList = undefined;
		_parentNode = undefined;
		_parentTitle = undefined;
	};

}).call(window);