# For server owners and operators

Pingspam has commands and features restricted to server operators.  
Pingspam supports permissions. For brevity, in the rest of the document "operators" means "operators or players with the corresponding permission."

## Groups and aliases

While players can ping groups, management is restricted to operators.

Operators can add or remove players to/from groups using  
`/pingspam group player <player> <add/remove> <group>`  
or list a player's groups using  
`/pingspam group player <player> list`

There are also special groups that only operators can ping:

`@online` pings every player online on the server right now.  
`@offline` pings all offline players that ever connected to the server - they will see it once they log in.  
`@everyone` pings all players, both online and offline.

Think twice before using these!

Operators can edit other people's aliases using   
`/pingspam alias player <playername> <add/remove> <alias>`  
or list their aliases using  
`/pingspam alias player <playername> list`

Group names and aliases may coincide with the name of a Mojang account.  
In this case, Pingspam always prefers to ping by group first, then by name, and only then by alias.

Operators bypass the alias limit.

## Ignore
Server operators bypass ignore lists. The ignore is still saved in users' data, so it will work once the offender is de-opped.

## Config
Pingspam is configured through `config/pingspam.json5`.
It will be created on the first mod launch, and contains all options with comments.

**N.B.:** if you are using a chat bridge mod, e.g. *Tom's Server Utils* or *Fabric-Discord Link*, you may want to disable `processPingsFromUnknownPlayers`, otherwise discord users will be able to ping people bypassing permissions

## Permissions 
Pingspam supports LuckPerms through the Fabric Permissions API.

Pingspam honors the following permissions:

Default op level 0:
```
pingspam.alias.own.add 
pingspam.alias.own.remove 
pingspam.ping.player 
pingspam.ping.group
```

Default op level 2:
```
pingspam.alias.player.add 
pingspam.alias.player.remove 
pingspam.bypass.aliaslimit 
pingspam.bypass.ignore 
pingspam.ping.everyone 
pingspam.ping.online 
pingspam.ping.offline 
pingspam.group.player.add 
pingspam.group.player.remove 
```