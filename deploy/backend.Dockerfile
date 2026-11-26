# 多阶段构建：第一阶段编译（利用依赖缓存层），第二阶段只保留 JRE 与 jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY backend/pom.xml ./pom.xml
COPY backend/ ./
RUN mvn -B -DskipTests clean package
# 断言产物存在：co-bootstrap/pom.xml 设置了 <finalName>campus-overflow</finalName>，
# 实际产物是 campus-overflow.jar。原写法 co-bootstrap-*.jar 的 glob 一个都匹配不上，
# 而 BuildKit 对 COPY --from 的空 glob 不报错，只产出一个空层——构建显示 success、
# 镜像照样推上去，容器一启动就是 "Unable to access jarfile app.jar"。
RUN test -s /src/co-bootstrap/target/campus-overflow.jar


FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && apk add --no-cache wget
WORKDIR /app
COPY --from=build /src/co-bootstrap/target/campus-overflow.jar app.jar
# 运行层再断言一次，把同类问题挡在构建阶段而不是运行阶段
RUN test -s app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Duser.timezone=UTC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
