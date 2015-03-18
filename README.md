AndroJoin
=========

An experimental implementation of join calculus in Java for Android.

Join calculus is a formal model for (mostly) purely functional, concurrent computations. Join calculus is somewhat similar to the actor model but, in some sense, is "more declarative".

There are a few implementations of join calculus in functional programming languages such as OCaml ("JoCaml"), F# ("joinads"), and Scala ("scalajoins"). It is perfectly possible to use join calculus with an imperative object-oriented language or with a functional language.

For an introduction to join calculus and many examples using JoCaml, see https://sites.google.com/site/winitzki/tutorial-on-join-calculus-and-its-implementation-in-ocaml-jocaml

An implementation in Objective-C for iOS is in the github repository `CocoaJoin`. The `README` file in the `CocoaJoin` repository also contains a detailed, language-neutral tutorial on concurrent programming in join calculus.

This `AndroJoin` project is an embedding of join calculus in Java, and is quite similar to `CocoaJoin` in spirit. The library consists of a single class `AJoin`. The project also contains an example Android application, `DinPhil5`, that simulates five "dining philosophers" taking turns thinking and eating. The asynchronous logic of this Android application is implemented as a declarative, purely functional program in join calculus.

Using `AndroJoin`
===============

Due to the limitations of Java (no macros, no implicit anything), the use of `AndroJoin` is quite verbose.

A join calculus program has three required parts:

- definition of the molecule names
- definition of reactions (a "join definition")
- injection of initial molecules into the chemical machine

Defining molecule names
-----------------------

Molecule names are Java objects of specially provided classes such as `M_int`, `M_empty`, `M_empty_int`, and so on. These classes all inherit from the abstract class `M_A`.

Synchronous ("fast") molecules inherit from the abstract class `M_S`.

The user must declare and instantiate the molecule names explicitly, as local variables. For example,

	final M_int counter = new M_int("global counter");
	final M_empty enabled = new M_empty();
	final M_empty_int getCount = new M_empty_int();

The optional parameter to the class constructor is a string uniquely identifying the molecule name. This string can be useful for debugging.

All molecule names must be declared `final`.

Defining reactions
------------------

A join definition consists of the global function `AJoin.define` that takes a list of reactions. Each reaction consumes some input molecules and executes the reaction body.

For example, here is a definition of the asynchronous counter in the JoCaml syntax,

	def counter(n) & inc() = counter(n+1)
	 or counter(n) & getCount() = counter(n), reply n to getValue;;

and in the Java syntax,

	final M_int counter = new M_int();
	final M_empty inc = new M_empty();
	final M_empty_int getCount = new M_empty_int();
	
	define(
		reaction(consume(counter, inc), new ReactionBody() {
			public void run(int n) { // the "n" is the value of "counter"
				counter.put(n+1);
			}
		}), 
		reaction(consume(counter, getCount), new ReactionBody() {
			public void run(int n) { // the "n" is the value of "counter"
				to(getCount.reply(n));
				counter.put(n);
			}
		})
	);

The `consume` construct uses the actual values of `counter`, `inc`, `getValue` created earlier.

An anonymous class derived from `ReactionBody` needs to define the `run` method. This method must have the correct number of arguments of correct types; one argument per value of the input molecule. If input molecules do not have any values, the `run` method must take no arguments.

For example, consider a reaction that consumes three molecules: `counter(1)`, `enable()`, and `name("abc")`. The body of the reaction is a function of _two_ values: an integer and a string, because the `enable` molecule has an empty value. The order of these values is fixed by the order of the consumed molecules. Therefore, the reaction should be defined like this,

	... reaction(consume(counter, enable, name), new ReactionBody() {
			public void run(int n, String s) {
			// counter(n) & enable() & name(s) => ...
				...
			}
	})

For convenience, a static import of `AJoin.*` can be used to import the names `define`, `defineUI`, `reaction`, `reactionUI`, `consume`, `ReactionBody`, `to`.

Injecting molecules
-------------------

Molecules are injected with the method `put`.

	final M_int counter = new M_int();
	counter.put(0);

Fast molecules are injected in the same way, except that they return a value.

	int n = getCount.put();

Replying to fast molecules is performed with the methods `ReactionBody.to` and `M_S.reply` in the following way:

	to(m.reply(x)); // reply x to m


Implementation notes
====================

Join objects are instances of the class `AJoin`. These instances are not directly available to the programmer.

Fully constructed molecules are instances of the same classes as molecule names. However, the programmer cannot use them directly.

Molecule names and join definitions are locally scoped and can be captured in closures, according to the standard Java mechanisms.

Runtime exceptions are generated when the user violates the semantics of join calculus:

- defining a reaction that consumes a molecule defined in another join definition
- injecting a molecule that is not consumed in any reaction

If a reaction running on a background thread generates a fatal exception, this exception will terminate the thread but not the application.

All join definitions are processed either on the UI thread (when designated) and otherwise on a single, dedicated background thread. Thus, all non-UI join definitions run _sequentially_.

All reactions are processed either on the UI thread (when designated) or on a pool of background threads. The number of threads in the pool is no more than one background thread per available CPU core, except for single-core CPU where 2 threads are used. This is to guarantee some concurrency.

Version history
===============

* Version 0.1.2

Additional checks and runtime exceptions generated in the code.

Allocate more background threads if there are more cores available, but not less than 2.

* Version 0.1.1

Fixed problems with multithreading. This version is usable now. The semantics of purely functional join calculus is fully implemented.

Synchronous molecules and UI-thread joins are not yet tested.

* Version 0.1

Almost completely implemented version of the `AndroJoin` library, closely following the iOS implementation (`CocoaJoin`).

Test application ("dining philosophers") runs but gets deadlocked after a while, due to bugs in the threads implementation and/or limitations of `AsyncTask` on Android (a single-threaded executor is being used for all tasks, which probably breaks the semantics of join calculus). This needs to be fixed before `AndroJoin` can be considered at all usable.

Roadmap
=======

- Add suspend/resume/stop functionality to joins, or else asynchronous reactions will continue forever, possibly holding stale references to activities or views

- Add more molecule classes

- Full testing

- More features to match the iOS implementation
