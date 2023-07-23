# End-to-end encrypted messaging client in Java

> It kinda works.. i think

It's actually double-encrypted, lmao. Too lazy to explain :P

> I really doubt it will work with big chats. I tested only with 10 accounts and it was already kinda weird.

This is just an experimental project of mine. It is **NOT** going to work properly. Wanted to play around with end-to-end encryption, hehe :3c

## Run server
```sh
java -jar e2ee-messaging-client.jar -a 127.0.0.1 -p 6969 -s
```

## Run client
```sh
java -jar e2ee-messaging-client.jar -a 127.0.0.1 -p 6969
```

## Arguments
**-a, --address:** Address of the server *(Default: `127.0.0.1`)*\
**-p, --port:** Port of the server *(Default: `8080`)*\
**-s, --server:** Run the server client *(Defalt: `false`)*

## Gallery
![зображення](https://github.com/SuperNeon4ik/e2ee-messaging-client/assets/52915540/7ff0131e-6a69-43c7-bb5f-0e5da27bcf6b)
