---
title: "Functional Error Accumulation in Scala"
tags: ["scala", "functional programming", "error handling", "error accumulation", "validation"]
description: "An intro to functional error accumulation/validation in Scala."
---

In a [previous post](http://longcao.org/2015/06/15/easing-into-functional-error-handling-in-scala), we dipped into what it means to handle application errors functionally in Scala and took a brief overview of the types that can be used to accomplish that. A common theme with those error handling types like `Try` or the disjunctions is that they all _fail fast_: only the first error in a purely functional call chain is returned. 

Let's keep the functional hype train going and explore more of the goodies that we can bring into our application logic. Stepping back from the fail-fast error handling discussion, let's consider something else: what if we wanted to execute _all_ functions applied to an input and report back any and all possible errors? In other words:

- I have a bag of functions that I want to apply to inputs that I feed them
- While I know that some of them can and will fail, none of them are showstoppers
- I also want a nice little bow-tied package in the end that gives me everything I could want to know: the possible errors or the result type.

## Another Coffee-based Example

[![Coffee.](http://i.imgur.com/OlCG2NC.jpg)](https://instagram.com/p/xxRQEpPuqx)

As I'm drinking a coffee in a [coffee shop](https://foursquare.com/v/devoci%C3%B3n/544fce47498e2fee8be84dd2) while I write this, let's reuse my favorite example domain, coffee, to sketch out what we might want to do.

Let's say that I'm a coffee shop owner and I want my shop to be known for having the highest quality coffee in the city, an accolade only possible by sourcing the best of roasted beans to use. Since I have an elite sense of discernment toward coffee quality, I want to apply my expertise towards evaluating the roasts I want to buy for my shop. Since I want this process to be repeatable, I will attempt to look at each characteristic of a roast objectively and independently - I want to _validate_ that the beans I'm looking at pass my standards.

Therein lies the basis for an example: the characteristics I'm looking to check against are the individual pieces of logic that I will use to evaluate a roast, and a select roast can fail none, some, or all of the logic. For the sake of this blog post [1], here's a vastly simplified list of coffee-evaluating functions:

- `evaluateDarkness` - fails if the roast is too extreme (too dark or too light)
- `evaluateFreshness` - fails if the roast isn't fresh enough
- `evaluateEvenness` - fails if the roast is uneven (different shades of color indicate uneven roast)

Without giving any thought yet, let's start by implementing these functions in only the Scala standard library (all code samples are intended to be fully compilable upon copy/paste and with any jar dependencies in scope):

```scala
import org.joda.time.LocalDate

import scala.util.{ Either, Left, Right }

sealed abstract class RoastLevel(val value: Int)
object RoastLevel {
  case object VeryLight extends RoastLevel(1)
  case object Light     extends RoastLevel(2)
  case object Medium    extends RoastLevel(3)
  case object Dark      extends RoastLevel(4)
  case object Burnt     extends RoastLevel(5)
}

trait Roast {
  def level: RoastLevel
  def date: LocalDate
  def isEven: Boolean
}
case class UnevaluatedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast
case class ApprovedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast

case class RoastProblem(reason: String)

object RoastEvaluation {
  def evaluateRoastLevel(roastLevel: RoastLevel): Option[RoastProblem] = {
    if (roastLevel.value > 2)
      None
    else
      Some(RoastProblem(s"roast too light, at a ${roastLevel.value}"))
  }

  def evaluateFreshness(roastDate: LocalDate): Option[RoastProblem] = {
    if (roastDate.isAfter(LocalDate.now.minusDays(3)))
      None
    else
      Some(RoastProblem(s"not fresh, roast date ${roastDate} is more than 3 days old"))
  }

  def evaluateEvenness(roastIsEven: Boolean): Option[RoastProblem] = {
    if (roastIsEven)
      None
    else
      Some(RoastProblem("roast is not evenly distributed"))
  }

  def evaluateRoast(roast: Roast): Either[List[RoastProblem], ApprovedRoast] = {
    val problems = List(
      evaluateRoastLevel(roast.level),
      evaluateFreshness(roast.date),
      evaluateEvenness(roast.isEven)).flatten

    if (problems.isEmpty)
      Right(ApprovedRoast(roast.level, roast.date, roast.isEven))
    else
      Left(problems)
  }
}
```

Our evaluation function `evaluateRoast` returns a pretty clear container type: either I should get back a list of problems with the roast, or the checks pass and I get back an instance of the approved roast. By `flatten`ing a `List[Option[_]]`, I get back only a `List` of the `Option`s that were `Some`.

## Taking It Further With Error Accumulating Types

For some, the previous example may already do most of the job: we're using the Scala standard library to roll up errors as we execute functions and we're returning a container type that can contain either a collection of errors or the result type we want. However, if we want to arbitrarily tack on more error accumulating functions with less of the manual labor of doing so, there are more attractive alternatives.

### cats.data.Validated

In the following example using [cats.data.Validated](http://typelevel.org/cats/api/#cats.data.Validated), we are doing the same validation logic except we return `ValidatedNel[RoastProblem, A]` instances, which is a type alias for `Validated[NonEmptyList[RoastProblem], A]` - it collects errors in a list that cannot be empty, thus if you have an `Invalid` you know you have at least one error.

```scala
import cats.data.Validated.{ invalidNel, valid }
import cats.data.{ NonEmptyList, ValidatedNel }
import cats.implicits._

import org.joda.time.LocalDate

sealed abstract class RoastLevel(val value: Int)
object RoastLevel {
  case object VeryLight extends RoastLevel(1)
  case object Light     extends RoastLevel(2)
  case object Medium    extends RoastLevel(3)
  case object Dark      extends RoastLevel(4)
  case object Burnt     extends RoastLevel(5)
}

trait Roast {
  def level: RoastLevel
  def date: LocalDate
  def isEven: Boolean
}
case class UnevaluatedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast
case class ApprovedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast

case class RoastProblem(reason: String)

object RoastEvaluationValidated {
  def evaluateRoastLevel(roastLevel: RoastLevel): ValidatedNel[RoastProblem, RoastLevel] = {
    if (roastLevel.value > 2)
      valid(roastLevel)
    else
      invalidNel(RoastProblem(s"roast too light, at a ${roastLevel.value}"))
  }

  def evaluateFreshness(roastDate: LocalDate): ValidatedNel[RoastProblem, LocalDate] = {
    if (roastDate.isAfter(LocalDate.now.minusDays(3)))
      valid(roastDate)
    else
      invalidNel(RoastProblem(s"not fresh, roast date ${roastDate} is more than 3 days old"))
  }

  def evaluateEvenness(roastIsEven: Boolean): ValidatedNel[RoastProblem, Boolean] = {
    if (roastIsEven)
      valid(true)
    else
      invalidNel(RoastProblem("roast is not evenly distributed"))
  }

  def evaluateRoast(roast: Roast): ValidatedNel[RoastProblem, ApprovedRoast] = {
    val ab = evaluateRoastLevel(roast.level) |@| evaluateFreshness(roast.date) |@| evaluateEvenness(roast.isEven)

    (ab) map { (roastLevel: RoastLevel, date: LocalDate, isEven: Boolean) =>
      ApprovedRoast(roastLevel, date, isEven)
    }
  }
}
```

This example is conceptually quite a bit more complex, there's a few pieces to unpack to mostly understand what's going on in the bigger picture. In this example, each function returns a `Validated` instance which can essentially be smushed together by what's called the home alone operator - `|@|`. Then, we're able to `map` the "good" values coming out of those individual roast property validators to the ultimate return type we want - an `ApprovedRoast` ready to be brewed. The end result will be either a list of problems with the roast, or an `ApprovedRoast` that passed our stringent coffee requirements.

The example code is made possible through a combination of things, and at risk of handwaving a bit too much I'll mention them anyways:

- `Validated` is an _applicative functor_ [2], which has properties that allow you to independently run these validations yet still combine the returned results for either accumulated errors or a nice return type.
- The implicits in `cats.implicits._` enables you the `|@|` syntax for applicative building.

### A similar option: scalaz.Validation

As with `Validated`, we can write our code using `Validation` in the same manner with only minor adjustments to function names. Everything said about `Validated` applies here since they are nearly the same.

```scala
import org.joda.time.LocalDate

import scalaz.syntax.apply._
import scalaz.Validation._
import scalaz.ValidationNel

sealed abstract class RoastLevel(val value: Int)
object RoastLevel {
  case object VeryLight extends RoastLevel(1)
  case object Light     extends RoastLevel(2)
  case object Medium    extends RoastLevel(3)
  case object Dark      extends RoastLevel(4)
  case object Burnt     extends RoastLevel(5)
}

trait Roast {
  def level: RoastLevel
  def date: LocalDate
  def isEven: Boolean
}
case class UnevaluatedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast
case class ApprovedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast

case class RoastProblem(reason: String)

object RoastEvaluationValidation {
  def evaluateRoastLevel(roastLevel: RoastLevel): ValidationNel[RoastProblem, RoastLevel] = {
    if (roastLevel.value > 2)
      success(roastLevel)
    else
      failureNel(RoastProblem(s"roast too light, at a ${roastLevel.value}"))
  }

  def evaluateFreshness(roastDate: LocalDate): ValidationNel[RoastProblem, LocalDate] = {
    if (roastDate.isAfter(LocalDate.now.minusDays(3)))
      success(roastDate)
    else
      failureNel(RoastProblem(s"not fresh, roast date ${roastDate} is more than 3 days old"))
  }

  def evaluateEvenness(roastIsEven: Boolean): ValidationNel[RoastProblem, Boolean] = {
    if (roastIsEven)
      success(true)
    else
      failureNel(RoastProblem("roast is not evenly distributed"))
  }

  def evaluateRoast(roast: Roast): ValidationNel[RoastProblem, ApprovedRoast] = {
    val ab = evaluateRoastLevel(roast.level) |@| evaluateFreshness(roast.date) |@| evaluateEvenness(roast.isEven)

    (ab) { (roastLevel: RoastLevel, date: LocalDate, isEven: Boolean) =>
      ApprovedRoast(roastLevel, date, isEven)
    }
  }
}
```

### The return of org.scalactic.Or

Returning from the last blog post is `org.scalactic.Or`: it turns out that the written language-friendly container type from Scalactic can also handle error accumulation.

```scala
import org.joda.time.LocalDate

import org.scalactic.Accumulation._
import org.scalactic.{ Every, One }
import org.scalactic.{ Bad, Good, Or }

sealed abstract class RoastLevel(val value: Int)
object RoastLevel {
  case object VeryLight extends RoastLevel(1)
  case object Light     extends RoastLevel(2)
  case object Medium    extends RoastLevel(3)
  case object Dark      extends RoastLevel(4)
  case object Burnt     extends RoastLevel(5)
}

trait Roast {
  def level: RoastLevel
  def date: LocalDate
  def isEven: Boolean
}
case class UnevaluatedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast
case class ApprovedRoast(level: RoastLevel, date: LocalDate, isEven: Boolean) extends Roast

case class RoastProblem(reason: String)

object RoastEvaluationOr {
  def evaluateRoastLevel(roastLevel: RoastLevel): RoastLevel Or Every[RoastProblem] = {
    if (roastLevel.value > 2)
      Good(roastLevel)
    else
      Bad(One(RoastProblem(s"roast too light, at a ${roastLevel.value}")))
  }

  def evaluateFreshness(roastDate: LocalDate): LocalDate Or Every[RoastProblem] = {
    if (roastDate.isAfter(LocalDate.now.minusDays(3)))
      Good(roastDate)
    else
      Bad(One(RoastProblem(s"not fresh, roast date ${roastDate} is more than 3 days old")))
  }

  def evaluateEvenness(roastIsEven: Boolean): Boolean Or Every[RoastProblem] = {
    if (roastIsEven)
      Good(true)
    else
      Bad(One(RoastProblem("roast is not evenly distributed")))
  }

  def evaluateRoast(roast: Roast): ApprovedRoast Or Every[RoastProblem] = {
    val roastLevel = evaluateRoastLevel(roast.level)
    val date = evaluateFreshness(roast.date)
    val isEven = evaluateEvenness(roast.isEven)

    withGood(roastLevel, date, isEven) { (rl, f, e) =>
      ApprovedRoast(rl, f, e)
    }
  }
}
```

The accumulation is enabled through the notion of using its `Every` non-empty collection, much like `NonEmptyList` is to cats and scalaz. Instead of having to go through the exercise of applicative building, Scalactic provides an `Accumulation.withGood` function that returns you either the `Good` result or the `Bad` result with every error rolled up within. A side benefit of using this type is being able to switch between fail-fast and accumulation with less overall refactoring if you wish - the right side of `Or` is purposefully built to handle both.

## Wrapping Up

In the end, we can again see the benefits (subject to your own opinion, of course) of using more advanced types despite the more complex machinery underneath: the return types more accurately reflect the computations that are going on and enforce a tighter contract. The mentioned error accumulating types present an advantage in being able to mix and match validation functions _and_ continue chaining more validations. Since I return a type containing either a collection of errors or a result, I can take that same type and run them through some more functions that continue validation. Tying it back to the example, I can take my approved roast and apply more validation functions to further scrutinize, grade, or otherwise classify roasts.

Hopefully I haven't tired out the coffee analogy too much for you. Until next time!

## Further Reading

As always, a few other people have written about the same topic, check them out as well!

- [herding cats - Validated datatype](http://eed3si9n.com/herding-cats/Validated.html) by Eugene Yokota
- [An Introduction to Cats](http://underscore.io/blog/posts/2015/06/10/an-introduction-to-cats.html) by Noel Welsh
- [How do I error handle thee?](http://typelevel.org/blog/2014/02/21/error-handling.html) by Adelbert Chang

---

[1] The SCAA defines a [coffee cupping protocol](https://www.scaa.org/?page=resources&d=cupping-protocols) that's fascinatingly thorough and has way more nuance than can be described in a programming blog post.
[2] [This StackOverflow answer](http://stackoverflow.com/a/12309023) does a much better job contextualizing the 'why' of an applicative functor using `scalaz.Validation`.

_Shoutout to [@tixxit](https://twitter.com/tixxit) and [@meestaveesa](https://twitter.com/meestaveesa) for help on writing this blog post._
