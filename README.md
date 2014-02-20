AndroJoin
=========

An experimental implementation of join calculus in Java for Android.

Join calculus is a formal model for (mostly) purely functional, concurrent computations. Join calculus is somewhat similar to the actor model but, in some sense, is "more declarative".

There are a few implementations of join calculus in functional programming languages such as OCaml ("JoCaml"), F# ("joinads"), and Scala ("scalajoins").

An implementation in Objective-C using Android is in the github repository "CocoaJoin". The README file also contains a tutorial on join calculus.

For a tutorial introduction to join calculus and several examples using JoCaml, see https://sites.google.com/site/winitzki/tutorial-on-join-calculus-and-its-implementation-in-ocaml-jocaml

This project contains the join calculus library and an example Android application, `DinPhil5`, that simulates five "dining philosophers" taking turns thinking and eating. The asynchronous logic of this Android application is implemented as a declarative, purely functional program in join calculus.

Version history
---------------

* Version 0.0.1

Initial commit. First ideas about the Java API are put into tests. Nothing works yet.