name := "merseyside"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.yaml" % "snakeyaml" % "1.15"
)

mappings in Universal ++= (baseDirectory.value / "_resume" * "*" get) map
  (x => x -> ("_resume/" + x.getName))

mappings in Universal ++= (baseDirectory.value / "_posts" * "*" get) map
  (x => x -> ("_posts/" + x.getName))
