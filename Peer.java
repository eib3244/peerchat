import java.io.Serializable;
import java.net.InetAddress;

/*
 * CSCI 351: Project 4
 * Helper class used to represent peers in our chat network
 */
public  class Peer implements Serializable {

    // name limit is 30 chars no commas or Exclamation points
    String name;

    // zipcode limit is 5 chars no commas or Exclamation points
    String zipCode;

    // must be an integer (fit in 32 bit int)
    int age;

    // default is 7001, port will change if a user connects to a server as they will need their own port that is unique to their ip
    // in order for others to connect to them
    int port;

    // ip address begins as: /0.0.0.0
    InetAddress ipAddress;

    /*
     * basic constructor
     */
    Peer(String name, String zipCode, int age, int port, InetAddress ipAddress){

        // name will be at most 30 chars long
        if (name.length() > 30) {
            this.name = name.substring(0, 30).replace(',', ' ').replace('!', ' ').replace(':', ' ');
        }
        else {
            this.name = name;
        }

        // zipcode will be at most 5 chars long
        if (zipCode.length() > 5) {
            this.zipCode = zipCode.substring(0, 5).replace(',', ' ').replace('!', ' ').replace(':', ' ');
        }
        else {
            this.zipCode = zipCode;
        }

        this.age = age;
        this.port = port;
        this.ipAddress = ipAddress;
    }

    /*
     * used to transport data: at max will be ~70 bytes thus, we round to 100
     */
    @Override
    public String toString() {
        return this.name + "," + this.zipCode + "," + this.age + "," + this.port + "," +this.ipAddress.toString();
    }

    /*
     * 2nd toString used to print out peer info
     */
    public String toString2(){
        return this.name + ": " + this.age + " " + this.zipCode;
    }
}