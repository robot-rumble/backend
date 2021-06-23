# robot-rumble

## Level 1: only edit backend
1. Install [yarn](https://yarnpkg.com/), [aws-cli](https://github.com/aws/aws-cli), [Docker](https://docs.docker.com/get-docker/), and [docker-compose](https://docs.docker.com/compose/install/).

2. Clone the repos:
```sh
mkdir rr && cd rr
git clone https://github.com/robot-rumble/backend
git clone https://github.com/robot-rumble/battle-viewer
```

3. Fetch WASM worker assets
```sh
aws s3 sync s3://rr-public-assets/worker-assets backend/public/dist/
```

4. Start the battle-viewer webpack process:
```sh
cd battle-viewer
yarn
yarn watch
```

4. Open a new terminal window and start docker:
```sh
# IN A NEW WINDOW:
sudo systemctl start docker

# If installing on linux, follow these steps to execute docker without root access:
# https://docs.docker.com/engine/install/linux-postinstall/
```

5. Start the backend:
```sh
cd rr/backend
docker-compose up
```

6. Open `localhost:9000` and apply the migration when prompted

7. Seed the database. Note that you won't be able to create accounts through the site because email verification won't work.
```sh
docker-compose exec backend sh -c "cat conf/db/seeds.scala | DB_HOST=db sbt console"
```

## Level 2: edit logic
1. Install [rustup](https://rustup.rs/), [wasm-pack](https://rustwasm.github.io/wasm-pack/installer/), [wasienv](https://github.com/wasienv/wasienv#install), clang, [wasmer](https://wasmer.io).

2. Clone the repos:
```sh
# /rr
git clone https://github.com/robot-rumble/logic --recursive
```

3. Build.
```sh
# /rr
bash logic/build-for-web.sh
```
