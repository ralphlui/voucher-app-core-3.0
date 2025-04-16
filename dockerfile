# Use a Java base image
FROM openjdk:17
# Set the working directory to /app
WORKDIR /app
# Copy the Spring Boot application JAR file into the Docker image
COPY target/voucher-app-core-0.0.1-SNAPSHOT.jar /app/voucher-app-core-0.0.1-SNAPSHOT.jar
# Copy certificate from build context into container
COPY devplify.crt /tmp/devplify.crt

RUN keytool -importcert \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit \
    -noprompt \
    -alias devplify \
    -file /tmp/devplify.crt && \
    rm /tmp/devplify.crt

# Expose the port that the Spring Boot application is listening on
EXPOSE 8081
# Run the Spring Boot application when the container starts
CMD ["java", "-jar", "voucher-app-core-0.0.1-SNAPSHOT.jar"]
