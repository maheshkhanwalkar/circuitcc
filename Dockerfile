FROM openjdk:17-oracle
WORKDIR /usr/local/app

# Install dependency (required for Gradle to function)
RUN microdnf install findutils

# Copy over source files
COPY gradle ./gradle
COPY samples ./samples
COPY src ./src
COPY gradle.properties ./gradle.properties
COPY gradlew ./gradlew
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts
COPY execute.sh ./execute.sh

# Build CircuitC compiler
RUN ./gradlew buildFatJar --no-daemon

# Run the shell script
CMD ./execute.sh
