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

## Features

+ optional reverse-enginering of 1-n and n-n joins
+ centralized model definition gathering all SQL code
+ fully configurable mapping of tables and columns identifiers
+ fully configurable mapping of values
+ works with any JDBC-compliant data source
+ instances (rows) can be mapped to any POJO
+ connections and prepared statements pooling

## Main Concepts

**Entities** represent the business logic concepts of the model. They often correspond to a database table.

**Instances** are individual representations of entities, with specific values. They often correspond to a table row values. Apart from specific cases
(like the logged user in the session), instances lifetime should not exceed the query.

**Attributes** are named SQL queries which appear as properties of the model itself (*root attributes*) or properties of a
specific entity instances. There are three types of attributes:

+ **Scalar** attributes, corresponding to the **`evaluate()`** method, returning a scalar (string, number, boolean).
+ **Row** attributes, corresponding to the **`retrieve()`** method, returning an instance.
+ **Rowset** attributes, corresponding to the **`query()`** method, returning an iterator over instances.

**Actions**, corresponding to the **`perform()`** method,  are named SQL queries performing an atomic or transactionnal database change.
Actions with more than one statement are **Transactions**. Actions return the number of changed database rows.

Each of the above methods can be invoked from the model object itself or from an instance, and can take additional query parameter arguments.

Here's the [Javadoc](https://republicate.com/modality/apidocs/) (wip).

### Configuration

You can either:

+ Start with an empty schema, and declare entities and attributes manually within it (*explicit* method).
+ Set a specific level of reverse enginering of a database schema (*implicit* method). In this case, an entity is
created for each table in the schema, plus optionally an attribute for each join.

Configuration can take place:

+ in a `Map` given to `Model.configure(Map)` or by caling specific Model configuration setters.
+ in `modality.properties` (prefixed by `model.`), if you use the Velocity Engine. This affects all models. In a web context,
this file is typically located in `/WEB-INF/`.
+ in `tools.xml` tools's attribute (without the `model.` prefix), if you use VelocityTools. This affects a specific ModelTool.
+ in `model.xml`, the model definition file (as XML attributes of the `<model>` tag, without the `model.` prefix). This affects all tools using this ModelTool.

The model definition itself (`model.xml`) defines the accessible model objects and gather the SQL code - it is distinct from the above configuration (`model.properties`),
which defines the model generic behavior. This definition contains all entities (which can *or not* correspond to a database table), attributes (sql queries returning a scalar, a row or rowset)
and actions (single or multi modification statements, always kept within a single transaction).

Instances classes can use the generic `Instance` class, or be any Java POJO with getters and setters.

Models can be given a string id, allowing them to be accessed in a static Java context.

Once configured, the model needs to be initialized against a definition file, named by default `model.xm`.
If a path is given using the configuration key `model.definition`, it will be searched for instead of the default. It is searched:
+ in the classpath
+ in the webapp servlet context resources when in a web environment
+ in the filesystem, and also with /WEB-INF/ prefixed when in a web environment

### Use from Java

A model needs a definition file, which is searched using the default name `model.xml` 

Please refer to the ModelTool javadoc.

DOCUMENTATION TODO 
maven dependency
choose where to place configuration parameters
choose initialization method

You can use the *explicit* (control visibility of data objects in VTL) or *implicit* method (show a full schema tables in VTL).

#### Explicit method

Let's start with the easiest one, the implicit method.

You need to set the property: `model.reverse = full`.

... in progress ...


