# End-to-end encrypted messaging client in Java

> It kinda works.. i think

It's actually double-encrypted, lmao. Too lazy to explain :P

> I really doubt it will work with big chats. I tested only with 10 accounts and it was already kinda weird.

## Run server
```sh
java -jar e2ee-server.jar -a 127.0.0.1 -p 6969 -s
```

## Run client
```sh
java -jar e2ee-server.jar -a 127.0.0.1 -p 6969
```

## Arguments
**-a, --address:** Address of the server *(Default: `127.0.0.1`)*\
**-p, --port:** Port of the server *(Default: `8080`)*\
**-s, --server:** Run the server client *(Defalt: `false`)*