package resources;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The GameResourceManager is a singleton that manages all Threads and connection
 * sockets.
 * <p>
 * Early implementations of the project thread was prone to leaving threads and
 * sockets alive after the game was over. Using the GRM ensures that we never
 * lose reference to the sockets and threads we instantiate, and provides
 * simple means to halt resources when we need to.
 * <p>
 * IntelliJ warnings about resource management have been disabled for this class
 * as it is introduced specifically to control resources used over multiple
 * threads and as such cannot perform try-with-resources statements.
 */
@SuppressWarnings("resource")
public final class GameResourceManager
{
    private static final Logger LOGGER = Logger.getLogger(GameResourceManager.class.getName());
    private static final GameResourceManager GAMERESOURCEMANAGER = new GameResourceManager();
    private final List<ExecutorService> seqExecutors = new ArrayList<>();
    private ExecutorService parExecutor = Executors.newCachedThreadPool();
    private final List<Closeable> allSockets = new ArrayList<>();

    private Map<String, Socket> messageSockets = new HashMap<>();
    private Map<String, Socket> dataSockets = new HashMap<>();
    private Map<String, DataOutputStream> messageOuts = new HashMap<>();
    private Map<String, BufferedReader> messageIns = new HashMap<>();
    private Map<String, ObjectOutputStream> dataOuts = new HashMap<>();
    private Map<String, ObjectInputStream> dataIns = new HashMap<>();

    private ServerSocket serverSocket = null;

    private static int socketNumber = 0;


    private GameResourceManager() {}

    public static GameResourceManager instance() {
	LOGGER.log(Level.FINEST, "GameResourceManager instance was polled.");
	return GAMERESOURCEMANAGER;
    }

    private static String nextSocketID() {
	socketNumber++;
	return Integer.toString(socketNumber, 10);
    }

    /**
     * Executes an array of threads sequentially. When a sequence of threads
     * are submitted, they are started on new SingleThreadExecutor. As such
     * all threads that are to be sequenced must be submitted at once.
     * <p>
     * **Project implements this while the server is waiting for clients,
     * as we can only accept one connection at a time.
     */
    public void executeSequentialThreads(Runnable... runnables) {
	ExecutorService seqExecutor = Executors.newSingleThreadExecutor();
	for (Runnable r : runnables) {
	    seqExecutor.execute(r);
	    LOGGER.log(Level.FINER, "GameResourceManager added new sequential thread.");
	}
	seqExecutors.add(seqExecutor);
    }


    /**
     * Executes threads in parallel. Multiple threads may be queued at once.
     */
    public void executeParallel(Runnable... runnable) {
	for (Runnable r : runnable) {
	    parExecutor.execute(r);
	    LOGGER.log(Level.FINER, "GameResourceManager added new parallel thread.");
	}
    }

    /**
     * Sends interrupt messages to all threads.
     * <p>
     * Note that TCPListener thread may not halt on interrupt
     * because its BufferedReader does not listen for interrupts.
     * <p>
     * As such, the TCPListener threads may remain alive until
     * they realize their socket has shut down. (Time varies.)
     */
    public void shutDownExecutors() {
	for (ExecutorService e : seqExecutors) {
	    e.shutdown();
	    e.shutdownNow();
	    LOGGER.log(Level.FINER, "GameResourceManager shut down a sequence threads.");
	}
	parExecutor.shutdown();
	parExecutor.shutdownNow();
	LOGGER.log(Level.FINER, "GameResourceManager shutdown the parallel thread pool.");
	parExecutor = Executors.newCachedThreadPool();
	LOGGER.log(Level.FINER, "GameResourceManager reset the parallel thread pool.");
	seqExecutors.clear();
	LOGGER.log(Level.FINER, "GameResourceManager purged all sequential threads.");
    }

    /**
     * Opens a server socket on this machine.
     */
    public void startServer(final String address, final int port) throws IOException, SocketException {
	serverSocket = new ServerSocket();
	serverSocket.setReuseAddress(true);
	serverSocket.bind(new InetSocketAddress(address, port));
	allSockets.add(serverSocket);
	LOGGER.log(Level.INFO, "GameResourceManager opened a server socket on address: " + address + ":" + port);
    }

    public String acceptNewConnection() throws SocketGenerationException, IOException {
	if (serverSocket == null) {
	    throw new SocketGenerationException("ServerSocket not initialized!");
	}
	LOGGER.log(Level.INFO, "Listening for message socket..");
	Socket messageSocket = serverSocket.accept();
	LOGGER.log(Level.INFO, "Listening for data socket..");
	Socket dataSocket = serverSocket.accept();
	String socketID;
	socketID = nextSocketID();
	messageSockets.put(socketID, messageSocket);
	dataSockets.put(socketID, dataSocket);
	allSockets.add(messageSocket);
	allSockets.add(dataSocket);

	LOGGER.log(Level.FINER,
		   "GameResourceManager server socket accepted a new connection on port: " + serverSocket.getLocalPort());
	return socketID;
    }

