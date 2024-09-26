FROM eclipse-temurin:21

RUN mkdir /opt/app

COPY target/expense-tracker-backend-0.0.1-SNAPSHOT.jar /opt/app

ARG JWT_EXPIRATION_SECONDS
ARG JWT_SECRET

ENV JWT_EXPIRATION_SECONDS=${JWT_EXPIRATION_SECONDS}
ENV JWT_SECRET=${JWT_SECRET}

CMD ["java", "-jar", "/opt/app/expense-tracker-backend-0.0.1-SNAPSHOT.jar"]
