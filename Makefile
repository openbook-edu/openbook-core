VERSION_FILE=VERSION
VERSION=`cat $(VERSION_FILE)`

all:
	sbt clean compile dist

clean:
	sbt clean

test:
	sbt test

compile:
	sbt compile

dist:
	sbt dist

publishLocal:
	sbt publishLocal

publish:
	sbt publish

deploy:
	sbt clean compile test publish