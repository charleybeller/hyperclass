import com.typesafe.sbt.SbtStartScript

name := "hyperclass"

scalaVersion := "2.10.0" 

retrieveManaged := true

seq(SbtStartScript.startScriptForClassesSettings: _*)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

scalacOptions ++= Seq(
	"-deprecation",
	"-feature",
	"-language:reflectiveCalls"
)

javaOptions += "-Xmx25G"

libraryDependencies  ++= Seq(
	"commons-lang" % "commons-lang" % "2.6",
	"org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
	"log4j" % "log4j" % "1.2.17",
	//"edu.jhu.agiga" % "agiga" % "1.0",
	"edu.stanford.nlp" % "stanford-corenlp" % "1.3.4",
	"edu.mit" % "jwi" % "2.2.3",
	"org.scalanlp" %% "breeze-math" % "0.2.3",
	"org.scalanlp" %% "breeze-learn" % "0.2.3",
	"org.scalanlp" %% "breeze-process" % "0.2.3",
	"org.scalanlp" %% "breeze-viz" % "0.2.3",
	"de.bwaldvogel" % "liblinear" % "1.92"
)

resolvers ++= Seq(
    	"Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)





