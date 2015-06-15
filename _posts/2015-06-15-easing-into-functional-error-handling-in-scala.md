---
title: "Easing Into Functional Error Handling in Scala"
tags: ["scala", "functional programming", "error handling"]
---

One of the biggest selling points of Scala is being able to transition from Java to the world of functional programming. What this means, practically speaking, is that you and/or your team can start off writing very Java-like Scala that already works, treating it as a "better Java" with more goodies and less verbose syntax. But why stop at that? We can do better by adopting more typesafe functional concepts, and the best part is we can ease ourselves into at least one practical application without drowning: functional error handling.

## On exceptions

Java has the concept of [checked exceptions](http://en.wikibooks.org/wiki/Java_Programming/Checked_Exceptions), where you have to explicitly catch exceptions or declare the method can throw, and in doing so the compiler can provide some assistance in enforcing error handling to a degree, in theory. In practice, it too easily became nothing short of a [nightmare](http://literatejava.com/exceptions/checked-exceptions-javas-biggest-mistake/) for Java developers, but that's another story.

Scala only has unchecked exceptions. This can be a problem when you're trying to transition to a more functional style, because it breaks what's called _referential transparency_ [1]: that an expression can be replaced with its value without changing prior behavior. Boiling it down succinctly: that a function always returns exactly what it says it returns given the same set of inputs with no side effects.

## An exceptional example

Here's a model program showing a simple flow of buying and making coffee (can you tell I am really into coffee?), where our functions aren't technically pure:

```scala
class Coffee
class Beans

object CoffeeService {

  val price = 3

  def purchaseCoffee(money: Int): Coffee =
    brewCoffee(buyBeans(money))

  def buyBeans(money: Int): Beans = {
    if (money < price)
      throw new Exception(s"Not enough money to buy beans for a coffee, need $price")
    else
      new Beans
  }

  def brewCoffee(beans: Beans): Coffee = {
    // simulate a faulty grinder that fails 25% of the time
    if (Math.random < 0.25)
      throw new Exception("Faulty grinder failed to grind beans!")
    else
      new Coffee
  }
}
```

In the example above, the functions `buyBeans` and `brewCoffee` effectively lie to you. In their type signatures, the function says it should always return a type given another (in the form of `A => B`), but according to their inner logic they may throw an exception. This breaks functional purity since these functions no longer always returns exactly the right type, in the case of `buyBeans`, if you don't have enough money it will throw an exception. In this case, the compiler won't tell you about a violation of functional purity by failing to compile.

Here's the hard truth if you're very used to this style of error handling but want to continue down the path of functional zen: **stop throwing exceptions in your own code**. That means not using them for logic control, 'object not found' errors, or anything like that. Granted, there will always be library code that throws exceptions, including in the standard library, but those should be explicitly handled where possible (one approach is mentioned below) and not be allowed to bubble out.

## Improving over exceptions

Why, you ask, would we want to 'improve' our code by not using exceptions? For someone coming from Java and the like, exceptions are familiar, easy to insert into code, and provide things like stack traces. What's not to like about that? Let's iron out the reasons to the contrary before diving into examples.

Our biggest reason has already been mentioned: pursuing functional purity, and therefore making it easier to substitute values as we read through code. No sneaky functions secretly masquerading as potential exceptions in disguise! Other benefits include _monadic composition_ [2] and taking advantage of Scala's for-comprehension sugar. Another effect of all this is that most of these container types also _short circuit_ upon error - when an error occurs then the rest of the downstream logic isn't executed and the result is a type containing the first error encountered. This is similar to unhandled exceptions, except now that we're using container types, the compiler will definitely complain if you don't explicitly handle it. 

The neat little conclusion is that in embracing functional composition, you can effectively linearize your code flow (with some detours when necessary) and prioritize in laying out the happy path in your code.

### Option

Let's get hands on: how do we go forward with error handling if we take away exceptions? Since this function results in either getting a back a coffee or not, one of the first things we can reach for is to use the [Option](http://www.scala-lang.org/api/current/index.html#scala.Option). It is a good first choice for exactly this case: after applying the function's logic, there's either `Some(value)` or `None`. 

Pay attention to what happened to `purchaseCoffee` - we chained `buyBeans` and `brewCoffee` together using a for-comprehension! If for some reason `buyBeans` returns a `None`, the logic will short circuit and `purchaseCoffee` will end up returning a `None` in the end, effectively bypassing `brewCoffee`. This is something we can call _biasing_, where application of a function to these container types is biased to one result over the other - in this case we bias to `Some` and the underlying `flatMap` used in the for-comprehension syntactic sugar is really only meaningful for that case.

```scala
class Coffee
class Beans

object CoffeeServiceOption {

  val price = 3

  def purchaseCoffee(money: Int): Option[Coffee] =
    for {
      beans <- buyBeans(money)
      coffee <- brewCoffee(beans)
    } yield coffee

  def buyBeans(money: Int): Option[Beans] = {
    if (money < price)
      None
    else
      Some(new Beans)
  }

  def brewCoffee(beans: Beans): Option[Coffee] = {
    // simulate a faulty grinder that fails 25% of the time
    if (Math.random < 0.25)
      None
    else
      Some(new Coffee)
  }
}
```

### Try

As previously mentioned, there's also another approach to refactoring exception-laden code: [Try](http://www.scala-lang.org/api/current/index.html#scala.util.Try). `Try` can be thought of as a container type that results in either a `Success(value)` or a `Failure(exception)`, and the creation of a `Try` is useful to wrap code which at any point may throw an exception. It may be pragmatic to drop in `Try` instead of `Option` especially if you are dealing with third party libraries or logic for which the refactoring may be too ambitious to attempt all at once. `Try`, like `Option`, is biased for the case that results in a value, `Success`.

```scala
import scala.util.Try

class Coffee
class Beans

object CoffeeServiceTry {

  val price = 3

  def purchaseCoffee(money: Int): Try[Coffee] =
    for {
      beans <- buyBeans(money)
      coffee <- brewCoffee(beans)
    } yield coffee

  def buyBeans(money: Int): Try[Beans] = {
    Try {
      if (money < price)
        throw new Exception(s"Not enough money to buy beans for a coffee, need $price")
      else
        new Beans
    }
  }

  def brewCoffee(beans: Beans): Try[Coffee] = {
    Try {
      // simulate a faulty grinder that fails 25% of the time
      if (Math.random < 0.25)
        throw new Exception("Faulty grinder failed to grind beans!")
      else
        new Coffee
    }
  }
}
```

### Either

There are some minor problems with `Option` and `Try`, however: `Option` doesn't necessarily provide you enough context as to _why_ a computation failed and `Try`'s failure mechanism still relies on exceptions, which we've assumed to disavow. Enter [Either](http://www.scala-lang.org/api/current/index.html#scala.util.Either), which represents the disjoint union of two types (either type A or type B) and may be more useful for us to provide context for failure without an exception: one type can represent a success result and the other can represent a failure or error. 

An important detail to note is that `Either` does not have the `map` and `flatMap` methods nor is biased towards a side. `Left` is adopted by _convention_ to hold an error while `Right` is assumed as the value itself. Instead, we can force a right-bias by calling the `.right` projection on these `Either`s, but that probably violates your DRYness sensibilities, no? We can improve yet further with alternatives from outside the standard library.

```scala
import scala.util.{ Either, Left, Right }

class Coffee
class Beans
case class FailureReason(reason: String)

object CoffeeServiceEither {

  val price = 3

  def purchaseCoffee(money: Int): Either[FailureReason, Coffee] =
    for {
      beans <- buyBeans(money).right
      coffee <- brewCoffee(beans).right
    } yield coffee

  def buyBeans(money: Int): Either[FailureReason, Beans] = {
    if (money < price)
      Left(FailureReason(s"Not enough money to buy beans for a coffee, need $price"))
    else
      Right(new Beans)
  }

  def brewCoffee(beans: Beans): Either[FailureReason, Coffee] = {
    // simulate a faulty grinder that fails 25% of the time
    if (Math.random < 0.25)
      Left(FailureReason("Faulty grinder failed to grind beans!"))
    else
      Right(new Coffee)
  }
}
```

## Library Alternatives

In the Scala ecosystem, there are some alternative error handling container types, the ones described below are isomorphic (similar in form and purpose) to the standard library's `Either`, except most make a decision on biasing and provide quite a bit more methods in their definitions.

### scalaz.\/

Similar to `Either` is `scalaz.\/` (commonly pronouced as disjunction), which is also a disjoint union, except `scalaz.\/` is right-biased where the right side is considered to be the success case and the left side to be the error case. `scalaz.\/` also has additional methods not available in `Either` such as `flatMap` - which is one of the reasons we can use this directly in for-comprehensions.

```scala
import scalaz.{ \/, \/-, -\/ }

class Coffee
class Beans
case class FailureReason(reason: String)

object CoffeeServiceDisjunction {

  val price = 3

  def purchaseCoffee(money: Int): FailureReason \/ Coffee =
    for {
      beans <- buyBeans(money)
      coffee <- brewCoffee(beans)
    } yield coffee

  def buyBeans(money: Int): FailureReason \/ Beans = {
    if (money < price)
      -\/(FailureReason(s"Not enough money to buy beans for a coffee, need $price"))
    else
      \/-(new Beans)
  }

  def brewCoffee(beans: Beans): FailureReason \/ Coffee = {
    // simulate a faulty grinder that fails 25% of the time
    if (Math.random < 0.25)
      -\/(FailureReason("Faulty grinder failed to grind beans!"))
    else
      \/-(new Coffee)
  }
}
```

*Note: The `A \/ B` syntax, infix type notation, probably looks strange to you and it is! For some reason it's not very well mentioned nor seen much in mainstream Scala code, but seems to be especially prevalent in the discussion of functional error handling. In this case, `A \/ B` is sugar for `\/[A, B]`.

### org.scalactic.Or

Another disjoint union type but out of the Scalactic library, [Or](http://doc.scalatest.org/2.2.4/index.html#org.scalactic.Or) differs slightly by being left-biased and reads like "the good value or a bad value/error". Usage of `Or` with infix type parameters is encouraged since the type name then flows like natural language: the `Good` value `Or` the `Bad` value.

```scala
import org.scalactic.{ Bad, Good, Or }

class Coffee
class Beans
case class FailureReason(reason: String)

object CoffeeServiceOr {

  val price = 3

  def purchaseCoffee(money: Int): Coffee Or FailureReason =
    for {
      beans <- buyBeans(money)
      coffee <- brewCoffee(beans)
    } yield coffee

  def buyBeans(money: Int): Beans Or FailureReason = {
    if (money < price)
      Bad(FailureReason(s"Not enough money to buy beans for a coffee, need $price"))
    else
      Good(new Beans)
  }

  def brewCoffee(beans: Beans): Coffee Or FailureReason = {
    // simulate a faulty grinder that fails 25% of the time
    if (Math.random < 0.25)
      Bad(FailureReason("Faulty grinder failed to grind beans!"))
    else
      Good(new Coffee)
  }
}
```

### cats.data.Xor

Yet another disjoint union type isomorphic to the rest of these, [Xor](http://non.github.io/cats/api/#cats.data.Xor) comes from the promisingly nascent [Cats](https://github.com/non/cats) typeclass library for Scala. Cats is still under heavy initial development and can best be currently seen as a something to keep an eye on, but nonetheless is worthwhile mentioning due to the buzz around it. `Xor` is right-biased and also benefits from being written out in infix notation.

```scala
import cats.data.Xor
import cats.data.Xor.{ left, right }

class Coffee
class Beans
case class FailureReason(reason: String)

object CoffeeServiceXor {

  val price = 3

  def purchaseCoffee(money: Int): FailureReason Xor Coffee =
    for {
      beans <- buyBeans(money)
      coffee <- brewCoffee(beans)
    } yield coffee

  def buyBeans(money: Int): FailureReason Xor Beans = {
    if (money < price)
      left(FailureReason(s"Not enough money to buy beans for a coffee, need $price"))
    else
      right(new Beans)
  }

  def brewCoffee(beans: Beans): FailureReason Xor Coffee = {
    // simulate a faulty grinder that fails 25% of the time
    if (Math.random < 0.25)
      left(FailureReason("Faulty grinder failed to grind beans!"))
    else
      right(new Coffee)
  }
}
```

## Caveats

As with many engineering decisions, there are tradeoffs in choosing one way or another - functional error handling is no exception. The payoffs are well worth it, but it is also prudent to mention some things that should be taken into consideration:

- Library dependencies on `scalaz`, `scalactic`, or `cats` - not a trivial decision. These are large libraries which your team may not be ready for. Consider carefully if you want to go outside the standard library.
- Possible performance hits - admittedly, this is an area where I have little expertise (but do I smell a possible follow-up post on benchmarking this?), but do keep in mind that in evolving from Java-like code to more functional code there may be performance hits, e.g. more allocations for all these little types which may lead to GC issues.
- **Mind your boundaries** - if you're writing a library you may be exposing clients (or consumers, or colleagues) to something they've never seen before that may turn them off immediately: consider providing bridging-the-gap conveniences for handling these types! Usability goes a long way for everyone.

## What about accumulating errors?

As mentioned before, all these error handling types _fail fast_, so there's no room for accumulating errors along a group of functions. In a future post we'll go over mechanisms for accumulating errors, some of which will look familar, and some look _very_ different from what someone coming from Java has ever seen.

## Further Reading

There are also other great resources on the topic specifically for Scala that I wholly recommend to read:

- [_Functional Programming in Scala_](http://www.manning.com/bjarnason/), Chapter 4: "Handling errors without exceptions" by Paul Chiusano and Rúnar Bjarnason
- [How do I error handle thee?](http://typelevel.org/blog/2014/02/21/error-handling.html) by Adelbert Chang
- [Error Handling Without Throwing Your Hands Up](http://underscore.io/blog/posts/2015/02/13/error-handling-without-throwing-your-hands-up.html) by Jonathan Ferguson
- [Designing Fail-Fast Error Handling](http://underscore.io/blog/posts/2015/02/23/designing-fail-fast-error-handling.html) by Noel Welsh
- [A short StackOverflow explainer on monads in Scala](http://stackoverflow.com/questions/25361203/what-exactly-makes-option-a-monad-in-scala/25361305#25361305)

---

[1] _Or purity, whatever. Before you break out the "well, actually...", see a [discussion on the terms _purity_ vs. _referential transparency_](http://stackoverflow.com/questions/4865616/purity-vs-referential-transparency), and for the purpose of this post I may interchange both._

[2] _Monads and monadic composition (function composition) are a hard thing to explain well, I like to boil it down to being boxes for values that come with built-in behavior. [Functors, Applicatives, And Monads In Pictures](http://adit.io/posts/2013-04-17-functors,_applicatives,_and_monads_in_pictures.html) is so far my favorite explainer, complete with pictures!_
