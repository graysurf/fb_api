import sbt._

addSbtPlugin("org.scalariform"   % "sbt-scalariform"      % "1.6.0")
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"         % "0.14.3")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"          % "0.1.10")
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph" % "0.8.2")
val PlayVersion = "2.5.3"
addSbtPlugin("com.typesafe.play" % "sbt-plugin"           % PlayVersion)
addSbtPlugin("com.typesafe.play" % "play-docs-sbt-plugin" % PlayVersion)
addSbtPlugin("com.typesafe.play" % "sbt-fork-run-plugin"  % PlayVersion)
//addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")
//addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")
//addSbtPlugin("com.typesafe.sbt" % "sbt-uglify" % "1.0.3")
//addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
//addSbtPlugin("net.ground5hark.sbt" % "sbt-css-compress" % "0.1.3")
//addSbtPlugin("com.slidingautonomy.sbt" % "sbt-filter" % "1.0.1")