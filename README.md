# SpongyCord

### What is this? (for plebs)
This is a utility plugin. Other plugins will depend on this. If you don't have any plugins that require this, you will have zero use for this plugin.

### What is this? (for devs)
Bungee has a convoluted plugin message system that they call an API. This is a much simpler API, with actual methods, that are wrappers around the plugin message logic.

### Okay, but what can I do with this?
You can:

* Connect players to other servers
* Get the real IP of any player on the network
* Get the amount of players on one server, or on all the servers
* Get a player list from one server, or all the servers
* Get the name of the server you are on
* Send a message to any player on the network
* Send any server or client another plugin message
* Get the real UUID of any player on the network
* Get the server IP of any networked server
* Kick any player on the network

###Hold on, some of these method signatures look weird.
Yeahh... about that... Because there's still no way of interfacing with Bungee other than plugin messages, the reply is not instantaneous. Most likely, any requested information will arrive on the next tick. This is why all of the methods are void, and some take Consumers. Also, for anyone wondering why I don't just return Future, the get() is blocking. It would block the single thread Minecraft runs on, the thread that would deliver the requested information. i.e. it would never arrive.

###How do I use such an awesome and well-made API in my plugin?
Add this to your build.gradle:

```gradle
repositories {
    maven {
        url "https://jitpack.io"
    }
}

dependencies {
    compile "com.github.pie-flavor:SpongyCord:1.1.0"
}
```

Not gonna provide a maven example cuz maven is for nerds.

Every method you need is found in `SpongyCord.API`. All method calls will throw exceptions if GameStartingServerEvent hasn't happened yet. 

##Changelog
1.0.0: Initial release.

1.1.0: Renamed the project to SpongyCord.

1.1.1: Fixed a bug with ConnectOther
