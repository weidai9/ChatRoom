Simple Chat Room Framework

1. Description

This project implemented a chat room service using client-server architecture and use a temporary connection between the server and the client. All the required features are  implemented, as well as the two bonus features:

- Message Center:
	- User Authentication
	- User Message Forwarding
	- Timeout
	- Blacklisting
	- Presence Broadcasts
	- Offline Messaging 
	- Guaranteed Message Delivery (Bonus part)
- Chat Client:
	- Authentication
	- Chat (send, receive)
	- Display Presence Notification
	- Find Users Online
	- Heartbeat
	- P2P Privacy and Consent (Bonus part)
	- Handle failure and crash

2. Source Code

This project is composed with 7 classes. 

- ChatClient.java: the main class for chat client. The entry of the client program

- ChatClientHeartBeat.java: thread class to send heartbeat to server

- ChatClientReceiver.java: thread class to receive connection from server or other clients

- ClientWorker.java: message processing thread to process every incoming message using a temporary connection

- ChatServer.java: the main class for chat server. This is for receiving connections from clients, and using ConcurrentHashMap to save data. This class is the entry of the server program

- ServerWorker.java: thread class to receive requests from clients and process, handle them. Each run of ServerWorker is a temporary connection.

- Message.java: the class that define the protocol and parse it

There are also two files that define the username and password data, and the configurations:

- configurations.txt
- credentials.txt

The ChatServer will firstly read configurations.txt and credentials.txt, then start listen to socket connections. Each connection will start a ServerWorker to process the requests.

The ChatClient will start a ChatClientReceiver, a ClientHeartBeat. The ChatClientReceiver will listen to socket connections and start a ChatWorker each time.

3. Run Instructions

There is a makefile in the directory. To compile the project, use “make” command. 

(1) To run the server:
$ make
$ java ChatServer 6789
The above commands will compile the server and start the server using port number 6789

(2) To run the client:
$ make
$ java ChatClient 0.0.0.0 6789
The above commands will compile the client and start the client by connecting to 0.0.0.0 with port number 6789.

The port number of the client message receiver is automatically assigned. 


4. Sample Commands 

(1) Login:
$ make
$ java ChatClient 0.0.0.0 6789
> Username: Dave
> Password: 123456
Welcome to simple chat server!
> [Server]: Dave is online!
[Server]: No offline message for you.

(2) message:
- On client 1:
> message User1 hello!

- On client 2:
[Dave]: hello!

(3) broadcast:
Scenario 1:
> broadcast hello, this is a broadcast from Dave!

- on all clients that did not block the user:
[Broadcast] [Dave]: hello, this is a broadcast from Dave!

Scenario 2:
> broadcast hello, this is a broadcast!
> Your message could not be delivered to some recipients: [User1]
> [Broadcast] [Dave]: hello, this is a broadcast!


(4) online:
> online
Online users: [User2] [User1] [Dave]
> 

(5) block:
> block User1
User1 has been blocked

> block WrongName
Fail: no such user name!

(6) unblock:
> unblock User1
User1 has been unblocked

> unblock User2
The user is not in your blacklist.

> unblock WrongName
Fail: no such user name!

(7) logout:
> logout
Logout succeed! Exit...

(8) getaddress:
1) Scenario 1:
- Client 1:
> getaddress User1
> Your request has been sent, and you will get the address after the user's permission
> 

- Client 2:
User [Dave] want to start a private talk with you. Enter "agree [username]" toagree the request; enter "deny [username]" to deny the request.
> agree Dave

- Client 1:
> User [User1] agreed your address request: ip: 192.168.1.100, port: 64998

2) Scenario 2 (“Dave” is blocked by “User1”):
> getaddress User1
> You are blocked by: User1. failed to get address.

3) Scenario 3 (“User1” denied to provide the address):
Client 1:
> getaddress User1
> Your request has been sent, and you will get the address after the user's permission

Client 2:
User [Dave] want to start a private talk with you. Enter "agree [username]" toagree the request; enter "deny [username]" to deny the request.
> deny Dave

Client1:
> User [User1] rejected your address request.

(9) private:
(“Dave” has get “User1”’s ip address)
Client 1:
> private User1 hello, this is a private message :)
> 

Client 2:
[Private] [Dave]: hello, this is a private message :)


5. Description About the Bonus Part

(1) P2P Privacy and Consent
Just as the description in the homework requirements, the “getaddress” command need the consent from another user to get the ip and port to start a p2p private talk. 

The test cases are the same as part 4, (8) and (9)

(2) Guaranteed Message Delivery
When the client crashed and is actually offline, it might be not offline in the server’s view because that its heartbeat is still valid in a short time. In the implementation the ConnectException is caught to detect such crash, and save the message in the server as offline message for the target. When the exception is caught, the server will mark the user as offline. 

When a user is logged in, the server will check if there is any offline message for the client, and send the offline messages or requests to the client.


Sample test cases (use control C to force the client to exit):
Client “Dave”: (Client “User1” is forced to exit after login)
> online
Online users: [User1] [Dave]
> message User1 hello
The user is offline. Offline message is sent.
> getaddress User1
> The user is offline. Offline request is sent.
> [Server]: User1 is online!
User [User1] agreed your address request: ip: 192.168.1.100, port: 50888

Client “User1”:
> Username: User1
> Password: 123456
Welcome to simple chat server!
> [Server]: User1 is online!
[Dave]: hello
User [Dave] want to start a private talk with you. Enter "agree [username]" to agree the request; enter "deny [username]" to deny the request.
agree Dave
> 

