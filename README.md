# Pingspam
[![Modrinth Badge](https://waffle.coffee/modrinth/pingspam/downloads)](https://modrinth.com/mod/pingspam)

*Reach those pesky AFKers, alt-tabbers, chat-ignorers and offline people with one simple trick!*  
Pingspam allows you to mention players in chat. You can mention by name (i.e "@GreatGrayOwl") or group.

The ping is highlighted in chat, plays a sound for the player, and hogs the actionbar until dismissed.
It will also persist between relogins - until you acknowledge it using the `/notifications` command.

## Groups
Operators (and people who have the permission) can ping groups of people at once.

`@online` pings every player online on the server right now.  
`@offline` pings all offline players that ever connected to the server - they will see it once they log in.  
`@everyone` pings all players, both online and offline.

## Aliases
You can have shorter, or a more descriptive names for others to ping you by.
`/pingspam alias [add|remove] <alias>` adds or removes aliases, and `/pingspam alias list` lists them.

You can have up to 10 ping aliases in addition to your own player name. Aliases are first come, first served.

Operators can edit other people's aliases using `/pingspam alias player <playername> [add|remove|list] <alias>`

## Ignore
You can ignore pings from particularly annoying players using `/pingspam ignore add <offender>`
You can use `/pingspam ignore list` to review your ignore list and `/pingspam ignore remove <offender>` to stop ignoring them.

Note that server operators bypass this setting.

## Sounds

You can change the notification sound that you hear by using `/pingspam sound <minecraft sound id>`
You can disable the ping sound altogether by using `/pingspam sound none`
This option is per-player.

The default sound is `/pingspam sound minecraft:block.bell.use`

## Server/client parts

Pingspam works with vanilla clients. You don't need to have Pingspam to ~~suffer from~~ enjoy advanced attention-grabbing techniques.

However, having it on the client adds chat autocompletion.

## For server owners 

### Config

Pingspam is configured through `config/pingspam.json5`.
It will be created on the first mod launch, and contains all options with comments.

**N.B.:** if you are using a chat bridge mod, e.g. *Tom's Server Utils* or *Fabric-Discord Link*, you may want to disable `processPingsFromUnknownPlayers`, otherwise discord users will be able to ping people bypassing permissions

### Permissions 
Pingspam supports LuckPerms through the Fabric Permissions API.

Pingspam honors the following permissions:
```
pingspam.alias.addown
pingspam.alias.removeown
pingspam.alias.addplayer
pingspam.alias.removeplayer
pingspam.bypass.aliaslimit
pingspam.bypass.ignore
pingspam.ping.everyone
pingspam.ping.online
pingspam.ping.offline
pingspam.ping.player 
```

