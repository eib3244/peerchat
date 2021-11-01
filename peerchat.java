import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/*
 * CSCI 351: Project 4
 * Peer Chat with multi-cast
 * A chat program that utilizes udp multi-cast to create a chat network
 * UDP datagrams & sockets are used to forward data to other domains
 *
 * Default port for join is port 7001
 *
 * Usage:
 * peerchat [-p <forwarding listen port>] [-f <forwarding client IP>:<port>]+ <username> <zip code> <age>
 *
 */
public class peerchat {

    // used to store forwarding ports that we will listen on
    static ArrayList <Integer> listeningPorts = new ArrayList<>();

    // uesd to store forwarding clients that we will forward data to: <IP, Port>
    static HashMap <String, Integer> forwardingClients = new HashMap<>();

    // basic user info
    public static String userName = null;
    public static String zipcode = null;
    public static int age = 0;
    public static Peer myinfo = null;

    // basic info to help us control flow of program
    public static boolean joined = false;
    public static Boolean exit = false;

    // peers currently connected
    public static ArrayList<Peer> peers = new ArrayList<>();

    public static Scanner scan = new Scanner(System.in);
    public static connection recive = new connection();

    /*
     * main method that reads in user args and then waits for user input
     * handles joining the multi-cast and starting threads to listen to data on
     */
    public static void main(String[] args) throws IOException {
        Boolean nextPortToListen = false;
        Boolean nextForwardingInfo = false;

        // looping over args and getting info
        for (String str: args){

            // getting listening ports
            if (str.trim().equals("-p")){
                nextPortToListen = true;
                continue;
            }
            if (nextPortToListen){
                listeningPorts.add(Integer.parseInt(str));
                nextPortToListen = false;
                continue;
            }

            // getting forwarding data
            if (str.trim().equals("-f")){
                nextForwardingInfo = true;
                continue;
            }
            if (nextForwardingInfo){
                String values[] = str.split(":");
                forwardingClients.put(values[0], Integer.parseInt(values[1]));
                nextForwardingInfo = false;
                continue;
            }

            if (userName == null){
                userName = str;
                continue;
            }
            if (zipcode == null){
                zipcode = str;
            }
            else {
                age = Integer.parseInt(str);
            }

        }

        /*
        System.out.println(userName);
        System.out.println(zipcode);
        System.out.println(age);
        System.out.println("");
        */

        myinfo = new Peer(userName, zipcode, age, 0, InetAddress.getByName("0.0.0.0"));

        /*
        System.out.println("listening ports");
        for (Integer a: listeningPorts){
            System.out.println(a);
        }

        System.out.println("");
        System.out.println("forwarding infos");
        for (String key: forwardingClients.keySet()){
            System.out.println(key + ":" + forwardingClients.get(key));
        }
        */

        System.out.println("Peer chat started successfully:");
        System.out.println("You can do a \"/join [-p port] <multi-cast IP>\" to join a multi-cast chat room");
        System.out.println("Suggested port / ip are as followed: 224.0.2.0:7001");

        // main loop that handles user input
        Boolean continueStartup = false;
        while (scan.hasNext()) {
            if (exit){
                break;
            }

            String input = scan.nextLine();
            if (input.equals(""))
                continue;

            // switching on user input
            String option[] = input.split("\\s+");
            switch (option[0]) {

                case "/join": {
                    if (joined){
                        System.out.println("You have joined a network please do a /leave to join another network.");
                        break;
                    }
                    recive = new connection();

                    int port = 7001;
                    String IP = "";

                    // optional port given
                    if (option.length == 4){
                        port = Integer.parseInt(option[2]);
                        IP = option[3];
                    }

                    // no port given
                    else {
                        IP = option[1];
                    }
                    joined = true;
                    recive.startListeningMulticast(port,IP);

                    // waiting for multi-cast thread to start
                    synchronized ((Object)joined) {
                        try {
                            ((Object)joined).wait();
                        } catch (InterruptedException e) {}
                    }

                    // starting listeners
                    for (Integer ports: listeningPorts){
                        recive.listenOnPort(ports);

                        // waiting for listening thread to start
                        synchronized ((Object)joined) {
                            try {
                                ((Object)joined).wait();
                            } catch (InterruptedException e) {}
                        }
                    }

                    if (!joined){
                        System.out.println("Error while joining");
                        break;
                    }

                    myinfo.port = port;
                    peers.add(myinfo);
                    recive.sendMessage("`" + peerchat.userName + "`:" + "`NEW_USER`" + myinfo.toString(), forwardingClients);
                    System.out.println("Joined chat success, you will have other users connection to you shortly.");
                    break;
                }

                case "/leave": {
                    if (!joined){
                        System.out.println("you have not joined a multi-cast network yet");
                        break;
                    }
                    else {

                        // sending leave message
                        recive.sendMessage("`" + peerchat.userName + "`:" + "`LEAVING_USER`" + myinfo.toString(), forwardingClients);
                        joined = false;

                        // closing listening sockets and forwarding sockets
                        for (DatagramSocket sock: recive.listeners){
                            sock.close();
                        }
                        try {
                            recive.mcSocketUsed.close();
                        } catch (Exception e){}
                    }
                    peers.clear();
                    System.out.println("You have left the chat successfully");
                    break;
                }

                case "/who": {

                    if (joined) {
                        for (Peer peer : peers) {
                            System.out.println(peer.toString2());
                        }
                    }
                    else {
                        System.out.println("Error: you have not joined a chat yet");
                    }
                    break;
                }

                case "/zip": {

                    // is the client in a chat server ??
                    if (joined){
                        for (Peer peer: peers){
                            if (peer.zipCode.equals(option[1]))
                                System.out.println(peer.toString2());
                        }
                    }
                    else {
                        System.out.println("Error: you have not joined a chat yet");
                    }
                    break;
                }

                case "/age": {
                    // is the client in a chat server ??
                    if (joined){
                        for (Peer peer: peers){
                            if (Integer.toString(peer.age).equals(option[1]))
                                System.out.println(peer.toString2());
                        }
                    }
                    else {
                        System.out.println("Error: you have not joined a chat yet");
                    }
                    break;
                }

                case "/exit": {

                    // sending leave message
                    recive.sendMessage("`" + peerchat.userName + "`:" + "`LEAVING_USER`" + myinfo.toString(), forwardingClients);
                    joined = false;

                    // closing listening sockets and forwarding sockets
                    for (DatagramSocket sock: recive.listeners){
                        sock.close();
                    }
                    try {
                        recive.mcSocketUsed.close();
                    } catch (Exception e){}

                    exit = true;
                    scan.close();
                    System.out.println("Exiting");
                    System.out.println();
                    System.exit(0);
                }

                default: {
                    // testing for unknown command
                    if (option[0].matches("/*")) {
                        System.out.println("Invalid command: " + input);
                        break;
                    }

                    // send a message
                    else if (joined) {
                        recive.sendMessage("`" + peerchat.userName + "`:" + "<" + peerchat.userName + "> " + (input.replace('`', ' ').replace(':', ' ')), forwardingClients);
                    }
                    else {
                        System.out.println("you have not joined a multi-cast network yet: can't send message");
                    }
                }
            }
        }
        System.out.println();
        scan.close();
        System.exit(0);
    }

    public static void setExit(){
        for (DatagramSocket sock: recive.listeners){
            sock.close();
        }
        try {
            recive.mcSocketUsed.close();
        } catch (Exception e){}

        exit = true;
        scan.close();
        System.out.println();
        System.exit(0);

    }
}
