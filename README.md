# robot-rumble

1. Install [Rust](https://rustup.rs/), [wasm-pack](https://rustwasm.github.io/wasm-pack/installer/), [Docker](https://docs.docker.com/get-docker/), and [docker-compose](https://docs.docker.com/compose/install/).

2. Clone the repos:
```
mkdir rr & cd rr
git clone https://github.com/robot-rumble/backend --recursive
git clone https://github.com/robot-rumble/logic --recursive
```

3. Build the web battle-runner:
```
# /rr
cd logic/runners/webapp
wasm-pack build
```

4. Start the battle-viewer webpack process:
```
# /rr
cd backend/src
yarn # or npm i
yarn watch # or npm run watch
```

5. Start the backend:
```
# /rr
cd backend
docker-compose up
```
