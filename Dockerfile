FROM node:lts AS frontend-builder
WORKDIR /frontend

COPY src/package.json ./
RUN yarn

COPY src/src src
COPY src/webpack.config.js src/.babelrc src/elm.json ./

RUN NODE_ENV=production yarn build


FROM hseeberger/scala-sbt:8u222_1.3.4_2.13.1
WORKDIR /backend

COPY . ./
RUN sbt clean stage

COPY --from=frontend-builder /frontend/dist /tmp/static

RUN mkdir /static

ARG SECRET
ENV SECRET $SECRET

EXPOSE 9000
CMD rm -rf /static/* && cp -r /tmp/static/* /static && \
    target/universal/stage/bin/robot-rumble \
        # https://stackoverflow.com/a/29244028
        -Dpidfile.path=/dev/null \
        -Dplay.http.secret.key='$SECRET'
