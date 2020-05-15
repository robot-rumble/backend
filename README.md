# robot-rumble

## Level 1: only edit backend
1. [wasm-pack](https://rustwasm.github.io/wasm-pack/installer/), [wasienv](https://github.com/wasienv/wasienv#install), clang, wasmer, [Docker](https://docs.docker.com/get-docker/), and [docker-compose](https://docs.docker.com/compose/install/).

2. Clone the repos:
```sh
mkdir rr & cd rr
git clone https://github.com/robot-rumble/backend
git clone https://github.com/robot-rumble/battle-viewer
```

3. Build the battle-viewer:
```sh
# /rr
yarn --cwd battle-viewer
```

4. Start the battle-viewer webpack process:
```sh
# /rr
cd backend/src
yarn
yarn dev
```

4. Start docker:
```sh
# If installing on linux, follow these steps to execute docker without root access:
# https://docs.docker.com/engine/install/linux-postinstall/
sudo systemctl start docker
```

5. Start the backend:
```sh
# /rr
cd backend
docker-compose up
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
