Running a Server
================

Now that you've read about the project, it's time to set up a server to play with your friends! We'll walk through the "traditional" way of running a PGM server, similar to how you would run a Bukkit server. If you want to learn more about running servers, please see some additional documentation [here.](https://www.spigotmc.org/wiki/spigot-installation/)

Installation
------------

1. Select the folder where you want to store your server files.
```bash
cd /path/to/folder
```

2. Download the latest version of [SportPaper](https://pkg.ashcon.app/sportpaper), a fork of the Minecraft 1.8 server.
```bash
curl https://pkg.ashcon.app/sportpaper -Lo sportpaper.jar
curl https://pkg.ashcon.app/sportpaper-config -Lo sportpaper.yml
```

3. Create a plugins folder and download the latest version of [PGM](https://pkg.ashcon.app/pgm).
```bash
mkdir plugins
curl https://pkg.ashcon.app/pgm -Lo plugins/pgm.jar
```

4. Run the server and enjoy playing PvP games with your friends!
```bash
java -jar sportpaper.jar nogui
```

Advanced
--------

You can also run an "out-of-the-box" PGM server as a container. This is a more advanced method of running a server and is only recommended if you have some basic knowledge about [Docker.](https://www.freecodecamp.org/news/a-beginner-friendly-introduction-to-containers-vms-and-docker-79a9e3e119b/)

1. Pull the latest version of the PGM server.
```bash
docker pull electroid/pgm:latest # Gets the latest version
docker pull electroid/pgm:2019-10-25 # Gets the version on a specific date
```

2. Run a PGM server with `1 GB` of RAM and `2` CPUs. Read [here](https://docs.docker.com/engine/reference/run) for more detailed documentation.
```bash
docker run \
    -dit \
    -p 25565:25565 \
    --restart=unless-stopped \
    --memory-swappiness=100 \
    --memory=1G \
    --cpus=2 \
    --name=myserver \
    electroid/pgm:latest
```

3. You can also run other `docker` commands to interact with the PGM server.
```bash
docker ps # List all servers
docker logs -f myserver # Tails the logs from the server
docker attach myserver # Access and run commands on the server
docker stop myserver # Stops the server, will not restart
docker start myserver # Starts a server, if stopped
docker restart myserver # Restarts the server
docker rm -f myserver # Stops and delete the server
```
