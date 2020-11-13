# FunctionSearch
Experimental research project providing type-based function searching for the JVM.

FunctionSearch (working title) is an attempt to bring [Hoogle](https://hoogle.haskell.org) inspired searching capabilities to JVM programming languages, starting with Java.
The goal is to provide an intuitive search engine that uses type-based queries to find available operations in JAR modules and the standard library (JCL).

Presently, the system is capable of parsing and processing the standard library and external JARs, including Maven artifacts and their dependencies.
This data then provides a domain upon which queries may be evaluated. The results show any compatible functions.

## Queries
Queries are inspired by Hoogle's own, and are designed to be intuitive for Java programmers.
Queries consist a comma separated enumeration of input parameters and an arrow separated output parameter.
The order of the input parameters are ignored, as they convey no special meaning for behavior.

Here are some example queries to give a flavor of the syntax:
```
// Simple query matching List<String>.get
List<String>, int -> String

// Query featuring function type (Map<String, Person>.computeIfAbsent)
Map<String, Person>, String, (String -> Person) -> Person

// Generic query with free type paramter (List.get)
List<a>, int -> a

// Generic query with constrained type paramter (Collections::max)
<a : Comparable<a>> Collection<a> -> a

```
