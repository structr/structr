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
                obj[prop] = value;
                if (prop !=='id' && prop !== 'type') {
                    // Build a new object that just contains id, type and the changed value
                    let persistenceObj = {
                        id: obj.id,
                        type: obj.type
                    };
                    persistenceObj[prop] = value;
                    persistence._persistObject(persistenceObj);
                }
            }

        }
    }

    static getType() {
        return 'AbstractNode';
    }


}