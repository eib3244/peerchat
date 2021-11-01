Notes on peerchat:
leaving a chat room and joining back to quickly can cause errors with the sockets (specifically if you are forwarding / listening to data).
This is not present if you wait a second before reconnecting.

Messages start with how the message was received (Multi-cast or Forwarding)
User data that is forwarded across the router to other domains will share an ip (meaning peer1 and peer2 seem to have to same IP to peers 3, 4, 5)
sometimes the client just needs to be restarted (container can stay open)

Overall program seems to be in well working order !

Example runs that create forwarding as described in write up:
javac *.java

java peerchat -p 10000 -f 10.0.1.1:10000 -f 10.0.2.1:10000 peer1 91501 10
java peerchat peer2 91501 10
java peerchat -p 10000 -f 10.0.0.1:10000 peer3 91501 10
java peerchat peer4 91501 10
java peerchat -p 10000 -f 10.0.0.1:10000 peer5 91501 10

join commands:
/join 224.0.2.0
/join -p 7001 224.0.2.0

Notes on setup_chats:
script is ran as follows: sudo bash setup_chats
xterm is used for the terminals
might need to do chmod u+x setup_chats to make the file executable (should be done however)

containers can ping everyone and the network seems to be setup correctly

Note:
Required or we get error:
Cannot create namespace file "/var/run/netns/Peerchat1": File exists
ip netns delete "Peerchat 1"
ip netns delete "Peerchat 2"
ip netns delete "Linux Router"
ip netns delete "Peerchat 3"
ip netns delete "Peerchat 4"
ip netns delete "Peerchat 5"

This does create an initial error when we first run the script as these namespaces have not been made yet.