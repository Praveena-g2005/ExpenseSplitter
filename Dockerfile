# Use OpenJDK 17 slim base image
FROM openjdk:17-slim

# Set working directory
WORKDIR /app

# Install sbt
RUN apt-get update && \
    apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x99E82A75642AC823" | apt-key add - && \
    apt-get update && \
    apt-get install -y sbt && \
    apt-get clean

# Copy project files
COPY build.sbt . 
COPY project ./project
COPY . .

# Generate ScalaPB classes (protobuf) and compile the project
RUN sbt compile

# Expose Play HTTP port and gRPC port
EXPOSE 9000
EXPOSE 50051

# Run the Play app
CMD ["sbt", "run"]
