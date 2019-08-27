# Making Documents with Vert.X

So you can do things like assembling documents in Vert.X, here
is a library.

## Build status
[![build status](https://travis-ci.org/ke4roh/vertx-engine.svg?branch=master)](https://travis-ci.org/ke4roh/vertx-engine/branches)
[![Coverage Status](https://coveralls.io/repos/github/ke4roh/vertx-engine/badge.svg?branch=master)](https://coveralls.io/github/ke4roh/vertx-engine?branch=master)

## Writing a step

Extend `AbstractStep` (you could use the Step interface, but AbstractStep has a whole lot of basic functionality to it)
and annotate your class with this:
```
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
````

## Writing a function

Extend `AbstractJinjaFunctionDefinition` and annotate your class with this:
```
import org.kohsuke.MetaInfServices;

@MetaInfServices(FunctionDefinition.class)
````

Annotate your public static methods with `@ELFunction`
