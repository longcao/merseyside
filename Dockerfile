FROM        dockerfile/java:openjdk-7-jdk

MAINTAINER  Long Cao <longcao@gmail.com>

ENV         ACTIVATOR_VERSION 1.2.10

# Install Typesafe Activator
RUN         cd /tmp && \
            wget http://downloads.typesafe.com/typesafe-activator/$ACTIVATOR_VERSION/typesafe-activator-$ACTIVATOR_VERSION.zip && \
            unzip typesafe-activator-$ACTIVATOR_VERSION.zip -d /usr/local && \
            mv /usr/local/activator-$ACTIVATOR_VERSION /usr/local/activator && \
            rm typesafe-activator-$ACTIVATOR_VERSION.zip

# Add project files
ADD         app /root/app
ADD         conf /root/conf
ADD         lib /root/lib
ADD         public /root/public
ADD         build.sbt /root/
ADD         project/plugins.sbt /root/project/
ADD         project/build.properties /root/project/

# Build project -- any errors will stop the build process
RUN         cd /root; /usr/local/activator/activator test stage
RUN         rm /root/target/universal/stage/bin/*.bat

# Copy "/public" assets folder over so it's accessible with Play.getFile
RUN         cp -r /root/public /root/target/universal/stage/public

# Run
WORKDIR     /root
EXPOSE      9000
CMD         target/universal/stage/bin/$(ls target/universal/stage/bin)
