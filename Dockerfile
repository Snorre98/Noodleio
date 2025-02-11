# Use OpenJDK 17 as base image
FROM eclipse-temurin:17-jdk

# Install necessary packages and Android SDK dependencies
RUN apt-get update && apt-get install -y \
    xorg-dev \
    libgl1-mesa-dev \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install Android SDK
ENV ANDROID_HOME /opt/android-sdk
ENV PATH ${PATH}:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools

# Download Android SDK Command-line tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O android-tools.zip && \
    unzip android-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm android-tools.zip

# Accept licenses and install required SDK packages
RUN yes | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses && \
    ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

WORKDIR /app

# Copy project files and build as before...
COPY gradle gradle
COPY gradlew .
COPY gradlew.bat .
COPY settings.gradle .
COPY gradle.properties .
COPY build.gradle .

# Copy the subproject gradle files
COPY core/build.gradle core/
COPY lwjgl3/build.gradle lwjgl3/
COPY android/build.gradle android/
COPY ios/build.gradle ios/

RUN chmod +x gradlew

COPY core core
COPY lwjgl3 lwjgl3
COPY android android
COPY ios ios
COPY assets assets

# Build both dekstop and Android versions
RUN ./gradlew lwjgl3:build android:build --no-daemon

# Build the runnable jar file from the core module.
# (This assumes your 'jar' task is configured in core/build.gradle to create a jar with Main-Class 'gr17.noodleio.game.Core')
RUN ./gradlew clean jar --no-daemon

# Default command can still be lwjgl3 for development
#CMD ["./gradlew", "desktop:run"]
CMD ["./start_game.sh"]
