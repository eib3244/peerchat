import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * CSCI 351: Project 4
 * main class that handles all of the connections
 * A udp multi-cast socket is opened and ran on a thread to handle receiving multi-cast data
 * 0+ udp datagram sockets are opened to receive udp packets from forwarding peers
 * messages are sent via udp datagrams
 */
public class connection  {

    private String MulticastIP = "";
    private int portToUse = 7001;
    public ArrayList<DatagramSocket> listeners = new ArrayList<>();
    public MulticastSocket mcSocketUsed = null;

    /*
     * main method for joining our multi-cast and listening on it
     * runs in a separate thread to allow user to input text from the cmdline
     */
    public void startListeningMulticast(int port, String ip){

        // the thread that is run
        Thread clientThread = new Thread(new Runnable() {
            public void run() {

                // creating multi-cast socket and connecting to it
                MulticastSocket mcSocket = null;
                MulticastIP = ip;
                portToUse = port;
                try {
                    int mcPort = port;
                    String mcIPStr = ip;
                    InetAddress mcIPAddress = null;
                    mcIPAddress = InetAddress.getByName(mcIPStr);
                    mcSocket = new MulticastSocket(mcPort);
                    mcSocket.joinGroup(mcIPAddress);
                    mcSocketUsed = mcSocket;

                    // notifying waiting main thread
                    synchronized ((Object)peerchat.joined) {
                        ((Object) peerchat.joined).notifyAll();
                    }

                    // while the peer is still in the chat room
                    while (peerchat.joined) {

                        // waiting to receive data from our multi-cast
                        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                        mcSocket.receive(packet);
                        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());

                        // not your message: used to stop looping messages
                        if (!msg.contains("`"+ peerchat.userName +"`:") && !msg.contains("<" + peerchat.userName + ">")){

                            // looking to see if we have a new user
                            Boolean newUserArleadyJoined = false;
                            Boolean newLeavingUser = false;
                            for (String str: msg.split("`:")) {
                                // new user is joining
                                if (str.contains("`NEW_USER`")){
                                    String newUserData[] = msg.split("`NEW_USER`")[1].split(",");

                                    // has the user already joined?
                                    for (Peer peer: peerchat.peers){
                                        if (peer.name.equals(newUserData[0])){
                                            newUserArleadyJoined = true;
                                            break;
                                        }
                                    }

                                    // not a new user for us
                                    if (newUserArleadyJoined) {
                                        break;
                                    }

                                    Peer newUser = new Peer(newUserData[0], newUserData[1], Integer.parseInt(newUserData[2]), Integer.parseInt(newUserData[3]), packet.getAddress());
                                    System.out.println("member joined: " + newUser.name + "@" + newUser.ipAddress + " " + newUser.zipCode + " " + newUser.age);

                                    // sending your info / others peers connected to you out
                                    try {
                                        for (Peer peer : peerchat.peers) {
                                            sendMessage("`" + peerchat.myinfo.name + "`:" + "`NEW_USER`" + peer.toString(), peerchat.forwardingClients);
                                        }
                                    } catch (Exception e){}

                                    peerchat.peers.add(newUser);
                                    break;
                                }

                                // user is leaving
                                if (str.contains("`LEAVING_USER`")){
                                    String newUserData[] = msg.split("`LEAVING_USER`")[1].split(",");

                                    // has the user already left?
                                    for (Peer peer: peerchat.peers){

                                        // user has not left
                                        if (peer.name.equals(newUserData[0])){
                                            peerchat.peers.remove(peer);
                                            Peer newUser = new Peer(newUserData[0], newUserData[1], Integer.parseInt(newUserData[2]), Integer.parseInt(newUserData[3]), packet.getAddress());
                                            System.out.println("member leaving: " + newUser.name + "@" + newUser.ipAddress + " " + newUser.zipCode + " " + newUser.age);
                                            newLeavingUser = true;
                                            break;
                                        }
                                    }
                                    break;
                                }

                                // basic message
                                if (str.contains("<")) {
                                    System.out.println("Multi-Cast Message: " + str);
                                    break;
                                }
                            }

                            // need to forward packet
                            if ((!newUserArleadyJoined)||(!newLeavingUser))
                                sendMessage("`" + peerchat.userName + "`:" + msg , peerchat.forwardingClients);
                        }

                    }
                } catch (Exception e){}
                mcSocket.close();
            }
        });

