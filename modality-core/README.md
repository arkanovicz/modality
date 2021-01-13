# Modality Core

Lightweight ORM for Java, providing:

- An automatic reverse enginering of the database structure
- An extensible XML model definition schema
- Custom filtering of identifiers an values

## Rationale

+ Pull MVC model ORM with easy syntax
+ ease of use from Java as well as from template languages like VTL
+ respect the SQL language
    - no error prone persistence *at all*, minimal caching
    - no limitating `.select(...).where(...)` dialect
    - avoid dissemination of SQL code in the application
+ easily configurable, with reasonable defaults
+ keep it a lightweight tool
+ no code generation, no annotations

## Features

+ optional reverse engineering of 1-n and n-n joins
+ centralized model definition gathering all SQL code
+ fully configurable mapping of tables and columns identifiers
+ fully configurable mapping of values
+ works with any JDBC-compliant data source
+ instances (rows) can be mapped to any POJO
+ connections and prepared statements pooling
+ database schema versionning

## Main Concepts

**Model** represents the model of a specific database schema. It comprises entities (tables) as long as attributes (named user-defined queries).

> There are several ways of configuring and initializing a model, but you could do:
>
> ```java
> Model bookstore = new Model("bookstore");
> bookstore.setDatasource("jndi/my_bookstore").setReverseMode("joins").initialize();
> ```

**Entities** represent the business logic concepts of the model. They can be reverse enginered from the working schema tables, and can be associated with a user provided POJO class.
>
> For instance, let's get the entity for our "books" table:
>
> ```java
> Entity books = bookstore.getEntity("books");
> ```

**Instances** are individual representations of entities, with specific values, with typed getters. They often correspond to a table row values.

When they belong to an entity with a primary key:

+ they can be fetched using **`entity.fetch(PK values...)`** and created using **`entity.newInstance({optional initial values map})`**
+ they have **`insert()`**, **`update()`**, **`upsert()`**, **`delete()`**, **`refresh()`** methods

> Let's do some CRUD operations
> ```java
> Instance mobyDick = books.newInstance();
> mobyDick.put("title", "Moby Dick");
> mobyDick.insert();
> long id = mobyDick.getLong("book_id");
> ...
> Instance check = books.fetch(id);
> assertEquals("Moby Dick", check.getString("title"));
> check.put("title", "The Whale");
> check.update();
> ...
> check.delete();
> ```

**Attributes** are named SQL queries which appear as properties of the model itself (*root attributes*) or properties of a
specific entity instances. There are three types of attributes:

+ **Scalar** attributes, corresponding to the **`evaluate()`** method, returning a scalar (string, number, boolean).
+ **Row** attributes, corresponding to the **`retrieve()`** method, returning an instance.
+ **Rowset** attributes, corresponding to the **`query()`** method, returning an iterator over instances.

Join attributes (aka `$book.author`) can be reverse enginered : 1-n for `joins` reverse mode and both 1-1 and n-n for `extended` reverse mode.

Other attributes are defined via the XML model definition file. Row and rowset attributes can be given a `result` XML attribute, referencing an entity of the model. This way, you can chain such attributes, for instance:

    $book.author.birth_country.name

> Let's define a new root attribute to get all the books published after a certain date in the XML model definition file
> 
> ```xml
> <rowset name="published_after" result="books">
>   SELECT * FROM books WHERE publication_date > DATE <publication_date/>;
> </rowset>
> ```
>
> and use this attribute from java:
> 
> ```java
> Iterator<Instance> bookIterator = model.query("published_after", "2018-01-01");
> while (bookIterator.hasNext()) {
>     Instance book = bookIterator.next();
>     System.out.println(book.getString("title") + " / published on " + dateFormat.format(book.getDate("publication_date")))
> }
> ```
> 
> Let's define a new book attribute returning the number of total books for the book author
> 
> ```xml
> <books>
>   <scalar name="same_author_count">
>     SELECT count(*) FROM books WHERE author_id = <author_id/>
>   </scalar>
> </books>
> ```
>
> and use this attribute from java:
> 
> ```java
> int melvilleBooksCount = mobyDick.evaluateInteger("same_author_count");
> ```

**Actions**, corresponding to the **`perform()`** method,  are named SQL queries performing an atomic or transactionnal database change.
Actions with more than one statement are **Transactions**. Actions return the number of changed database rows.

Each of the above methods can be invoked from the model object itself or from an instance, and can take additional query parameter arguments (or a { name => value } map of those arguments).

Manual transactions can be performed via the `Model.attempt()` method which expects a ModelRunnable functional object and handles commit and rollback operations:

```java
try {
    model.attempt(() -> {
        ... crud operations ...
    });
} catch(SQLException sqle) {
    ... handle error ...
}
```

Here's the [Javadoc](https://republicate.com/modality/apidocs/) (wip).

### Configuration

You can either:

+ Start with an empty schema, and progressively declare needed entities and attributes manually within it (*explicit* method).
+ Set a specific level of reverse enginering (for instance `extended` to even get standard n-n joins reverse enginered) of a database schema (*implicit* method). In this case, an entity is
created for each table in the schema, plus optionally two attributes at both ends of each join.

