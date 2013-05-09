name := "hyperclass"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.0" 

retrieveManaged := true

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

libraryDependencies += "log4j" % "log4j" % "1.2.17"

//libraryDependencies += "edu.jhu.agiga" % "agiga" % "1.0"

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "1.3.4"

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "1.3.4"

libraryDependencies += "edu.mit.jwi" % "jwi" % "2.2.3"

scalacOptions += "-deprecation"

scalacOptions += "-feature"

scalacOptions += "-language:reflectiveCalls"



//  "edu.jhu.agiga" % "agiga" % "1.0",
//  "edu.jhu.coe.cale" % "cale" % "1.0",
//  "flanagan" % "flanagan" % "3.0",
//  "ac.biu.nlp.normalization" % "normalization" % "0.6.1",
//  "edu.jhu.coe.coecoref" % "coecoref" % "1.0",






