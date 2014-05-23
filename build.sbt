name := "merseyside"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  "com.typesafe.slick" %% "slick" % "2.0.2",
  "mysql" % "mysql-connector-java" % "5.1.30",
  "ws.securesocial" %% "securesocial" % "2.1.3",
  "com.github.tototoshi" %% "slick-joda-mapper" % "1.1.0"
)

play.Project.playScalaSettings
