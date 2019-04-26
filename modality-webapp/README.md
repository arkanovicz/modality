# Modality



Collection of J2EE filters, servlets and tools helping to build lightweight [pull-MVC](https://en.wikipedia.org/wiki/Web_framework#Push-based_vs._pull-based) webapps.

It's not a framework but rather a collection of modules within which you're free to cherry pick what suits you.

It uses Modality Model for its Model layer, and Apache Velocity for its View layer.

## Content

    com.republicate.modality:modality - parent project
    +- com.republicate.modality:modality-core - Model layer implementation
    +- com.republicate.modality:modality-tools-apiclient - API client tool
    +- com.republicate.modality:modality-tools-model - Model client tool
    +- com.republicate.modality:modality-webapp - Common module for Modality webapp modules
    +- com.republicate.modality:modality-webapp-api - API servlet
    +- com.republicate.modality:modality-webapp-auth - authentication filters
    +- com.republicate.modality:modality-webapp-oauth - OAuth authentication filters
    +- com.republicate.modality:modality-examples - examples parent project
        +- com.republicate:modality-example-bookshelf - Bookshelf Webapp example

## Architecture

Here is the dependency graph of main internal and external modules:

![Modality modules](./src/site/modality-modules.svg)

## Authentication Filters

See the [class diagram](src/site/dependencies.svg), and the [call graph](src/site/auth_call_graph.svg).

### Configuration

    :::properties
    model.auth. +-- protected = ".*" : regexp of protected resources URIs
                +-- session. +-- invalidate_on_logout = true : whether to invalidate session on logout
                |            +-- logged_key = "logged" : session attribute key for logged user
                |            +-- max_inactive_interval = 0 : session lifetime
                |            +-- redirect. +-- parameter = null : redirection query string parameter to honor after login or logout
                |                          +-- referrer = false : whether to redirect using referrer after login or logout
                |            +-- uri. +-- dologin = "login.do" : login action URI
                |                     +-- dologout = "logout.do" : logout action URI
                +-- form. +-- field. +-- login = "login" : login input form field name
                |         |          +-- password = "password" : password input form field name
                |         +-- login_redirect = false : whether to redirect unauthentified requests towards the login URI
                |         +-- success. +-- forward_post = false : whether to save unauthentified POST requests and forward successful logins to them
                |         |            +-- redirect_get = false : whether to save unauthentified GET requests and redirect successful logins to them
                |         +-- uri. +-- home = <auto> : home URI for unauthentified sessions, set by default to '/' resource path
                |                  +-- login = "login.vhtml" : login form URI
                |                  +-- user_home = home : home URI for logged users
                +-- model. +-- model_id = <auto> : model id to use
                |          +-- refresh_rate = 0 : session user instance refresh rate
                |          +-- user_by_credentials = null : model attribute returning a user given correct dredentials
                +-- cookie. +-- check. +-- ip = false : should the *remember me* cookie handler check IPs
                |           |          +-- user_agent = true : should the *remember me* cookie handler check user agents
                |           +-- clean_rate = 0 : rate at which obsolete cookie keys are cleaned up from the database
                |           +-- handler = null : provide an alternate *remember me* cookie handler
                |           +-- name = "remember_me" : *remember me* cookie name
                |           +-- domain = null : *remember me* cookie domain
                |           +-- max_age = 31536000 : *remember me* cookie max age
                |           +-- path = "/" : *remember me* cookie path
                +-- header TODO
                +-- oauth TODO

## API Servlet

