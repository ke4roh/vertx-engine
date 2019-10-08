# Making Documents with Vert.X

Assemble a json document via a variety of steps, functions, and filters,
using Vert.X and reactive programming.

## Build status
[![build status](https://travis-ci.org/ke4roh/vertx-engine.svg?branch=master)](https://travis-ci.org/ke4roh/vertx-engine/branches)
[![Coverage Status](https://coveralls.io/repos/github/ke4roh/vertx-engine/badge.svg)](https://coveralls.io/github/ke4roh/vertx-engine)

## The basic idea
An input document provides a seed for a completed document.  Our motivation for 
development is preparing search queries and indexing documents for search, but we
see broader opportunities for the general system.

A pipeline provides a set of instructions for how to transform an input document
into its final result.  The `Engine` class is responsible for executing a pipeline.
Pipelines consist of `Section`s and `Step`s.  The Steps in a section will execute 
as soon as their prerequisites are ready.

The `EnginePool` manages a collection of pipelines that might do different things - 
for use in developing pipelines and/or organizing pipelines.

## Writing a pipeline
See the [abstract-step-test-pipeline.json](src/test/resources/abstract-step-test-pipeline.json)
for the basics.  A pipeline consists of a tacit or declared outer section, which includes a 
list of steps. The steps are executed in the order they are ready (which may not be their
order of appearance in the pipeline).  Variables for each step are processed by the
step, and they are evaluated through the Jinja template engine before use.

Steps carry out core functionality for construction of the document.  Logically lighter weight 
activities may be carried out as fiilters and functions within the Jinja template engine.  For example,
a step would call out to a search engine, while a set of filters might manipulate a query string 
in preparation for it.   

## The step environment
When a step executes, `AbstractStep` assembles its environment with the variables provided in the "vars" block and
some standard keys:

| Key     | Explanation |
| ---     | ---         |
| doc     | The data initially provided and all values registered subsequently |
| system  |System variables (-D java options, environment variables, and config file values), as provided to the Engine |
| stepdef | Parameters provided in the definition of this step, such as timeout, when, and register. |

Values not in system and doc (vars), are evaluated with [Jinja](https://github.com/HubSpot/jinjava/) 
templating. 

Jinjava provides [a number of filters](https://static.javadoc.io/com.hubspot.jinjava/jinjava/2.5.2/com/hubspot/jinjava/lib/filter/package-frame.html) 
and [functions](https://static.javadoc.io/com.hubspot.jinjava/jinjava/2.5.2/com/hubspot/jinjava/lib/fn/Functions.html).

Additional functions built in to this project are `re:m(string, pattern)` which returns the first match of the string, 
and `re:s(string, pattern, replacement)` which replaces all occurrences of `pattern` with `replacement`.

Filter `reMatch(pattern)` returns the first match of the pattern in the string
piped in, and `reSub(pattern,replacement)` replaces all occurrences of pattern with replacement.
  
You can write your own steps, functions, and filters, too.   

## Writing a step

Extend `AbstractStep` (you could use the Step interface, but `AbstractStep` has a whole lot of basic functionality to it)
and annotate your class with this:
```
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
````

## Writing a Jinja function

Implement `JinjaFunctionDefinition` and annotate your class with this:
```
import com.redhat.vertx.pipeline.templates.JinjaFunctionDefinition
import org.kohsuke.MetaInfServices;

@MetaInfServices(JinjaFunctionDefinition.class)
````

Annotate your public static methods with `@ELFunction(value,namespace)`, where "value" 
is the name of the function and "namespace" will come before, like
 `re:match("foobar","[fo]*")`.  Namespace may be left blank.

## Writing a Jinja filter
Implement a Jinja `Filter` and annotate your class with this:
```
import com.hubspot.jinjava.lib.filter.Filter;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Filter.class)
````
