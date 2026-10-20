# syntax=docker/dockerfile:1

# ---- build: compila com o JDK e separa o jar em camadas ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY ticketing-service/pom.xml ticketing-service/
COPY ticketing-service/src ticketing-service/src

# O cache do ~/.m2 sobrevive entre builds: só baixa dependências quando o pom muda.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -pl ticketing-service -am package -DskipTests -Dspotless.check.skip=true \
    && cp ticketing-service/target/ticketing-service-*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---- runtime: só o JRE, sem compilador nem código-fonte ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
USER app

# Dependências mudam pouco e ficam em camadas de baixo; o código da aplicação, que muda a
# cada commit, fica na última. Um deploy novo só transfere essa camada.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
