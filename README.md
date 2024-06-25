# ServerLinksSender

## Context

Minecraft 1.21 released a new feature, allowing vanilla servers to send vanilla clients certain links which the client then can display in the pause menu under a new option called Server Links. In there they are listed and can be clicked on.

Vanilla only exposes sending a “Bug report link” that players can use to report bugs on the server, but the system is capable of doing much more! That’s where this mod comes in.

## Description

ServerLinksSender allows to configure custom button labels and links for those, so you can supply your very own links and labels for things like server website, vote links, discord links, etc. and periodically update them as the server runs.
Additionally, this supports [placeholder API and quicktext](https://placeholders.pb4.eu/user/general/), allowing for easy and dynamic text components.
Finally, you can make use of the [Predicate API](https://github.com/Patbox/PredicateAPI/blob/1.21/BUILTIN.md) to send certain links only under certain conditions, for example, send a link to the staff docs only to players with the staff permission or send a link with event rules if players are in the event world!

## Limitations

First things first, you are **not able to use clickevents or hovereffects of any sort** as these are used in a button, not chat. I have not tested everything, but can confirm **text decorations and colours work fine**.

Additionally, the vanilla server links screen does **NOT** re-render if the new links gets sent. It will only re-render whenever the player actually opens it, and then it **only gets rendered once**, meaning you can't do super dynamic placeholders like worldtime as they don't get updated as the played looks at them.

## Commands

| Command                               | Permission         | Description                                                                                                                                                                                                                                        |
|---------------------------------------|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/reloadserverlinks [--force-update]` | `serverlinks.main` | Reloads the config from file and can optionally send the new list to all currently online players; this is especially useful if the players only get the links once (more on that below) and you want to update the links without having to re-log |

## Example config

The mod creates a default configuration that you can use as a baseline, below is a similar configuration with some explanations (marked with `//`

```json
{
// how many ticks should pass before the list gets sent to all players again
// you will need this if you have placeholders in use that change throughout user's play session. Setting this to 20 means it gets sent roughly every second. Lower than 20 is not recommended.
// Setting this to -1 will disable periodic updates, meaning players will only get the list when they login (or --force-update is used)
  "refresh_interval": -1,
// list of links to get sent to all users without any condition
  "global_server_links": {
    "<red><bold>My cool link!": "https://google.com"
  },
// multiple sets of links that can be sent additionally to those above if the player matches the requirements (e.g. has a certain permission or is in a certain world)
  "additional_links": [
    {
      "links": {
        "<rb>My cool extra link!": "https://google.com"
      },
      // for more info on how to use this, see here https://github.com/Patbox/PredicateAPI/blob/1.21/BUILTIN.md
      "requirement": {
        "type": "permission",
        "permission": "example.permission",
        "operator": 2
      }
    }
  ]
}
```