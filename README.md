# Robot Rumble

Prepare:
```
docker-compose -f docker-compose.[dev/prod].yml build
docker-compose -f docker-compose.[dev/prod].yml run --rm backend mix do ecto.create, ecto.migrate
```

Start:
```
docker-compose -f docker-compose.[dev/prod].yml up [-d]
```

Run a mix command:
```
docker-compose -f docker-compose.[dev/prod].yml exec [service] mix [command]
```

Remote-shell into the Phoenix process:
```
docker ps # find the id of the app container
# the name and cookie are in the compose file
docker-compose -f docker-compose.prod.yml exec backend iex --sname local --cookie [cookie] --remsh "[name]@[id]"
```

Prepare deploy:
```
# where robot is the ip alias
ssh robot 'bash -s' < deploy/install-docker.sh
ssh robot 'bash -s' < deploy/disable-aa.sh
ssh robot git init --bare robot.git
scp ./deploy/post-receive robot:robot.git/hooks
ssh robot chmod +x robot.git/hooks/post-receive
git remote add production robot:robot.git
git push production master
scp ./backend/config/prod.secret.exs robot:robot/backend/config
sudo systemctl restart docker
```

