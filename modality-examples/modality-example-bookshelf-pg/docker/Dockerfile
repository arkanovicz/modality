FROM tomcat:8

RUN wget http://central.maven.org/maven2/org/postgresql/postgresql/42.2.5/postgresql-42.2.5.jar
RUN wget http://central.maven.org/maven2/mysql/mysql-connector-java/8.0.16/mysql-connector-java-8.0.16.jar
RUN mv postgresql-42.2.5.jar lib
RUN chown root:root lib/postgresql-42.2.5.jar
RUN mv mysql-connector-java-8.0.16.jar lib
RUN chown root:root lib/mysql-connector-java-8.0.16.jar