        // starting thread
        clientThread.start();
    }

    /*
     * main method for listening on a port for forwarded messages
     * messages are received as udp datagrams
     */
    public void listenOnPort(int port){
        System.out.println("Listening on port: " + port);

        Thread listeningThread = new Thread(new Runnable() {

            // main thread that handles the connection
            public void run() {

                // opening socket
                DatagramSocket listeningSocket = null;
                try {
                    listeningSocket = new DatagramSocket(port);
                    listeners.add(listeningSocket);

                    // notifying waiting main thread
                    synchronized ((Object)peerchat.joined) {
                        ((Object) peerchat.joined).notifyAll();
                    }

                    // we loop until the user exits
                    while (peerchat.joined) {
                        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

                        // try to catch error when we force close socket later
                        try {
                            listeningSocket.receive(packet);
                        } catch (Exception e){}

                        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());

                        // is the message our own ? have we forwarded this before ? if so we don't do it again - prevents loops
                        if (!msg.contains("`"+ peerchat.userName +"`:") && !msg.contains("<" + peerchat.userName + ">")) {

                            // new user ?
                            Boolean newUserArleadyJoined = false;
                            Boolean newLeavingUser = false;
                            for (String str: msg.split("`:")) {

                                // new user is joining
                                if (str.contains("`NEW_USER`")){
                                    String newUserData[] = msg.split("`NEW_USER`")[1].split(",");

                                    // has the user already joined?
                                    for (Peer peer: peerchat.peers){
                                        if (peer.name.equals(newUserData[0])){
                                            newUserArleadyJoined = true;
                                            break;
                                        }
                                    }

                                    // not a new user for us
                                    if (newUserArleadyJoined) {
                                        break;
                                    }

                                    Peer newUser = new Peer(newUserData[0], newUserData[1], Integer.parseInt(newUserData[2]), Integer.parseInt(newUserData[3]), packet.getAddress());
                                    System.out.println("member joined: " + newUser.name + "@" + newUser.ipAddress + " " + newUser.zipCode + " " + newUser.age);

                                    // sending your info / others peers connected to you out
                                    for (Peer peer: peerchat.peers) {
                                        sendMessage("`" + peerchat.myinfo.name + "`:" + "`NEW_USER`" + peer.toString() , peerchat.forwardingClients);
                                    }

                                    peerchat.peers.add(newUser);
                                    break;
                                }

                                // user is leaving
                                if (str.contains("`LEAVING_USER`")){
                                    String newUserData[] = msg.split("`LEAVING_USER`")[1].split(",");

                                    // has the user already left?
                                    for (Peer peer: peerchat.peers){

                                        // removing user
                                        if (peer.name.equals(newUserData[0])){
                                            peerchat.peers.remove(peer);
                                            Peer newUser = new Peer(newUserData[0], newUserData[1], Integer.parseInt(newUserData[2]), Integer.parseInt(newUserData[3]), packet.getAddress());
                                            System.out.println("member leaving: " + newUser.name + "@" + newUser.ipAddress + " " + newUser.zipCode + " " + newUser.age);
                                            newLeavingUser = true;
                                            break;
                                        }
                                    }
                                    break;
                                }

                                // basic message
                                if (str.contains("<")) {
                                    System.out.println("Forward Message: " + str);
                                    break;
                                }
                            }

                            // need to forward data to other users
                            if ((!newUserArleadyJoined)||(!newLeavingUser))
                                sendMessage("`" + peerchat.userName + "`:" + msg, peerchat.forwardingClients);
                        }
                    }

                    // ending connection
                    listeningSocket.close();
                } catch (Exception e){
                    System.out.println("Error opening listening port. Possible duplicate port or IP used: exiting program.");
                    peerchat.joined = false;
                    peerchat.exit = true;
                    peerchat.setExit();
                }
                // closing socket
                try {
                    assert listeningSocket != null;
                    listeningSocket.close();
                    mcSocketUsed.close();
                } catch (java.lang.NullPointerException e){}
            }
        });
        // starting thread
        listeningThread.start();
    }

    /*
     * used to send a message to:
     * our current multi-cast domain
     * to anyone we are set to forward data to
     */
    public void sendMessage(String message, HashMap<String, Integer> forwardingClientInfo) throws IOException {
        // multi cast send
        DatagramSocket udpSocket = new DatagramSocket();
        InetAddress mcIPAddress = InetAddress.getByName(MulticastIP);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length);
        packet.setAddress(mcIPAddress);
        packet.setPort(portToUse);
        udpSocket.send(packet);
        udpSocket.close();

        // looping over forwarding clients and forwarding message
        for (String key: forwardingClientInfo.keySet()){
            udpSocket = new DatagramSocket();
            mcIPAddress = InetAddress.getByName(key);
            packet = new DatagramPacket(msg, msg.length);
            packet.setAddress(mcIPAddress);
            packet.setPort(forwardingClientInfo.get(key));
            udpSocket.send(packet);
            udpSocket.close();
        }
    }
}
