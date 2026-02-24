# pekko-persistence-mapdb

[![Maven](https://img.shields.io/maven-central/v/io.github.fgrutsch/pekko-persistence-mapdb_2.13?logo=Apache%20Maven&style=for-the-badge)](https://search.maven.org/search?q=g:%22io.github.fgrutsch%22%20AND%20a:%22pekko-persistence-mapdb_2.13%22)
[![Github Actions CI Workflow](https://img.shields.io/github/actions/workflow/status/fgrutsch/pekko-persistence-mapdb/ci.yml?logo=Github&style=for-the-badge)](https://github.com/fgrutsch/pekko-persistence-mapdb/actions/workflows/ci.yml?query=branch%3Amain)
[![Codecov](https://img.shields.io/codecov/c/github/fgrutsch/pekko-persistence-mapdb/main?logo=Codecov&style=for-the-badge)](https://codecov.io/gh/fgrutsch/pekko-persistence-mapdb)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)

pekko-persistence-mapdb is a plugin for [pekko-persistence](https://pekko.apache.org/docs/pekko/1.1/typed/index-persistence.html) which uses MapDB for storing journal and snapshot messages.

## Getting Started

Add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "io.github.fgrutsch" %% "pekko-persistence-mapdb" % "<latest>"
```

Add the following to your `application.conf` to use pekko-persistence-mapdb as the persistence backend:

```
pekko {
  persistence {
    journal {
      plugin = "mapdb-journal"
    }
    snapshot-store {
      plugin = "mapdb-snapshot"
    }
  }
}
```

This is the minimum required configuration you need to use `pekko-persistence-mapdb`. No further configuration is needed to get it running. Be aware that this by default stores data in memory, check out the full documentation on how to change that.

If you need to query the stored events you can use [Persistence Query](https://pekko.apache.org/docs/pekko/1.1/persistence-query.html) to stream them from the journal. All queries are supported:

```scala
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.query.PersistenceQuery
import io.github.fgrutsch.pekko.persistence.mapdb.query.scaladsl.MapDbReadJournal

val actorSystem: ActorSystem = ???
val readJournal: MapDbReadJournal = PersistenceQuery(actorSystem).readJournalFor[MapDbReadJournal](MapDbReadJournal.Identifier)
```

## Documentation

For the full documentation please check [this](https://pekko-persistence-mapdb.fgrutsch.com) link.

## Credits

The code of this project is heavily inspired and based on the [akka-persistence-jdbc](https://github.com/akka/akka-persistence-jdbc) backend implementation. Go check it out!

## Contributors

* [Fabian Grutsch](https://github.com/fgrutsch)

## License

This code is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0.txt).
