# Lifecycle Methods
## afterCreate
Called after a new object of this type is created.

The `afterCreate()` lifecycle method is called after a new object of this type is created. This method runs after the creating transaction is committed, so you can be sure that the validation was successful and the object is stored in the database.

This method would be the right place to send a welcome email, for example, as you can be sure that the user exists.

### Notes
- See also: `onCreate()`.


## afterDelete
Called after an object of this type was deleted.

The `afterDelete()` lifecycle method is called after an object of this type is deleted. This method runs after the deleting transaction is committed, so you can be sure that the object was deleted from the database.
### Notes
- See also: `onDelete()`.


## afterSave
Called after an existing object of this type is modified.

The `afterSave()` lifecycle method is called after an existing object of this type is modified. This method runs after the modifying transaction is committed, so you can be sure that the validation was successful and the object is stored in the database.
### Notes
- See also: `onSave()`.


## onCreate
Called when a new object of this type is created.

The `onCreate()` lifecycle method is called when a new object of this type is created. This method runs at the end of a transaction, but **before** property constraints etc. are evaluated.

If you throw an error in this method, the enclosing transaction will be rolled back and nothing will be written to the database.

If you want to execute code after successful validation, implement the `afterCreate()` callback method.

### Notes
- See also: `afterCreate()`, `error()` and `assert()`.

### Examples
##### Example 1 (JavaScript)
```
{
	if ($.this.name === 'foo') {

		// don't allow creation of nodes named "foo"
		$.error('name', 'create_not_allowed', 'Can\'t be created because name is "foo"');

	} else {

		$.log('Node with name ' + $.this.name + ' has just been created.');
	}
}

```

## onDelete
Called when an object of this type is deleted.

The `onDelete()` lifecycle method is called when an existing object of this type is deleted. This method runs at the end of a transaction, but **before** property constraints etc. are evaluated.

If you throw an error in this method, the enclosing transaction will be rolled back and nothing will be written to the database.

If you want to execute code after successful validation, implement the `afterDelete()` callback method.

You can access the local properties of the deleted entity through the `this` keyword.

### Notes
- See also: `afterSave()`, `error()` and `assert()`.

### Examples
##### Example 1 (JavaScript)
```
{
	if ($.this.name === 'foo') {

		// don't allow deletion of nodes named "foo"
		$.error('name', 'delete_not_allowed', 'Can\'t be deleted because name is "foo"');

	} else {

		$.log('Node with name ' + $.this.name + ' has been deleted.');
	}
}

```

## onSave
Called when an existing object of this type is modified.

The `onSave()` lifecycle method is called when an existing object of this type is modified. This method runs at the end of a transaction, but **before** property constraints etc. are evaluated.

If you throw an error in this method, the enclosing transaction will be rolled back and nothing will be written to the database.

If you want to execute code after successful validation, implement the `afterSave()` callback method.

### Notes
- See also: `afterSave()`, `error()` and `assert()`.

### Examples
##### Example 1 (JavaScript)
```
{
	if ($.this.name === 'foo') {

		// don't allow deletion of nodes named "foo"
		$.error('name', 'save_not_allowed', 'Name can\'t be changed to "foo"');

	} else {

		$.log('Node with name ' + $.this.name + ' has been modified.');
	}
 }

```
