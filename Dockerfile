# build container
FROM maven:latest as maven
COPY ./pom.xml ./pom.xml
# mvn <goal>; the goal is to get all dependencies (so that we can go offline)
# this will allow docker to realize that the only input to this stage is the pom
# so we do not need to download dependencies when building app code
RUN mvn dependency:go-offline
# now copy source files and build
COPY ./src ./src
RUN mvn package

# runtime container
from openjdk:latest
# copy the runtime jar from the build container
copy --from=maven target/discord-bot-*.jar ./JacksonBottock.jar
# cmd is the final thing to do (run is a side effect)
cmd ["java", "-jar" , "./JacksonBottock.jar", "??TOKENHERE???"]
