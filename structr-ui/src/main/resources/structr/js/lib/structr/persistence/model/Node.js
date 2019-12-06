'use strict';

export class Node {

    constructor(id, name, type) {
        if(id !== undefined && id !== null) {
            this.id = id;
        }
        if (name !== undefined && name !== null && name.length > 0) {
            this.name = name;
        }

        if (type !== undefined && type !== null) {
            this.type = type;
        } else {
            this.type = Node.getType();
        }

    }

    static getProxyHandler(persistence) {
        return {
            set: function(obj, prop, value){
                if (prop !=='id' && prop !== 'type') {
                    obj[prop] = value;
                    // Build a new object that just contains id, type and the changed value
                    let persistenceObj = {
                        id: obj.id,
                        type: obj.type
                    };
                    persistenceObj[prop] = value;
                    persistence._persistObject(persistenceObj);
                }
                return true;
            }

        }
    }

    addRelation(attribute, targetId, multiple) {
        if (Object.keys(this).includes(attribute)) {
            if (this[attribute].isArray()) {
                let arr = this[attribute];
                arr.push(target);
                this[attribute] = arr;
            } else {
                this[attribute] = target;
            }
        } else {
            if (multiple !== undefined && multiple === true) {
               let arr = [];
               arr.push(target);
               this[attribute] = arr;
            } else {
                this[attribute] = target;
            }
        }
    }

    removeRelation(attribute, targetId) {
        if (Object.keys(this).includes(attribute)) {
            if (this[attribute].isArray()) {
                let arr = this[attribute];
                let index = arr.indexOf(target);
                if (index > -1) {
                    this[attribute] = arr.splice(index, 1);
                }
            } else {
                if (this[attribute] === target) {
                    this[attribute] = null;
                }
            }
        }
    }

    static getType() {
        return 'AbstractNode';
    }


}
