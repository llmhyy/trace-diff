git submodule update --init --recursive
cd trace-model
call mvn install —file pom.xml
cd ..
call mvn package