    public String requestNewConnection(final String address, final int portNumber)
	    throws SocketGenerationException, IOException, UnknownHostException, SocketException, SocketTimeoutException,
	    ConnectException
    {
	Socket clientMessageSocket = new Socket(address, portNumber);

	allSockets.add(clientMessageSocket);
	Socket clientDataSocket = new Socket(address, portNumber);
	allSockets.add(clientDataSocket);
	String socketID;
	socketID = nextSocketID();
	messageSockets.put(socketID, clientMessageSocket);
	dataSockets.put(socketID, clientDataSocket);

	LOGGER.log(Level.FINER, "GameResourceManager client socket connected to: " + address + ":" + portNumber);

	return socketID;
    }

    public DataOutputStream getMessageOut(String id) throws SocketGenerationException, IOException {
	if (!messageSockets.containsKey(id)) {
	    throw new SocketGenerationException("Prerequisite sockets not initialized!");
	}
	DataOutputStream messageOut;
	if (!messageOuts.containsKey(id)) {
	    messageOut = new DataOutputStream(messageSockets.get(id).getOutputStream());
	    messageOuts.put(id, messageOut);
	    allSockets.add(messageOut);
	    LOGGER.log(Level.FINER, "GameResourceManager opened a new messageOut socket with ID: " + id);
	} else {
	    messageOut = messageOuts.get(id);
	    LOGGER.log(Level.FINER, "GameResourceManager shared an existing open messageOut socket with ID: " + id);

	}
	return messageOut;
    }

    public BufferedReader getMessageIn(String id) throws SocketGenerationException, IOException {
	if (!messageSockets.containsKey(id)) {
	    throw new SocketGenerationException("Prerequisite sockets not initialized!");
	}

	BufferedReader messageIn;
	if (!messageIns.containsKey(id)) {
	    messageIn = new BufferedReader(new InputStreamReader(messageSockets.get(id).getInputStream()));
	    messageIns.put(id, messageIn);
	    allSockets.add(messageIn);
	    LOGGER.log(Level.FINER, "GameResourceManager opened a new messageIn socket with ID: " + id);

	} else {
	    messageIn = messageIns.get(id);
	    LOGGER.log(Level.FINER, "GameResourceManager shared an existing open messageIn socket with ID: " + id);
	}
	return messageIn;
    }

    public ObjectOutputStream getDataOut(String id) throws SocketGenerationException, IOException {
	if (!dataSockets.containsKey(id)) {
	    throw new SocketGenerationException("Prerequisite sockets not initialized!");
	}

	ObjectOutputStream dataOut;
	if (!dataOuts.containsKey(id)) {
	    dataOut = new ObjectOutputStream(dataSockets.get(id).getOutputStream());
	    dataOuts.put(id, dataOut);
	    allSockets.add(dataOut);
	    LOGGER.log(Level.FINER, "GameResourceManager opened a new dataOut socket with ID: " + id);
	} else {
	    dataOut = dataOuts.get(id);
	    LOGGER.log(Level.FINER, "GameResourceManager shared an existing open dataOut socket with ID: " + id);

	}
	return dataOut;
    }

    public ObjectInputStream getDataIn(String id) throws SocketGenerationException, IOException {
	if (!dataSockets.containsKey(id)) {
	    throw new SocketGenerationException("Prerequisite sockets not initialized!");
	}
	ObjectInputStream dataIn;
	if (!dataIns.containsKey(id)) {
	    dataIn = new ObjectInputStream(dataSockets.get(id).getInputStream());
	    dataIns.put(id, dataIn);
	    allSockets.add(dataIn);
	    LOGGER.log(Level.FINER, "GameResourceManager shared an existing open dataIn socket with ID: " + id);
	} else {
	    dataIn = dataIns.get(id);
	    LOGGER.log(Level.FINER, "GameResourceManager shared an existing open dataIn socket with ID: " + id);

	}
	return dataIn;
    }

    public void closeSockets(final String socketID){
	LOGGER.log(Level.FINER, "GameResourceManager started closing sockets.");
	try {
	    messageSockets.get(socketID).close();
	    dataSockets.get(socketID).close();
	} catch (IOException e) {
	    LOGGER.log(Level.WARNING, "Failed to properly close socket. Resources may be leaking.", e);
	}

	LOGGER.log(Level.FINER, "GameResourceManager finished closing sockets.");

    }

    public void purgeGameResources() throws IOException {
	LOGGER.log(Level.FINER, "GameResourceManager started purging game resources.");
	for (Closeable socket : allSockets) {
	    if (socket != null) { socket.close();}
	}
	serverSocket = null;
	dataSockets = new HashMap<>();
	messageIns = new HashMap<>();
	messageOuts = new HashMap<>();
	dataIns = new HashMap<>();
	dataOuts = new HashMap<>();

	shutDownExecutors();
	LOGGER.log(Level.FINER, "GameResourceManager finished purging game resources.");
    }

    public void closeServerSocket() throws IOException {
	serverSocket.close();
	serverSocket = null;
    }

    public boolean isSocketDead(final String socketID) {
	if (messageSockets.get(socketID) == null) {
	    return true;
	}
        return messageSockets.get(socketID).isClosed();
    }
}
