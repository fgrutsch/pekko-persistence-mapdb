# Query

If you need to query the stored events you can use [Persistence Query](https://pekko.apache.org/docs/pekko/1.1/persistence-query.html) to stream them from the journal (Scala and Java supported).

Get read journal in Scala:

```scala
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.query.PersistenceQuery
import com.fgrutsch.akka.persistence.mapdb.query.scaladsl.MapDbReadJournal

val actorSystem: ActorSystem        = ???
val readJournal: MapDbReadJournal   = PersistenceQuery(actorSystem)
                                       .readJournalFor[MapDbReadJournal](MapDbReadJournal.Identifier)
```

Get read journal in Java:

```java
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.query.PersistenceQuery
import com.fgrutsch.akka.persistence.mapdb.query.javadsl.MapDbReadJournal

final ActorSystem actorSystem         = ???
final MapDbReadJournal readJournal    = PersistenceQuery.get(actorSystem)
                                        .getReadJournalFor[MapDbReadJournal](MapDbReadJournal.Identifier)
```


All query operations are supported:

* Get persistenceIds (current)
* Get persistenceIds (live)
* Get events by persistenceId (current)
* Get events by persistenceId (live)
* Get events by tag (current)
* Get events by tag (live)


## Configuration

The internal behavior of the read journal queries can be changed (e.g. refresh interval). For instructions please check the @ref:[Configuration](configuration.md) page on how to change that.

## Event Adapters

When reading events from the journal the `MapDbReadJournal` applies configured [events adapters](https://pekko.apache.org/docs/pekko/1.1/typed/persistence.html#event-adapters) thay you may have configured in the @ref:[write plugin](journal-snapshots.md#tagging) (under the `mapdb-journal` configuration key).
