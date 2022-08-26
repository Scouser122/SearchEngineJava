FROM openjdk:16-alpine3.13

RUN mkdir -p /apps
COPY ./target/search-engine-122-1.0.0.jar /apps/app.jar
COPY ./target/application.yml /application.yml
COPY ./entrypoint.sh /apps/entrypoint.sh

RUN chmod +x /apps/entrypoint.sh
CMD ["/apps/entrypoint.sh"]