goog.provide("ecs.EntityManager");
goog.require("ecs.Entity");
goog.require("ecs.ReusePool");

ecs.EntityManager.Manager = EntityManager;

function EntityManager (globals) {
  this._tags = {};
  this._entities = [];
  this._groups = {};
  this._entityPool = ecs.ReusePool.pool(function () { return new ecs.Entity.Entity(); });
  this._componentPools = {};
  this.globals = globals;
}

function Group (Components, entities) {
  this.Components = Components || [];
  this.entities = entities || [];
}

EntityManager.prototype.createEntity = function () {
  var entity = this._entityPool.get();

  this._entities.push(entity);
  entity._manager = this;
  return entity;
}

EntityManager.prototype.removeEntitiesByTag = function (tag) {
  var entities = this._tags[tag];

  if (!entities) return;

  for (var x = entities.length - 1; x >= 0; x--) {
    var entity = entities[x];
    entity.remove();
  }
}

EntityManager.prototype.removeAllEntities = function () {
  for (var x = this._entities.length - 1; x >= 0; x--) {
    this._entities[x].remove();
  }
}

EntityManager.prototype.removeEntity = function (entity) {
  var index = this._entities.indexOf(entity);

  if (!~index) {
    throw new Error('Tried to remove entity not in list');
  }

  this.entityRemoveAllComponents(entity);

  // Remove from entity list
  this._entities.splice(index, 1);

  // Remove entity from any tag groups and clear the on-entity ref
  entity._tags.length = 0;
  for (var tag in this._tags) {
    var entities = this._tags[tag];
    var n = entities.indexOf(entity);
    if (~n) entities.splice(n, 1);
  }

  // Prevent any acecss and free
  entity._manager = null;
  this._entityPool.recycle(entity);
}

EntityManager.prototype.entityAddTag = function (entity, tag) {
  var entities = this._tags[tag];

  if (!entities) {
    entities = this._tags[tag] = [];
  }

  // Don't add if already there
  if (~entities.indexOf(entity)) return;

  // Add to our tag index AND the list on the entity
  entities.push(entity);
  entity._tags.push(tag);
}

EntityManager.prototype.entityRemoveTag = function (entity, tag) {
  var entities = this._tags[tag];
  if (!entities) return;

  var index = entities.indexOf(entity);
  if (!~index) return;

  // Remove from our index AND the list on the entity
  entities.splice(index, 1);
  entity._tags.splice(entity._tags.indexOf(tag), 1);
}

EntityManager.prototype.entityAddComponent = function (entity, cName, cData) {
  if (~entity._components.indexOf(cName)) return;

  if(typeof cData === undefined) {
    cData = null;
  }

  entity._components.push(cName);

  // Create the reference on the entity to this component
  entity[cName] = {data: cData,
                   entity: entity};

  // Check each indexed group to see if we need to add this entity to the list
  for (var groupName in this._groups) {
    var group = this._groups[groupName];

    // Only add this entity to a group index if this component is in the group,
    // this entity has all the components of the group, and its not already in
    // the index.
    if (!~group.Components.indexOf(cName)) {
      continue;
    }
    if (!entity.hasAllComponents(group.Components)) {
      continue;
    }
    if (~group.entities.indexOf(entity)) {
      continue;
    }

    group.entities.push(entity);
  }

}

EntityManager.prototype.entityRemoveAllComponents = function (entity) {
  var Cs = entity._components;

  for (var j = Cs.length - 1; j >= 0; j--) {
    var C = Cs[j];
    entity.removeComponent(C);
  }
}

EntityManager.prototype.entityRemoveComponent = function (entity, cName) {
  var index = entity._components.indexOf(cName);
  if (!~index) return;

  // Check each indexed group to see if we need to remove it
  for (var groupName in this._groups) {
    var group = this._groups[groupName];

    if (!~group.Components.indexOf(cName)) {
      continue;
    }
    if (!entity.hasAllComponents(group.Components)) {
      continue;
    }

    var loc = group.entities.indexOf(entity);
    if (~loc) {
      group.entities.splice(loc, 1);
    }
  }

  // Remove T listing on entity and property ref, then free the component.
  entity._components.splice(index, 1);
  delete entity[cName];
}

EntityManager.prototype.queryComponents = function (cNames) {
  var group = this._groups[groupKey(cNames)];

  if (!group) {
    group = this._indexGroup(cNames);
  }

  return group.entities;
}

EntityManager.prototype.queryTag = function (tag) {
  var entities = this._tags[tag];

  if (entities === undefined) {
    entities = this._tags[tag] = [];
  }

  return entities;
}

EntityManager.prototype.count = function () {
  return this._entities.length;
}

EntityManager.prototype._indexGroup = function (cNames) {
  if (this._groups[cNames]) return null;

  var group = this._groups[groupKey(cNames)] = new Group(cNames);

  for (var n = 0; n < this._entities.length; n++) {
    var entity = this._entities[n];
    if (entity.hasAllComponents(cNames)) {
      group.entities.push(entity);
    }
  }
  return group;
}

function groupKey (cNames) {
  return cNames
    .map(function (x) { return x.toLowerCase(); })
    .sort()
    .join("-");
}
