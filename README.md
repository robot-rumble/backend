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
logic/build-for-web.sh
```

4. Start the battle-viewer webpack process:
```
# /rr
cd backend/src/battle-viewer
yarn # or npm i
cd ..
yarn # or npm i
yarn dev # or npm run dev
```

4. Start docker:
```
# If installing on linux, follow these steps to execute docker without root access:
# https://docs.docker.com/engine/install/linux-postinstall/
sudo systemctl start docker
```

5. Start the backend:
```
# /rr
cd backend
docker-compose up
```
