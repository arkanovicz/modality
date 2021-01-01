<style>
  .docs-menu { float:right };
  .docs-menu > ul { list-style-type: none; }
  .docs-menu > ul > li { font-style:italic; }
</style>

<p align="center">
  <img src="https://raw.githubusercontent.com/arkanovicz/modality/master/src/site/modality.png" title="Modality">
</p>

**Modality** is a lightweight but hightly configurable Java ORM, with a companion set of tools 


<div class="docs-menu">
  <ul>
    <li><a href="https://arkanovicz.github.io/modality/docs/index.html">docs home</a></li>
    <li><a href="https://arkanovicz.github.io/modality/docs/modality-core.html">modality-core doc</a></li>
    <li><a href="https://arkanovicz.github.io/modality/docs/modality-webapp.html">modality-webapp doc</a></li>
    <li><a href="https://arkanovicz.github.io/modality/docs/velocity-tools-model.html">velocity-tools-model doc</a></li>
    <li><a href="https://arkanovicz.github.io/modality/docs/apidocs/">javadocs</a></li>
  </ul>
</div>

# Modality ORM library

Encompases a data access layer, a hierarchical and reentrant model definition gathering SQL queries, database values, names custom filters and much more. See [modality-core's REAME](modality-core).

Usage: include the needed module(s) in your `pom.xml`'s dependencies section as follow:

      <dependency>
          <groupId>com.republicate.modality</groupId>
          <artifactId>modality-core</artifactId>
          <version>1.0</version>
      </dependency>

# Modality Web anti-framework

While the ORM is usable on its own or within other Java/Kotlin web frameworks, the project comes with a companion set of tools which stick to the J2EE and JDBC standards to provide a lightweight, loosely coupled and highly cusomizable [MVC](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) [pull](https://en.wikipedia.org/wiki/Web_framework#Push-based_vs._pull-based) architecture based on the Java J2EE servlets API.

Its design is based on a [bottom-up](https://en.wikipedia.org/wiki/Top-down_and_bottom-up_design) approach rather than top-down. This makes Modality an *anti-framework*: its various components do stick to the underlying norms, have a minimal interdependance and just do the specific task they are intended to, leveraging the learning curve and added complexity, and nevertheless those components assembly cover all the basic needs of a fully functional MVC webapp.

+ the Model layer is addressed with a generic ORM Java API witouth any code generation and with minimal optional caching, providing a complete database structure reverse enginering feature.
+ the View layer is Apache Velocity, but other Java View layer technologies can easily be used instead.
+ the Control layer is decentralized in URI to path conventions (like serving *basedir*`/foo.vhtml` for `GET /foo.vhtml`), configuration files and helpers. It can easily be blended in existing frameworks.

Components:

+ [modality-api-client](modality-api-client) - API client utility for JSON/XML APIs.
+ [modality-webapp](modality-webapp) - servlets and filters for Modality-core configuration and initialization in a J2EE webapp environment.
+ [modality-webapp-auth](modality-webapp-auth) - authentication filters assortment.
+ [velocity-tools-model](velocity-tools-model) - view layer model tool which encapsulates modality-core model objects for use with Apache Velocity Tools.

Modules `modality-webapp` and `modality-webapp-auth` have been published with v1.0 because they are needed by `modality-example-bookshelf`, but shall undergo some refactoring. 
