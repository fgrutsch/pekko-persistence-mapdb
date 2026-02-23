import sbt._

object Dependencies {

  object Versions {
    val pekko = "1.4.0"
  }

  val core: Seq[ModuleID] = Seq(
    "ch.qos.logback"    % "logback-classic"             % "1.5.32"       % Test,
    "org.apache.pekko" %% "pekko-persistence"           % Versions.pekko,
    "org.apache.pekko" %% "pekko-persistence-query"     % Versions.pekko,
    "org.apache.pekko" %% "pekko-slf4j"                 % Versions.pekko,
    "org.apache.pekko" %% "pekko-stream-testkit"        % Versions.pekko % Test,
    "org.apache.pekko" %% "pekko-persistence-tck"       % Versions.pekko % Test,
    "org.apache.pekko" %% "pekko-serialization-jackson" % Versions.pekko % Test,
    "org.mapdb"         % "mapdb"                       % "3.1.0",
    "org.scalatest"    %% "scalatest"                   % "3.2.19"       % Test
  )

  val betterMonadicFor: ModuleID = "com.olegpy" %% "better-monadic-for" % "0.3.1"

}
