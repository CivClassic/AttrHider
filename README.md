AttrHider
=========

Stops Bukkit Minecraft server from sending enchantment, item damage and potion duration data. 

Updated for Spigot 1.12.2 as of May 13th, 2019 thanks to Okx (https://github.com/okx-code/MetaHider)

## Description

Minecraft servers send packets containing data about other players to Minecraft clients. Some of this data is not necessarily needed by the client, such as potion durations, equipment enchantment types, and health. The vanilla Minecraft client does not display these for other entities other than the player themself, but hacked clients can. This gives an edge to players with hacked clients over those who use the default client, especially in PvP situations. 

AttrHider solves this by modifying outgoing packets from the server that concern potion durations, equipment enchantments, and entity health. Data that clients do not need to know about are replaced with fake values. Players using default clients will not see any differences, but players using hacked clients will see false information.
