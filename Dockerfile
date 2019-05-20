FROM ocaml/opam2:latest AS logic-builder
WORKDIR /logic

COPY logic/logic.opam ./
RUN opam pin add -yn logic ./
RUN opam depext logic
RUN opam install --deps-only logic

COPY logic ./
RUN sudo chown -R opam:nogroup ./
RUN opam exec dune build @frontend


FROM node:lts AS frontend-builder
WORKDIR /frontend

COPY frontend/package.json ./
RUN npm install

COPY frontend/src src
COPY --from=logic-builder /logic/_build/default/frontend.js src/frontend.js

COPY frontend/dist dist
COPY frontend/webpack.config.js frontend/.babelrc frontend/elm.json ./
RUN npm run build


FROM elixir:alpine
ENV MIX_ENV=prod

WORKDIR /backend
RUN mix local.hex --force && mix local.rebar --force

COPY backend/mix.exs backend/mix.lock ./
RUN mix do deps.get, deps.compile

COPY backend ./

COPY --from=frontend-builder /frontend/dist /static

EXPOSE 4000
CMD iex --cookie $COOKIE --sname $NAME -S mix phx.server
