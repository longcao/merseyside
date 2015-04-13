name := "merseyside"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

mappings in Universal ++= (baseDirectory.value / "_resume" * "*" get) map
  (x => x -> ("_resume/" + x.getName))

