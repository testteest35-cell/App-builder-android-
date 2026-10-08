# DroidIDE Remote Cloud Build Container
# Base: Ubuntu with OpenJDK 17 + Android Command Line Tools + Node.js

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Install base dependencies and JDK 17
RUN apt-get update && apt-get install -y \
    curl \
    git \
    unzip \
    openjdk-17-jdk \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 20
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install Android Command Line Tools
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools \
    && curl -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
    && unzip /tmp/cmdline-tools.zip -d /tmp \
    && mv /tmp/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# Accept Android SDK licenses and install platform tools + API 35
RUN yes | sdkmanager --licenses \
    && sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

WORKDIR /app

COPY package*.json ./
RUN npm install --production

COPY server.js ./

EXPOSE 3000

CMD ["node", "server.js"]
