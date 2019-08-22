FROM ubuntu:16.04

MAINTAINER David Monschein

# Set environment variables
ENV MD5=4e7ce65489347960e9797d2161e0ada2

ADD start.sh /start.sh 

RUN \
	apt-get update &&\
	apt-get install -y software-properties-common && \
	apt-get install unzip && \
	apt-get install nano && \
	apt-get install --yes git && \
	apt-get install curl wget

RUN apt-get update && apt-get install -y dos2unix &&\
	dos2unix /start.sh

#Make the scripts executable 
RUN chmod +x /*.sh

# Install Java.
# This is in accordance to : https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-get-on-ubuntu-16-04
RUN apt-get update && \
add-apt-repository ppa:linuxuprising/java && \
apt-get update && \
echo oracle-java12-installer shared/accepted-oracle-license-v1-2 select true | /usr/bin/debconf-set-selections && \
apt-get install -y oracle-java12-set-default && \
echo oracle-java12-installer shared/accepted-oracle-license-v1-2 select true | /usr/bin/debconf-set-selections && \
apt-get install -y ant && \
apt-get clean && \
rm -rf /var/lib/apt/lists/*
 
# Fix certificate issues, found as of 
# https://bugs.launchpad.net/ubuntu/+source/ca-certificates-java/+bug/983302
RUN apt-get update && \
apt-get install -y ca-certificates-java && \
apt-get clean && \
update-ca-certificates -f && \
rm -rf /var/lib/apt/lists/* && \
rm -rf /var/cache/oracle-jdk12-installer;

# Setup JAVA_HOME, this is useful for docker commandline
#ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
#RUN export JAVA_HOME

#Install eclipse
RUN mkdir /etc/eclipse && \
	cd /etc/eclipse && \
	wget http://ftp.fau.de/eclipse/technology/epp/downloads/release/oxygen/3a/eclipse-modeling-oxygen-3a-linux-gtk-x86_64.tar.gz && \
	tar xfz eclipse-modeling-oxygen-3a-linux-gtk-x86_64.tar.gz -C /etc/eclipse && \
	rm eclipse-modeling-oxygen-3a-linux-gtk-x86_64.tar.gz && \
	cd /etc/eclipse && \
	ls

# python
RUN apt-get update
RUN apt-get install -y software-properties-common vim
RUN add-apt-repository ppa:jonathonf/python-3.6
RUN apt-get update

RUN apt-get install -y build-essential python3.6 python3.6-dev python3-pip python3.6-venv
RUN apt-get install -y git
RUN apt-get install -qqy x11-apps

# update pip
RUN python3.6 -m pip install pip --upgrade
RUN python3.6 -m pip install wheel

RUN python3.6 -m pip install requests

# install eclipse plugins
RUN chmod -R a+x /etc/eclipse/eclipse/
ADD eclipse.py /etc/eclipse/eclipse/eclipse.py
RUN cd /etc/eclipse/eclipse/ && \
	python3.6 /etc/eclipse/eclipse/eclipse.py

# install gradle
ENV GRADLE_VERSION=5.2.1
ENV GRADLE_HOME /home/user/gradle-$GRADLE_VERSION
ENV PATH $GRADLE_HOME/bin:$PATH

RUN wget -P /home/user/ --quiet https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip && \
cd /home/user/ && unzip gradle-$GRADLE_VERSION-bin.zip && rm gradle-$GRADLE_VERSION-bin.zip

RUN mkdir /etc/pipeline && \
	cd /etc/pipeline && \
	git clone https://github.com/dmonsch/PCM-Docker && \
	cd /etc/pipeline/PCM-Docker/org.pcm.automation.interface/ && \
	gradle bootJar

EXPOSE 8080 8080

CMD ["/start.sh"]