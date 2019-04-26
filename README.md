# Modality

Modality implements a pull model layer for a J2EE MVC architecture.

It comprises:

- A lightweight but highly configurable ORM 
- An extensible XML model definition schema

## Rationale

+ Pull MVC model ORM with easy syntax
+ optional reverse-enginering of 1-n and n-n joins
+ ease of use from Java as well from VTL
+ respect the SQL language
    - no error prone persistence *at all*
    - no limitating `.select(...).where(...)` dialect
    - avoid dissemination of SQL code in the application
+ easily configurable, with reasonable defaults
+ keep it a lightweight tool

### Java API

TODO

### Configuration

You can either:

+ Start with an empty schema, and declare entities and attributes manually within it (*explicit* method).
+ Set a specific level of reverse enginering of a database schema (*implicit* method).

Configuration can take place:

+ in a Map given to Model.configure(Map)
+ in `velocity.properties` (prefixed by `model.`), if you use the Velocity Engine. This affects all models.
+ in `tools.xml` tools's attribute (without the `model.` prefix), if you use VelocityTools. This affects a specific ModelTool.
+ in `model.xml`, the model definition file (without the `model.` prefix). This affects all tools using this ModelTool.

The model definition itself is distinct from its configuration (which defines its generic behavior).
It contains all entities (which can *or not* correspond to a database table), attributes (sql queries returning a scalar, a row or rowset)
and actions (single or multi modification statements, always kept within a single transaction).

Models can be given a string id, allowing them to be accessed in a static Java context.

Once configured, the model needs to be initialized against a definition file, named by default `model.xm`.
If a path is given using the configuration key `model.definition`, it will be searched for instead of the default. It is searched:
+ in the classpath
+ in the webapp servlet context resources when in a web environment
+ in the filesystem, and also with /WEB-INF/ prefixed when in a web environment

### Use from Java

A model needs a definition file, which is searched using the default name `model.xml` 

Please refer to the ModelTool javadoc.

maven dependency
choose where to place configuration parameters
choose initialization method

TODO 

You can use the *explicit* (control visibility of data objects in VTL) or *implicit* method (show a full schema tables in VTL).

#### Explicit method

Let's start with the easiest one, the implicit method.

You need to set the 'model.

### Features

+ provides both a Java API and a VTL API with an intuitive syntax without code generation
+ configurable database reverse enginering (tables, columns, joins and even n-n joins), and allows a progressive enrichment of the model in a hierarchical model definition file gathering all SQL code
+ configurable table and column names mapping
+ configurable values types mapping
+ configurable access control?

