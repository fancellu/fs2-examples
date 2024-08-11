ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.2"

lazy val root = (project in file("."))
  .settings(
    name := "fs2stuff"
  )

libraryDependencies += "co.fs2" %% "fs2-core" % "3.10.2"
libraryDependencies += "co.fs2" %% "fs2-io" % "3.10.2"
