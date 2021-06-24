# Pingspam
[![Modrinth Badge](https://waffle.coffee/modrinth/pingspam/downloads)](https://modrinth.com/mod/pingspam)
[![CurseForge Badge](https://cf.way2muchnoise.eu/pingspam.svg)](https://www.curseforge.com/minecraft/mc-mods/pingspam)

*Reach those pesky AFKers, alt-tabbers, chat-ignorers and offline people with one simple trick!*  
Pingspam allows you to mention players in chat. You can mention by name (i.e "@GreatGrayOwl") or group.

The ping is highlighted in chat, plays a sound for the player, and hogs the actionbar until dismissed.
It will also persist between relogins - until you acknowledge it using the `/notifications` command.

Pingspam works with vanilla clients.
You don't need to have Pingspam to ~~suffer from~~ enjoy advanced attention-grabbing techniques.
However, installing it on the client adds chat autocompletion for @mentions.

## Groups and aliases
You can have a shorter or a more descriptive name for others to ping you by.
`/pingspam alias [add|remove] <alias>` adds or removes aliases, and `/pingspam alias list` lists them.

You can have up to 10 ping aliases in addition to your own player name. Aliases are first come, first served.

Operators can also assign players to groups, so that you can ping all the @redstoners at once.

`/pingspam group list` lists all groups you are a member of.

## Ignore
You can ignore direct pings from particularly annoying players using `/pingspam ignore add <offender>`
You can use `/pingspam ignore list` to review your ignore list and `/pingspam ignore remove <offender>` to stop ignoring them.

## Sounds
You can change the notification sound that you hear by using `/pingspam sound <minecraft sound id>`
You can disable the ping sound altogether by using `/pingspam sound none`

The default sound is `/pingspam sound minecraft:block.bell.use`

## Server owners and operators
Read more in the [README-SERVER.md](README-SERVER.md)


