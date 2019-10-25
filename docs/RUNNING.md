Running a Server
================

Now that you've read about the project, it's time to set up a server to play with your friends! We'll walk through the "traditional" way of running a PGM server, similar to how you would run a Bukkit server. If you want to learn more about running servers, please see some additional documentation [here.](https://bukkit.gamepedia.com/Setting_up_a_server)

Installation
------------

1. Select the folder where you want to store your server files.
```bash
cd /path/to/folder
```

2. Download the latest version of [SportPaper](https://github.com/Electroid/SportPaper), a fork of the Minecraft 1.8 server.
```bash
curl https://pkg.ashcon.app/sportpaper -o sportpaper.jar
curl https://pkg.ashcon.app/sportpaper-config -o sportpaper.yml
```

3. Create a plugins folder and download the latest version of PGM.
```bash
mkdir plugins
curl https://pkg.ashcon.app/pgm -o plugins/pgm.jar
```

4. Add your maps to the maps folder, if you don't have any, skip this step.
```bash
cp -r /path/to/map-0/ plugins/PGM/maps/map-0
```

5. Run the server and enjoy playing PvP games with your friends!
```bash
java -jar sportpaper.jar nogui
```
