goog.provide("ecs.Entity");

ecs.Entity.Entity = Entity;

function Entity () {
  this.id = nextId++;
  this._manager = null;
  this._components = [];
  this._tags = [];
}

Entity.prototype.__init = function () {
  this.id = nextId++;
  this._manager = null;
  this._components.length = 0;
  this._tags.length = 0;
}

var nextId = 0;
Entity.prototype.addComponent = function (cName, cData) {
  this._manager.entityAddComponent(this, cName, cData);
  return this;
}

Entity.prototype.removeComponent = function (cName) {
  this._manager.entityRemoveComponent(this, cName);
  return this;
}

Entity.prototype.hasComponent = function (cName) {
  return !!~this._components.indexOf(cName);
}

Entity.prototype.removeAllComponents = function () {
  return this._manager.entityRemoveAllComponents(this);
}

Entity.prototype.get = function(cName) {
  return this[cName].data;
}

function _getNested(obj, path) {
  if(path.length == 1) {
    return obj[path[0]];
  } else {
    return _getNested(obj[path[0]], path.slice(1));
  }
}

function _getNested(object, path) {
  var index = 0,
      length = path.length;

  while (object != null && index < length) {
    object = object[(path[index++])];
  }
  return (index && index == length) ? object : undefined;
}

var lookups = {};

function pathArrPath(path) {
  var ret = path[0];
  for(var i = 1; i < path.length; i++) {
    ret += "." + path[i];
  }
  return ret;
}

Entity.prototype.getNested = function(cName, path) {
  if(lookups[path]) {
    return lookups[path](this[cName].data);
  }
  else {
    var f = new Function( "o", "return o." + pathArrPath(path) );
    lookups[path] = f;
    return  f(this[cName].data);
  }
};


function _setNested(obj, path, val) {
  if(path.length == 1) {
    obj[path[0]] = val;
  }
  else {
    _setNested(obj[path[0]], path.slice(1), val);
  }
}

Entity.prototype.set = function(cName, path, val) {
  _setNested(this[cName].data, path, val);
}

Entity.prototype.hasAllComponents = function (cNames) {
  var b = true;

  for (var i = 0; i < cNames.length; i++) {
    var c = cNames[i];
    b &= !!~this._components.indexOf(c);
  }

  return b;
}

Entity.prototype.hasTag = function (tag) {
  return !!~this._tags.indexOf(tag);
}

Entity.prototype.addTag = function (tag) {
  this._manager.entityAddTag(this, tag);
  return this;
}

Entity.prototype.removeTag = function (tag) {
  this._manager.entityRemoveTag(this, tag);
  return this;
}

Entity.prototype.remove = function () {
  return this._manager.removeEntity(this);
}