Configuration can take place:

+ in a `Map` given to `Model.configure(Map)` or by caling specific Model configuration setters.
+ in `modality.properties` (prefixed by `model.`), if you use the Velocity Engine. This affects all models. In a web context,
this file is typically located in `/WEB-INF/`.
+ in `tools.xml` tools's attribute (without the `model.` prefix), if you use VelocityTools. This affects a specific ModelTool.
+ in `model.xml`, the model definition file (see below), as XML attributes of the `<model>` tag, without the `model.` prefix. This affects all tools using this ModelTool.

The model definition itself (`model.xml`) defines the accessible model objects and gather the SQL code - it is distinct from the above configuration (`model.properties`),
which defines the model generic behavior. This definition contains all entities (which can *or not* correspond to a database table), attributes (sql queries returning a scalar, a row or rowset)
and actions (single or multi modification statements, always kept within a single transaction).

Small catch: in `model.xml`, you will have to escape the `<` character into `&lt;`.

Instances classes can use the generic `Instance` class, or be any Java POJO with standard getters and setters.

Models can be given a string id, allowing them to be accessed in a static Java context (using the ModelRepository).

Once configured, the model needs to be initialized against a definition file, named by default `model.xm`.
If a path is given using the configuration key `model.definition`, it will be searched for instead of the default. It is searched:

+ in the classpath
+ in the webapp servlet context resources when in a web environment
+ in the filesystem, and also with /WEB-INF/ prefixed when in a web environment

![Model Configuration](https://raw.githubusercontent.com/arkanovicz/modality/master/src/site/model_configuration.png)


### Getting an Existing Model

You can get an existing model:

+ by using the ModelRepository
+ in a webapp context, by using a WebappModelAccessor
+ in a Velocity Tools webapp context, by inheriting your tools from ModelConfig

![Existing Model](https://raw.githubusercontent.com/arkanovicz/modality/master/src/site/existing_model.png)

[TODO - add more examples]

## Filtering

Filters mappings associate a table or column name or pattern (or a Java or SQL data type) to a filter which can be:

+ a named stock filter
+ a regex
+ a class name
+ a sequence of the above items (applied from left to right)
+ a closure

If a stock filter name is prefixed with '-', the corresponding filter is never applied to the specified mapping.  

Filters can be used to transform:

+ SQL identifiers (for instance to remove a common prefix from column names)
+ values read from the database
+ values written to the database

Apart from custom closures, filters can be defined from within `modality.properties` (or from a schema-specific model properties file).

### Table and column names filtering

Available stock filters: `lowercase`, `uppercase`, `snake_to_camel`, `plural_en`

Examples:

```properties
# use lowercase everywhere, pluralize tables names
model.identifiers.mapping.* = lowercase, plural_en
model.identifiers.mapping.*.* = lowercase

# remove foo_ prefix from table names
model.identifiers.mapping.foo_* = /foo_(.*)/$1/
```

### Value filters

Available stock filters: `lowercase`, `uppercase`, `calendar_to_date`, `date_to_calendar`, `number_to_boolean`, `obfuscate`, `deobfuscate`, `base64_encode`, `base64_decode`, `mask`, `no_html`, `escape_html`

Examples:
```properties
# Work with Calendar objects, let the database see Date objects
model.filters.write.java.util.calendar = calendar_to_date
model.filters.read.java.sql.Date = date_to_calendar

# Never return the users.password field
model.filters.read.user.password = mask

# disallow html by default in all fields
model.filters.write.*.* = no_html
# but allow html in fields prefixed by "html_"
model.filters.write.*.html_* = -no_html
# and escape html at rendering for those field
model.filters.read.*.html_* = escape_html
```

### Database schema versionning

Modality provides a basic schema versionning feature.

Migration scripts are specific to each database schema. They are searched in the `migations/$modelId` directory, which can be changed using the `model.migration_scripts` configuration property.

Scripts are supposed to be alphanumerically ordered, with `.sql` extension (typically named `001_do_something.sql`).

This location is searched:

+ as webapp resources in the `WEB-INF` directory when running in a J2EE webapp context
+ as resources in the classpath under the corresponding package
+ in the corresponding file system directory (still under `WEB-INF` when running in a J2EE webapp context)

When found, the following happens at the end of model initialization:

+ Modality will create the `model_version` table in the target schema if it doesn't exist, containing a single `varchar(200)` `script` field, which is meant to receive all the successfully applied scripts filenames (without path).
+ It will then compare available scripts and applied scripts history, bailing out on any divergence or inconsistency.
+ Each remaining available script which hasn't yet been applied will be run inside a specific transaction which will also populate the `model_version` table.

That's it. No checksum tests as in Flyway or Liquibase, which I found rather counterproductive with time.

Be aware that when using an engine which is not able to handle rollbacks of DDL statements (like `mysql` and `mariadb`), you will have to manually revert those if something wrong happens.

When updating a schema, if you are maintaining global creation scripts, you would typically add the `model_version` table and populate it with the considered scripts.
