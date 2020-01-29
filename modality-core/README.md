# Modality Core

Lightweight ORM.

It comprises:

- A lightweight but highly configurable ORM
- An extensible XML model definition schema

## Rationale

+ Pull MVC model ORM with easy syntax
+ ease of use from Java as well from VTL
+ respect the SQL language
    - no error prone persistence *at all*, minimal caching
    - no limitating `.select(...).where(...)` dialect
    - avoid dissemination of SQL code in the application
+ easily configurable, with reasonable defaults
+ keep it a lightweight tool
+ no code generation, no annotations

## Features

+ optional reverse-enginering of 1-n and n-n joins
+ centralized model definition gathering all SQL code
+ fully configurable mapping of tables and columns identifiers
+ fully configurable mapping of values
+ works with any JDBC-compliant data source
+ instances (rows) can be mapped to any POJO
+ connections and prepared statements pooling

## Main Concepts

**Model** represents the model of a specific database schema. It comprises entities (tables) as long as attributes (named user-defined queries).

> There are several ways of configuring and initializing a model, but you could do:
>
> ```java
> Model bookstore = new Model("bookstore");
> bookstore.setDatasource("jndi/my_bookstore").setReverseMode("joins").initialize();
> ```

**Entities** represent the business logic concepts of the model. They often correspond to a database table.
>
> For instance, let's get the entity for our "books" table:
>
> ```java
> Entity books = bookstore.getEntity("books");
> ```

**Instances** are individual representations of entities, with specific values. They often correspond to a table row values. Apart from specific cases
(like the logged user in the session), instances lifetime should not exceed the query.

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

Attributes are defined via the XML model definition file. Row and rowset attributes can be given a `result` XML attribute, referencing an entity of the model. This way, you can chain such attributes, for instance:

    $book.author.birth_country.name

When reverse enginering joins, foreign key attributes like `$book.author` are available straight out of the box.

> Let's define a new root attribute to get all the books published after a certain date in the XML model definition file.
> 
> ```xml
> <rowset name="published_after" result="books">
>   SELECT * FROM books WHERE publication_date > DATE <publication_date/>;
> </rowset>
> ```
>
> and use this attribute from java
> 
> ```java
> Iterator<Instance> bookIterator = model.query("published_after", "2018-01-01");
> while (bookIterator.hasNext()) {
>     Instance book = bookIterator.next();
>     System.out.println(book.getString("title") + " / published on " + dateFormat.format(book.getDate("publication_date")))
> }
>
> Let's define programmatically a new book attribute returning the number of total books for the book author.
> 
> ```xml
> <books>
>   <scalar name="same_author_count">
>     SELECT count(*) FROM books WHERE author_id = <author_id/>
>   </scalar>
> </books>
> ```
>
> ```java
> int melvilleBooksCount = mobyDick.evaluateInteger("same_author_count");
> ```

**Actions**, corresponding to the **`perform()`** method,  are named SQL queries performing an atomic or transactionnal database change.
Actions with more than one statement are **Transactions**. Actions return the number of changed database rows.

Each of the above methods can be invoked from the model object itself or from an instance, and can take additional query parameter arguments (or a { name => value } map of those arguments).

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

Instances classes can use the generic `Instance` class, or be any Java POJO with standard getters and setters.

Models can be given a string id, allowing them to be accessed in a static Java context (using the ModelRepository).

Once configured, the model needs to be initialized against a definition file, named by default `model.xm`.
If a path is given using the configuration key `model.definition`, it will be searched for instead of the default. It is searched:

+ in the classpath
+ in the webapp servlet context resources when in a web environment
+ in the filesystem, and also with /WEB-INF/ prefixed when in a web environment

[TODO - add more examples]
