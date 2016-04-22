name := "merseyside"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.yaml" % "snakeyaml" % "1.16"
)

// so `play stage` also picks up _folders
mappings in Universal ++= (baseDirectory.value / "_resume" * "*" get) map
  (x => x -> ("_resume/" + x.getName))

mappings in Universal ++= (baseDirectory.value / "_posts" * "*" get) map
  (x => x -> ("_posts/" + x.getName))

// add _posts directory so play autoreload picks it up
unmanagedResourceDirectories in Compile += baseDirectory.value / "_posts"
