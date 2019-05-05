# ModelTool

## Features

The ModelTool exposes a model definition to the view.

## VTL API

TODO

    - fetching: #set($book = 
    - properties and joins: $book.publisher.address.zip
    - loops: #foreach($author.books) ...
    - attributes: $book.most_recent_edition.year


## Upgrading Velosurf Webapps

Fix libraries versions (TODO add link towards dependencies).

### In `toolbox.xml`

+ Use the `com.republicate.tools.model.velosurf.VelosurfTool` class for the model.
+ Add `credentials = "`*...path to credentials.xml...*`"` to the `<tool>` tag.

### In `model.xml`

+ Remove the `<credentials>` tag or the `<xi:include>` of the credentials, if any.
+ Replace `<model>`'s attribute `read-only="true|false"` by `write="none|java|vtl"`.
+ Remove `<model>`'s attributes `loglevel` and `seed`, if any.
+ Other deprecated elements will appear as deprecation warnings in the log, but will be taken into account.
+ Obfuscated columns are to be configured in `modality.properties`, see below.

### In `velocity.properties`

Update uberspectors:

    runtime.introspector.uberspect = com.republicate.modality.tools.model.ModelUberspector,org.apache.velocity.util.introspection.UberspectImpl,org.apache.velocity.tools.view.WebappUberspector

### In the Java code

+ Replace all references to velosurf.util.Logger, if any, with slf4j loggers.
+ Servlets or filters needing access to the model can inherit from `com.republicate.modality.webapp.ModalityFilter` or `com.republicate.modality.webapp.ModalityServlet`, and call `getModel()`.
+ In Java code, the generic getter of instances now only concern columns. You'll call `evaluate()` to get a scalar, `retrieve()` to get a row, `query()` to get a rowset and `perform()` to perform an action. The same methods can be called on the model for root attributes.
+ Entities have `fetch()`, `getCount()` and `iterate()` methods.
+ Use the above methods instead of `getWithParams()`.


### `modality.properties`

You'll potentially need a new `/WEB-INF/modality.properties`.

+ For obfuscated columns, set:
    
        model.filters.read.*.foo_id = obfuscate
        model.filters.write.*.foo_id = deobfuscate_strings
    
    This will obfuscate all foo_id columns.

### In the templates

+ You can still call an attribute as if it were a method. Paramters can now also be specified in their natural order instead of inside a map.
+ Don't set parameter values in the model itself. You'll get `java.sql.SQLFeatureNotSupportedException: ModelTool is read-only` messages in the log.
+ The `.order` and `.refine` features have been dropped.
+ The `Instance.setColumnValues()` method has been dropped.

### Testing

Set Velocity to strict mode to be sure your templates are ok.
