---
title: "End-to-End Reactive Streams with Slick, Akka Streams, and elastic4s"
tags: ["scala", "slick", "postgresql", "reactive streams", "elastic4s", "akka streams", "functional programming"]
description: "A walkthrough of streaming data from PostgreSQL to Elasticsearch with reactive streams and Scala."
---

_Code for this blog post can be found on GitHub at: [https://github.com/longcao/reactive-streams-end-to-end-example](https://github.com/longcao/reactive-streams-end-to-end-example)._

_This post is also cross-posted to my company's engineering blog at: [https://www.reonomy.com/reactive-streams-end-to-end-with-slick-akka-streams-and-elastic4s/](https://www.reonomy.com/reactive-streams-end-to-end-with-slick-akka-streams-and-elastic4s/).

## Introduction: Reactive Streams

If you've had your ear to the ground in JVM land lately, especially in Scala, you may have heard the ✨_reactive_✨ buzzword being tossed around like it's the panacea to all your ills (read: it isn't).

Past the marketing hype, however, is a very real idea that may serve to solve your very real problems - the [Reactive Streams](http://www.reactive-streams.org/) initiative defines a interoperating specification for what is essentially dual-channel streaming: one channel for pushing elements of data, and another to propagate back demand signals to upstream senders. An analogy I like to think of in my head is like plumbing: elements of your data are like water molecules, and there is a possibility that you can break your pipes or systems if you were to pump too much water too fast through them, ergo the need for a _backpressure_ mechanism which exists in the physical world as fluid pressure. 

This, in combination with libraries that implement the specification, allows you to write your applications against a streaming standard where you focus on _what_ you want to do with your stream of elements without having to drop down too much into the gritty details of _how_ to stream. The motivation behind a JVM-wide standard becomes more clear when more and more libraries provide bindings for the specification: I, as a user of these libraries, am able to fit the pipes together even across JVM libraries for streaming without having to write adapters for a lower level streaming protocol between them myself.

While it's been over a year since the release `1.0.0` of the reactive-streams spec, it still feels early in the lifetime of the reactive streams initiative. However, there are parts and libraries out there that you can cobble together today to do something of value. So, if you don't consider yourself an early-ish adopter who's okay with the myriad problems that may arise from that (breaking APIs, unoptimized performance, and other bugs), take this walkthrough with a grain of salt!

## Sample Problem: Bulk Indexing Data

![ironic big data streaming image](http://i.imgur.com/lTGm5gA.png)

_I like ironic images of "big data", like this stream of meaningless binary digits._

Before I start go too far into describing a solution looking for a problem, let's step back to outline an example scenario we need to architect. For example, a common situation in software is to store canonical data in a reliable RDBMS like [PostgreSQL](http://www.postgresql.org/), which is great for most intents and purposes, like when you need SQL and your data isn't huge. Supplementing this would be some kind of specialized search index that serves as a view upon that canonical data for when tradeoffs need to be made with speed, full-text search, and SQL.

In a concrete case, let's outline some specifications for our problem, which is to bulk populate a search index from an RDBMS table:

* We have a table in PostgreSQL populated with [NYC taxi trip data](http://www.nyc.gov/html/tlc/html/about/trip_record_data.shtml) that needs to be indexed in [Elasticsearch](https://www.elastic.co/products/elasticsearch) for nebulously defined search reasons
* We _can't_ assume we can hold the entire table in memory
* We have some transformations/enriching of table records that need to be executed on the fly
* Indexing should be reasonably fast (and tunable) without overwhelming the Elasticsearch cluster

You can probably guess that the prescribed solution for this simple case is to _stream_ the table records from PostgreSQL to Elasticsearch as our bulk indexing operation.

## First Thing's First: Load Into PostgreSQL

First thing's first: let's populate our database table with a subset of NYC taxi ride data. I've done the legwork of defining an SQL schema in `create-schema.sql` here:

```sql
CREATE TABLE nyc_taxi_data (
  vendor_id integer NOT NULL,
  tpep_pickup_datetime timestamp WITHOUT TIME ZONE NOT NULL,
  tpep_dropoff_datetime timestamp WITHOUT TIME ZONE NOT NULL,
  passenger_count integer NOT NULL,
  trip_distance numeric NOT NULL,
  pickup_longitude numeric NOT NULL,
  pickup_latitude numeric NOT NULL,
  rate_code_id integer NOT NULL,
  store_and_fwd_flag boolean NOT NULL,
  dropoff_longitude numeric NOT NULL,
  dropoff_latitude numeric NOT NULL,
  payment_type integer NOT NULL,
  fare_amount numeric NOT NULL,
  extra numeric NOT NULL,
  mta_tax numeric NOT NULL,
  tip_amount numeric NOT NULL,
  tolls_amount numeric NOT NULL,
  improvement_surcharge numeric NOT NULL,
  total_amount numeric NOT NULL
);
```

We'll grab the [December 2015](https://storage.googleapis.com/tlc-trip-data/2015/yellow_tripdata_2015-12.csv) subset (approx. 11.5M rows) of NYC yellow cab ride data, and load it into PostgreSQL, assuming your `psql` client is already properly configured:

`load.sql`:
```sql
\COPY nyc_taxi_data FROM 'taxi_data.csv' WITH ( FORMAT CSV, HEADER true )
```

`load.sh`:
```sh
#!/bin/sh

# get some taxi data - January 2015
printf "\n-----> Download some sample data, NYC yellow cab data (Dec 2015)...\n\n"
curl -o taxi_data.csv 'https://storage.googleapis.com/tlc-trip-data/2015/yellow_tripdata_2015-12.csv'

# create the schema
printf "\n-----> Create 'nyc_taxi_data' table...\n\n"
psql -f create-schema.sql

# load the data with COPY
printf "\n-----> Loading data...\n\n"
psql -f load.sql
```

As you can see, we don't do much here other than load data into the database with an appropriate schema.

## Setting Up Dependencies

Before we start with the Scala code, add these required dependencies to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.2.0",
  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.2.0",
  "com.typesafe.akka" %% "akka-stream" % "2.4.2",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "com.github.tminglei" %% "slick-pg" % "0.11.3",
  "com.github.tminglei" %% "slick-pg_date2" % "0.11.3"
)
```

These will all be used in our next few steps.

#### Setting Up Slick

Next, we'll create a basic setup for [Slick](http://slick.typesafe.com/), a Scala library for database access, and [slick-pg](https://github.com/tminglei/slick-pg), a library with PostgreSQL-specific extensions for Slick, which we'll only lightly need for more seamless date support.

I'm eliding some details like getting set up with credentials, but it should become apparent where to plug things in:

```scala
import com.github.tminglei.slickpg._

// Create a custom driver that mixes in extended behavior on
// top of the default Slick PostgreSQL driver 
object CustomPostgresDriver extends ExPostgresDriver
  with PgDate2Support {

  override val api = ExtendedAPI

  object ExtendedAPI extends API
    with Date2DateTimePlainImplicits
}

import CustomPostgresDriver.api._

object Db {
  lazy val db = Database.forURL(
    url = s"jdbc:postgresql://$PGHOST:$PGPORT/$PGDATABASE",
    user = PGUSER,
    password = PGPASSWORD,
    driver="org.postgresql.Driver")
}
```

## Streaming Out From Slick

Now that we have a handle for the database within our Scala code, we can begin spewing out records from the `nyc_taxi_data` table.

First, we'll model the table record with a `case class` that maps directly over the table schema:

```scala
case class TaxiRide(
  vendor_id: Int,
  tpep_pickup_datetime: LocalDateTime,
  tpep_dropoff_datetime: LocalDateTime,
  passenger_count: Int,
  trip_distance: Double,
  pickup_longitude: Double,
  pickup_latitude: Double,
  rate_code_id: Int,
  store_and_fwd_flag: Boolean,
  dropoff_longitude: Double,
  dropoff_latitude: Double,
  payment_type: Int,
  fare_amount: Double,
  extra: Double,
  mta_tax: Double,
  tip_amount: Double,
  tolls_amount: Double,
  improvement_surcharge: Double,
  total_amount: Double)
```

**Note**: It's totally not normal Scala convention to snake_case field names! It was easier to keep this going in the example code.

Now, let's give it a bit of a tap to see if we can get some materialized results. We'll choose to construct a query using the Slick query interpolator and an implicit `GetResult` instance that tells `Slick` how to read rows into our type:

```scala
import slick.jdbc.GetResult

object TaxiRide {
  implicit val getTaxiRideResult: GetResult[TaxiRide] = GetResult { r =>
    TaxiRide(
      vendor_id = r.<<,
      tpep_pickup_datetime = r.<<,
      tpep_dropoff_datetime = r.<<,
      passenger_count = r.<<,
      trip_distance = r.<<,
      pickup_longitude = r.<<,
      pickup_latitude = r.<<,
      rate_code_id = r.<<,
      store_and_fwd_flag = r.<<,
      dropoff_longitude = r.<<,
      dropoff_latitude = r.<<,
      payment_type = r.<<,
      fare_amount = r.<<,
      extra = r.<<,
      mta_tax = r.<<,
      tip_amount = r.<<,
      tolls_amount = r.<<,
      improvement_surcharge = r.<<,
      total_amount = r.<<)
  }
}

val q = sql"SELECT * FROM nyc_taxi_data LIMIT 5;".as[TaxiRide]
```

Then, we can call [`db.stream`](http://slick.typesafe.com/doc/3.1.1/api/index.html#slick.jdbc.JdbcBackend$DatabaseDef@stream[T](a:slick.dbio.DBIOAction[_,slick.dbio.Streaming[T],Nothing]):slick.backend.DatabasePublisher[T]) to get a `DatabasePublisher` reference, which doesn't begin execution until something forces it, and isn't terribly useful on its own other than for something like printing each record, which we'll do now to confirm something will come out:

```scala
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import slick.backend.DatabasePublisher

val publisher: DatabasePublisher[TaxiRide] = db.stream(q.transactionally.withStatementParameters(fetchSize = 5000))
  .foreach(tr => println(tr))

Await.result(publisher.foreach(println), Duration.Inf)

TaxiRide(2,2015-12-17T15:27:01,2015-12-17T16:06:42,1,5.0,-73.98983764648438,40.757179260253906,1,false,-73.95295715332031,40.79164123535156,2,25.5,0.0,0.5,0.0,0.0,0.3,26.3)
TaxiRide(2,2015-12-17T15:27:01,2015-12-17T15:55:15,1,7.18,-73.95632934570312,40.8036994934082,1,false,-73.87104034423828,40.77180099487305,1,24.0,0.0,0.5,6.07,5.54,0.3,36.41)
TaxiRide(2,2015-12-17T15:27:01,2015-12-17T15:28:55,5,0.48,-73.97564697265625,40.78196334838867,1,false,-73.98095703125,40.774436950683594,2,3.5,0.0,0.5,0.0,0.0,0.3,4.3)
TaxiRide(2,2015-12-17T15:27:01,2015-12-17T15:37:46,1,1.08,-73.98175811767578,40.75217056274414,1,false,-73.99471282958984,40.745338439941406,2,8.0,0.0,0.5,0.0,0.0,0.3,8.8)
TaxiRide(2,2015-12-17T15:27:01,2015-12-17T15:43:07,1,1.57,-74.00810241699219,40.72257995605469,1,false,-73.9935302734375,40.73754119873047,1,11.5,0.0,0.5,2.46,0.0,0.3,14.76)
```

As a side note, calling the `.transactionally` and `.withStatementParameters(fetchSize = 5000)` combinators on the query value is necessary to ensure that PostgreSQL pushes results as soon as it gets them, rather than waiting for completion and buffering everything in memory. After all, we _are_ streaming and one of the specifications of the problem is that the dataset may not fit into memory.

Now that we've confirmed things are working and clearing the previous working slate, let's get it to where we really want it, and introduce the next piece of the plumbing that will make the `DatabasePublisher` more useful:

```scala
import akka.actor.ActorSystem
import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

// Implicit boilerplate necessary for creating akka-streams stuff
implicit lazy val system = ActorSystem("reactive-streams-end-to-end")
implicit lazy val materializer = ActorMaterializer()

// A very large query. Loading it all into memory could result in a Bad Time.
val allTaxiDataQuery = sql"SELECT * FROM nyc_taxi_data;".as[TaxiRide]

/**
 * Create an akka-streams Source from a reactive-streams publisher,
 * entering akka-streams land where we get access to a richer API for stream element processing
 */
val taxiRidesSource: Source[TaxiRide, NotUsed] = Source.fromPublisher {
  db.stream {
    allTaxiDataQuery
      .transactionally
      .withStatementParameters(fetchSize = 5000)
  }
}
```

What we've done here is create an akka-streams `Source` from the `DatabasePublisher` - this gently slides us into akka-streams. We can think of a `Source` as a higher level abstraction for publishers of elements, in this case we can think of the `Source[TaxiRide]` as say, a _wellspring_ of `TaxiRide` elements ready to be tapped.

## Processing Elements with akka-streams `Flow`

We mentioned before that we had some processing/business logic steps to perform in flight for each element. Luckily, akka-streams provides a high level API that enables us to create processing stages out of anonymous functions: `Flow`.

Let's say one of our hard requirements is to enrich `TaxiRide` elements with some kind of `fake_description` field via an API call, meaningless on its own but used to illustrate the need for additional control constructs. The function and new type definition looks like so:

```scala
// TaxiRide but adds a "fake_description" field.
case class TaxiRideWithDescription(
  tr: TaxiRide,
  fake_description: String)

// Function that mocks some kind of API call or code that produces a Future
// Essentially adds a fake_description field to TaxiRide.
def fakeApiCall(tr: TaxiRide): Future[TaxiRideWithDescription] = {
  Future.successful {
    TaxiRideWithDescription(
      tr = tr,
      fake_description = "I'm a fake description of some sort from calling a fake Future API.")
  }
}
```

In most cases, if we were handling this `Future` ourselves it wouldn't exactly be so straight forward, but we can use `Flow.mapAsync` to take this anonymous function of shape `A => Future[B]` such that it forms a `Flow[A, B]`, thus hiding the implementation details of handling newly spawned `Future`s:

```scala
/**
 * Construct a Flow[TaxiRide] that emits TaxiRideWithDescription elements from a function
 * that returns a Future[TaxiRideWithDescription]. Parallelism of the Future-producing
 * call is controlled under the hood by the actor behind the Flow.
 */
def addFakeDescriptionFlow(f: TaxiRide => Future[TaxiRideWithDescription]): Flow[TaxiRide, TaxiRideWithDescription, NotUsed] =
  Flow[TaxiRide].mapAsync(parallelism = 5) { tr =>
    f(tr)
  }
```

And, on top of that, we should also have a `Flow` that needs to be defined to calculate the price per distance ratio of each `TaxiRide`, which we can choose to model as a later stage `Flow`:

```scala
// Fully 'enriched' TaxiRide - adds "fake_description" and "price_per_distance" fields.
case class FullyEnrichedTaxiRide(
  trwd: TaxiRideWithDescription,
  price_per_distance: Option[Double])

// function for calculating ratio of total_amount / trip_distance
def pricePerDistanceRatio(totalAmount: Double, tripDistance: Double): Option[Double] = {
  if (tripDistance <= 0d)
    None
  else
    Some(totalAmount / tripDistance)
}

/**
 * Construct a Flow[TaxiRideWithDescription] that performs a calculation given TaxiRideWithDescription elements and emits further enriched FullyEnrichedTaxiRide elements.
 */
val addPricePerDistanceRatioFlow: Flow[TaxiRideWithDescription, FullyEnrichedTaxiRide, NotUsed] =
  Flow[TaxiRideWithDescription].map { trwd =>
    val pricePerDistance = pricePerDistanceRatio(trwd.tr.total_amount, trwd.tr.trip_distance)

    FullyEnrichedTaxiRide(trwd, pricePerDistance)
  }
``` 

There's no complete reason why these `Flow`s can't be folded together, other than this is an constructed example showing you how to chain these parts together, and that the separation helps testing.

To summarize the incomplete stream pipeline we have so far:

`Source[TaxiRide] -> Flow[TaxiRide, TaxiRideWithDescription] -> Flow[TaxiRideWithDescription, FullyEnrichedTaxiRide]`

## Tying The Rest of It Together with `elastic4s`

Now that we have our source and processing pipeline complete, we need to define the terminus of the stream with `elastic4s`, namely we'll use the [`BulkIndexingSubscriber`](https://github.com/sksamuel/elastic4s/blob/2241c2a7a44a0014d401aeab764df1cd0d736c5d/elastic4s-streams/src/main/scala/com/sksamuel/elastic4s/streams/BulkIndexingSubscriber.scala) defined in the `elastic4s-streams` module - it is an implementation of a reactive streams subscriber and is able to receive our `FullyEnrichedTaxiRide` elements and bulk insert them to the Elasticsearch cluster.

Again, I will assume that you'll be able to set up credentials properly but we'll initialize a basic `ElasticClient` here:

```scala
import com.sksamuel.elastic4s.{ ElasticClient, ElasticsearchClientUri }
import com.sksamuel.elastic4s.streams.ReactiveElastic._

import org.elasticsearch.common.settings.Settings

object Elasticsearch {
  val esClient = {
    val isRemote = ESHOST != "localhost"

    val settings = Settings
      .settingsBuilder
      .put("cluster.name", ESCLUSTERNAME)
      .put("client.transport.sniff", isRemote) // don't set sniff = true if local
      .build

    ElasticClient.transport(settings, ElasticsearchClientUri(ESHOST, ESPORT.toInt))
  }
}
```

And, given that, we need to define how to map the case class to an Elasticsearch document, which `elastic4s` needs in order to index, which we can do for our own type by building a `RequestBuilder`:

```scala
import com.sksamuel.elastic4s.BulkCompatibleDefinition
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.streams.RequestBuilder

object FullyEnrichedTaxiRide {
  // EnrichedTaxiRide => Elasticsearch compatible documents
  def builder(indexName: String) = new RequestBuilder[FullyEnrichedTaxiRide] {
    def request(e: FullyEnrichedTaxiRide): BulkCompatibleDefinition = {
      index into indexName -> "taxi_rides" fields (
        "vendor_id"             -> e.trwd.tr.vendor_id,
        "tpep_pickup_datetime"  -> e.trwd.tr.tpep_pickup_datetime,
        "tpep_dropoff_datetime" -> e.trwd.tr.tpep_dropoff_datetime,
        "passenger_count"       -> e.trwd.tr.passenger_count,
        "trip_distance"         -> e.trwd.tr.trip_distance,
        "pickup_longitude"      -> e.trwd.tr.pickup_longitude,
        "pickup_latitude"       -> e.trwd.tr.pickup_latitude,
        "rate_code_id"          -> e.trwd.tr.rate_code_id,
        "store_and_fwd_flag"    -> e.trwd.tr.store_and_fwd_flag,
        "dropoff_longitude"     -> e.trwd.tr.dropoff_longitude,
        "dropoff_latitude"      -> e.trwd.tr.dropoff_latitude,
        "payment_type"          -> e.trwd.tr.payment_type,
        "fare_amount"           -> e.trwd.tr.fare_amount,
        "extra"                 -> e.trwd.tr.extra,
        "mta_tax"               -> e.trwd.tr.mta_tax,
        "tip_amount"            -> e.trwd.tr.tip_amount,
        "tolls_amount"          -> e.trwd.tr.tolls_amount,
        "improvement_surcharge" -> e.trwd.tr.improvement_surcharge,
        "total_amount"          -> e.trwd.tr.total_amount,
        "fake_description"      -> e.trwd.fake_description,
        "price_per_distance"    -> e.price_per_distance.getOrElse(null)
      )
    }
  }
}
```

And, stringing this together:

```scala
/**
 * Sink that indiscriminately tallies up and prints the count of elements it has seen.
 */
def sumElementsSink[T] = Sink.fold[Int, T](0) { (sum, _) =>
  val newSum = sum + 1
  if (newSum % 5000 == 0) {
    print(s"\rCount: $newSum")
  }
  newSum
}

/**
 * Function that wraps over some less-than-ideal Promise-using code.
 *
 * Since the Subscriber to our elements isn't able to give us a handle to a Future
 * that may eventually complete, create one by passing a callback to the elastic4s subscriber
 * that will be called when the Subscriber completes, which completes our Promise.
 *
 * This function weaves together the akka-stream pipeline so that we stream elements
 * from the Source through a pair of processing Flows, which terminate at a receiving Sink.
 */
def bulkInsertToElasticsearch: Future[Unit] = {
  val p = Promise[Unit]()

  val esSink = Sink.fromSubscriber {
    esClient.subscriber[FullyEnrichedTaxiRide](
      batchSize = 5000,
      completionFn = { () => p.success(()); ()},
      errorFn = { (t: Throwable) => p.failure(t); ()})(FullyEnrichedTaxiRide.builder("nyc-taxi-rides"), system)
  }

  taxiRidesSource                             // sequence the streaming, whose sink will complete the promise
    .via(addFakeDescriptionFlow(fakeApiCall)) // Source elements directed through this Flow
    .via(addPricePerDistanceRatioFlow)
    .alsoTo(sumElementsSink)                  // fork the stream flow to an alternate end that counts
    .runWith(esSink)                          // starts the streaming by specifying a target Sink

  p.future
}
```

There are quite a few talking points here worth mentioning, especially the `Promise`-based workaround. Due to the fact that the `Subscriber` provided by `elastic4s` isn't able to give us an asynchronous handle to when the `Subscriber` would complete the stream, we create a callback that will complete a `Future` that we create from a local `Promise` hidden from outside scope.

In a similar manner as the `Publisher + Source` wrapping we did earlier, we also wrap the `Subscriber` with a `Sink` to gain access to the higher-level plumbing of akka-streams.

We also route elements to an alternate `Sink` with `.alsoTo` - in this case a `Sink.fold` that counts and prints every 5000 elements so we can get some visual feedback of streaming progress.

Lastly, _inside_ this function itself we do what I think of as the actual pipe fitting itself - we call `.via` to fit new `Flow`s on top of the `Source`, and kickstart processing with `.runWith`, which both attaches a terminus `Sink` and runs the stream pipeline.

## The End of the World: Run and Await the Result

Now that we have the entire pipeline defined and ready to go, we can call `bulkInsertToElasticsearch` at the end of the world (the end of our app) and wait indefinitely until the entire streaming process is finished, as signaled by when the function's returned `Future` is complete. Afterwards, to be a good citizen (and because the runtime may not let us exit if we forget), we shut down the underlying akka `ActorSystem` to clean up.

```scala
// wait for the underlying Promise to be complete (the indexing to be complete)
Await.result(bulkInsertToElasticsearch, Duration.Inf)

// shut down the Akka ActorSystem nicely so we can exit
Await.result(system.terminate, Duration.Inf)
```

## Conclusion

Now that I've shown you how to weave together the code, in the [example repo](https://github.com/longcao/reactive-streams-end-to-end-example), run `sbt run` with appropriate environment variables to see it in action, and you should immediately see documents in Elasticsearch. 

After all this, the final shape of our streaming application is nearly linear, with the fork in the end to two separate `Sink`s, one for bulk inserting to Elasticsearch and the other for printing a running tally:

```
Source[TaxiRide] (from PostgreSQL) -> Flow[TaxiRide, TaxiRideWithDescription] -> Flow[TaxiRideWithDescription, FullyEnrichedTaxiRide] -> (Sink[FullyEnrichedTaxiRide] (for Elasticsearch), Sink[FullyEnrichedTaxiRide] (for printing))
```

Now, at this point you might conclude that there's quite a bit of magic under the hood to make things work - and you are completely right! There's a lot of complicated implementation underneath that enables multithreaded streaming and pipeline building involving Akka actors that is out of scope for this post, but as far as being a _user_ of Akka Streams, we're mostly insulated from the lower level details.

In any case, if you're wondering about alternatives I would be remiss to not mention [Rob Norris'](https://twitter.com/tpolecat) [doobie](https://github.com/tpolecat/doobie) and [scalaz-stream (now fs2)](https://github.com/functional-streams-for-scala/fs2), both of which can be plugged together with _purely functional_ streaming to a target you desire.

If you have any questions, feel free to toot at [@oacgnol](https://twitter.com/oacgnol), thanks for reading!

## Resources, Further Reading

* [Akka Stream 2.4.2 Documentation](http://doc.akka.io/docs/akka/2.4.2/scala/stream/index.html)
* [Slick 3.1.1 Streaming](http://slick.typesafe.com/doc/3.1.1/dbio.html#streaming)
* [elastic4s Reactive Streams](https://github.com/sksamuel/elastic4s#elastic-reactive-streams)
* [the book of doobie](http://tpolecat.github.io/doobie-0.2.3/00-index.html)
* [Introduction to scalaz-stream (now fs2)](https://gist.github.com/djspiewak/d93a9c4983f63721c41c)