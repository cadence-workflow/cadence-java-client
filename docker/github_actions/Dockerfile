# Always use a release version or a specific commit hash below. Don't use
# a branch name since we'd keep getting updates as long as there are new
# commits to that branch and one of them can break the build
FROM adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine

# Apache Thrift version
ENV APACHE_THRIFT_VERSION=0.9.3

# Install dependencies using apk
RUN apk update && apk add --virtual wget ca-certificates wget && apk add --virtual build-dependencies build-base gcc
# Git is needed in order to update the dls submodule
RUN apk add git libstdc++ bash curl

# Compile source
RUN set -ex ;\
	wget https://archive.apache.org/dist/thrift/${APACHE_THRIFT_VERSION}/thrift-${APACHE_THRIFT_VERSION}.tar.gz && \
	tar -xvf thrift-${APACHE_THRIFT_VERSION}.tar.gz && \
	rm thrift-${APACHE_THRIFT_VERSION}.tar.gz && \
	cd thrift-${APACHE_THRIFT_VERSION}/ && \
	./configure --enable-libs=no --enable-tests=no --enable-tutorial=no --with-cpp=no --with-c_glib=no --with-java=yes --with-ruby=no --with-erlang=no --with-go=no --with-nodejs=no --with-python=no && \
	make && \
	make install && \
	cd .. && \
	rm -rf thrift-${APACHE_THRIFT_VERSION}

# Cleanup packages and remove cache
RUN apk del build-dependencies wget && rm -rf /var/cache/apk/*

RUN mkdir /cadence-java-client
WORKDIR /cadence-java-client
