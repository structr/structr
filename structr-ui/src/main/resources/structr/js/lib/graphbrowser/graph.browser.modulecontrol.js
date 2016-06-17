var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

(function() {
	'use strict';

	var _s = undefined;
	var _moduleSettings = undefined;
	var _attachedModules = {};
	var _refreshTimeout = undefined;
	var _callbackMethods = {};

	Graphbrowser.Control.ModuleControl = function(callbackMethods, moduleSettings, sigmaInstance){
		var self = this;
		self.name = 'ModuleControl';

		_s = sigmaInstance;
		_callbackMethods = callbackMethods;
		_refreshTimeout = 0;
		_moduleSettings = moduleSettings;

		_callbackMethods.refreshSigma 		= self.refreshSigma.bind(self);
		_callbackMethods.sigmaEvent 		= self.onSigmaEvent.bind(self);
		_callbackMethods.pickNodes 			= self.onPickNodes.bind(self);
		_callbackMethods.filterNodes 		= self.onFilterNodes.bind(self);
		_callbackMethods.hideNodeType 		= self.onHideNodeType.bind(self);
		_callbackMethods.hideNodes 			= self.onHideNodes.bind(self);
		_callbackMethods.dataChanged 		= self.onDataChanged.bind(self);
		_callbackMethods.saveGraph 			= self.onSave.bind(self);
		_callbackMethods.graphRestored		= self.onGraphRestored.bind(self);
		_callbackMethods.undo				= self.onUndo.bind(self);
		_callbackMethods.changeSigmaHandler	= self.onChangeSigmaHandler.bind(self);

		_callbackMethods.api.dataChanged = self.onDataChanged.bind(self);
	};

	Graphbrowser.Control.ModuleControl.prototype.init = function(){
		var self = this;

		if(Graphbrowser.Modules){
			$.each(Graphbrowser.Modules, function(i, module){
				var newModule = new module(_s, _callbackMethods);
				newModule.init(_moduleSettings[newModule.name]);
				_attachedModules[newModule.name] = newModule;		
			});
		}
	};

	Graphbrowser.Control.ModuleControl.prototype.start = function(settings) {
		var self = this;

		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('startListener') > -1 && module.types.indexOf('nodeFilter') > -1)
				module.start();			
		});
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('startListener') > -1 && module.types.indexOf('nodeFilter') === -1){
				module.start();
			}

			if(settings !== undefined){
				if(module.types.indexOf('layout') > -1 && settings.layout !== undefined)
					module[settings.layout].doLayout();				
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.refreshSigma = function(){
		window.clearTimeout(_refreshTimeout);
		_refreshTimeout = window.setTimeout(function(){
			_s.refresh({skipIndexation: false});
		}, 50);
	};

	Graphbrowser.Control.ModuleControl.prototype.onSigmaEvent = function(sigmaEvent, node){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('sigmaEventHandler') > -1){
				module.handleSigmaEvent(sigmaEvent, node);
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onPickNodes = function(nodesToSelect, onNodesSelected, parentNode, parentTitle){
		var self = this;
		var noNodePicker = true;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('nodePicker') > -1 && noNodePicker){
				noNodePicker = false;
				module.pickNodes(nodesToSelect, onNodesSelected, parentNode, parentTitle);
			}
		});

		if(noNodePicker)
			onNodesSelected(nodesToSelect, parentNode);
	};


	Graphbrowser.Control.ModuleControl.prototype.onFilterNodes = function(nodesToFilter, inOrOut){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('nodeFilter') > -1){
				nodesToFilter = module.filterNodes(nodesToFilter, inOrOut);
			}
		});
		return nodesToFilter;
	};

	Graphbrowser.Control.ModuleControl.prototype.onHideNodeType = function(nodeType, status){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('nodeHider') > -1){
				module.hideNodeType(nodeType, status);
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onHideNodes = function(nodes, status){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('nodeHider') > -1){
				module.hideNodes(nodes, status);
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onDataChanged = function(changeType){
		var self = this;	
		self.refreshSigma(false, false);
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('dataChangedHandler') > -1){
				module.onDataChanged(changeType);
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onUndo = function(){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('undoListener') > -1){
				module.onUndo();
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.doLayout = function(layout, options){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('layout') > -1  && layout){
				if(module.name === layout){
					module.doLayout(options);
					return false;
				}
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onSave = function(nameOfView){
		var self = this;
		var data = {'name': nameOfView};
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('saveListener') > -1){
				data[module.name] = module.onSave();
			}
		});

		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('saveHandler') > -1){
				module.onSave(data);
				return false;
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onGraphRestored = function(data){
		var self = this;	
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('restoreListener') > -1){
				if(data[module.name])
					module.onRestore(data[module.name]);
				else
					module.onRestore();
			}
		});

		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('newGraphListener') > -1){
				module.onNewGraph();
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.onChangeSigmaHandler = function(newHandler){
		var self = this;

		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('sigmaHandler') > -1){
				if(module.name !== newHandler)
					module.deactivate();
			}

		});

		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('sigmaHandler') > -1){
				if(module.name === newHandler){
					module.activate();
					return false;
				}
			}			
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.status = function(){
		var self = this;
		var status = {};
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('statusHandler') > -1){
				status[module.name] = module.status();
			}
		});

		return status;
	};

	Graphbrowser.Control.ModuleControl.prototype.reset = function(){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('resetListener') > -1){
				module.onReset();
			}
		});
	};

	Graphbrowser.Control.ModuleControl.prototype.expandNode = function(id){
		var self = this;
		$.each(_attachedModules, function(i, module){
			if(module.types.indexOf('nodeExpander') > -1){
				module.expandNode(id)
			}
		});
	};

}).call(window);