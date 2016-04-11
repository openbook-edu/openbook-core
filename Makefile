VERSION_FILE=VERSION
VERSION=`cat $(VERSION_FILE)`

all:
	sbt clean compile dist

clean:
	rm -rf target
	rm -rf project/target
	rm -rf project/project

test:
	sbt test:compile test:scalastyle test

compile:
	sbt compile scalastyle

dist:
	sbt dist

publishLocal:
	sbt publishLocal

publish:
	sbt publish

deploy:
	sbt clean compile test publish
