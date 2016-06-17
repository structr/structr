var Graphbrowser = Graphbrowser || {};
Graphbrowser.Modules = Graphbrowser.Modules || {};

(function() {
	'use strict';

	var _s, _callbacks, _callbackOnSuccess, _callbackOnError;

	Graphbrowser.Modules.Exporter = function (sigmaInstance, callbacks){
		var self = this;
		self.name = "exporter";
		_s = sigmaInstance;
		_callbacks = callbacks;
	};

	Graphbrowser.Modules.Exporter.prototype.types = ["saveHandler"];

	Graphbrowser.Modules.Exporter.prototype.init = function(settings){
		var self = this;

		if(settings){
			(settings.onSuccess !== undefined && typeof settings.onSuccess === 'function' ) ? _callbackOnSuccess = settings.onSuccess : _callbackOnSuccess = undefined;
			(settings.onError !== undefined && typeof settings.onError === 'function') ? _callbackOnError = settings.onError : _callbackOnError = undefined;
		}
		else
			throw new Error("Graph-Browser-Exporter: No settings found!");		

		$(document).on('click', '#exporter-saveGraph-form-submit',  function(){	
			self.triggerSave(true);
		});	
	};

	Graphbrowser.Modules.Exporter.prototype.exportJson = function(){
		var _nodeAttributes = ['fr', 'fr_x', 'fr_y'];
		var _edgeAttributes = ['color'];

		var jsonString = _s.toJSON({removeNodeAttributes: _nodeAttributes, removeEdgeAttributes: _edgeAttributes});
	};

	Graphbrowser.Modules.Exporter.prototype.exportGefx = function(){
		var gexfString = _s.toGEXF({
			download: false,
			nodeAttributes: 'data',
			edgeAttributes: 'data',
			renderer: _s.renderers[0],
			creator: 'OP',
		});
	};

	Graphbrowser.Modules.Exporter.prototype.triggerSave = function(nameEntered){
		var self = this;
		var name = $('#exporter-saveGraph-form-name').val();
		if(name && name.length !== 0){
			_callbacks.saveGraph(name);
		}
		else{
			if(_callbackOnError !== undefined)
				_callbackOnError();
		}				
	};

	Graphbrowser.Modules.Exporter.prototype.onSave = function(data){
		var self = this;
		data['mode'] = "all";
		var _nodeAttributes = ['fr', 'fr_x', 'fr_y'];
		var _edgeAttributes = ['color'];

		var jsonGraphString = _s.toJSON({removeNodeAttributes: _nodeAttributes, removeEdgeAttributes: _edgeAttributes});
		var jsonDataString = JSON.stringify(data);
		_callbacks.conn.saveGraphView(data.name, jsonGraphString, jsonDataString, currentViewId, self.onGraphSaved.bind(self), false);
	};

	Graphbrowser.Modules.Exporter.prototype.onGraphSaved = function(){	
		if(_callbackOnSuccess !== undefined)
			_callbackOnSuccess();
	};

}).call(window);