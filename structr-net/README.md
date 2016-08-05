## structr-net
A protocol implementation for distributed Structr instances

### Description
structr-net is a UDP-based peer-to-peer synchronization toolkit. Its goal is to provide reliable, transactional data exchange between individual peers in an unreliable network.

### Concepts
structr-net implements distributed **shared objects**. A shared object has a single **master** version that lives on a single peer (the **home** of the object), with distributed copies on each of the other peers. Each distributed copy will update itself to eventually reflect the state of the master object. Changes to a shared object can be requested at the object **home**, and will be coordinated based on a concept of hierarchical time slots called **pseudo-time**.

### Running
structr-net can be run with the following commands

    mvn clean install
    java -jar target/structr-net-0.3.jar -i

#### Parameters
The following parameters can be used to configure a command-line structr-net peer.

    -i  - interactive mode
    -b  - bind address (default is 0.0.0.0)
    -p  - initial peer (default is 255.255.255.255)

#### Commands in interactive mode

    info                   - broadcasts an info message which causes all peers to print their status information
    i                      - prints status information
    create                 - creates a new shared object (this peer will be the home)
    get [id] [key]         - fetches the value of the given key from the given object
    set [id] [key] [value] - sets a property in the given object
    exit                   - exits the peer

### Source
The concepts of distributed transactions, pseudo-time and shared objects are based on ["Naming and Synchronization in a Decentralized Computer System" by David P. Reed (1978)](http://publications.csail.mit.edu/lcs/pubs/pdf/MIT-LCS-TR-205.pdf)
