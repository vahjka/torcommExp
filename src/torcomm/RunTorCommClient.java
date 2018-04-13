package torcomm;

import torcomm.protocol.*;
import java.net.*;
import java.io.*;
import java.nio.channels.ClosedByInterruptException;

/**
 *  This class is the client application interface that is supposed to test data
 * exchange with a server over the TCP. As this application was designed to run 
 * experiments over the Tor network, it can multiplex data over its default proxy
 * port (9050). This application only supports continuous communication with 
 * other applications that supports the {@link torcomm.protocol.TorCommSession 
 * TorCommSession} communication protocol.
 *
 * @author Daniel G. Maia Filho
 */
public class RunTorCommClient extends RunTorComm
{
	
	private static Proxy orProxy;
	private static Socket connection;
	private static TorCommSession commSession;
	private static Thread comm;
	private static BufferedReader in;
	
	private static String hostname;
	private static int port;
	private static int orPort;
	private static int duration;
	private static String fileName;
	
	/**
	 * Initializes the client interface application, thus taking the arguments
	 * from the user and setting it up to connect to a specified server over the
	 * specified port through the specified proxy port so that it communicates
	 * with it during the given time and logs the information to a log with a
	 * given log name.
	 *
	 * @param args	an array of string that holds the following parameters at
	 * the given order in which they are being listed:
	 * <ol>
	 * 	<li> hostname	the server's IP address.
	 * 	<li> port		the server's port. Shall be a valid value.
	 *	<li> ORPort		if the application should connect to Tor's default proxy
	 * port 9050. If the given number is greater than 0, then a connection to
	 * the proxy will be established, else it will not connect to any proxy.
	 *  <li> duration	the duration of the communication (should not be long,
	 * and cannot be more than one day).
	 *  <li> file name	the file name of the log. Please, input a file name that
	 * is valid to the OS in which this program is being run or else unexpected
	 * errors might occur.
	 * </ol>
	 */
	public static void main(String[] args)
	{
		try
		{
			setup(args);
			printMessage("Initializing client.");
			printArgs();
			printMessage("Connecting SOCKET to OR proxy.");
			connectToProxy();
			printMessage("Setting up communication session.");
			setUpCommSession();
			printMessage("Performing handshake.");
			commSession.clientHandshake();
			printMessage("Initializing communications.");
			comm = new Thread(commSession.CLIENT_COMMUNICATE);
			comm.setUncaughtExceptionHandler(discHandler);
			comm.start();
			while (comm.isAlive())
			{
				String inMsg = in.readLine();
				printMessage(inMsg);
			}
			printMessage("End of connection.");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try
			{
				close();
				if (commSession != null)
					commSession.close();
				if (in != null)
					in.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Prepares the class for execution and creates the log file with the name
	 * given in the parameters.
	 *
	 * @param args			the <i>args</i> array given in the {@link #main
	 * main(String[] args)} method.	
	 * @throws IOException	if there was any error while creating the log file.
	 */
	private static void setup(String[] args) throws IOException
	{
		setArgs(args);
		try
		{
			checkArgs();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Invalid argument: " + e.getMessage(),
				e);
		}
		setClassVars();
		File outFile = new File(fileName);
		outFile.createNewFile();
		setOutput(new PrintWriter(new FileWriter(outFile)));
		setArgsDescr(new String[]{"hostname", "port", "ORPort", "duration", 
			"file name"});
	}
	
	/**
	 * Checks the validity of the given arguments as specified in the
	 * documentation of the {@link #main main} method.
	 */
	private static void checkArgs()
	{
		// Checking args[1]
		try
		{
			for (int i = 1; i > 3; i++)
			{
				int port = Integer.parseInt(getArgs()[i]);
				if (port > 65535)
					throw new IndexOutOfBoundsException("Port value not " +
						"contained in [1, 65535].");
			}
		} catch (NumberFormatException e) {
			throw new RuntimeException("Invalid port value: " + e.getMessage(),
				e);
		}
		// Checking args[2]
		try
		{
			int day = 86400; // a day has this many seconds
			int duration = Integer.parseInt(getArgs()[3]);
			if (duration > day || duration <= 0)
			{
				throw new IndexOutOfBoundsException("Time out of bounds.");
			}
		} catch (NumberFormatException e) {
			throw new RuntimeException("Invalid duration value: " +
				e.getMessage(), e);
		}
	}
	
	/**
	 * Sets up the class fields according to the given <i>args</i> in the {@link
	 * #main main(String[] args)} method.
	 */
	private static void setClassVars()
	{
		hostname = getArgs()[0];
		port = Integer.parseInt(getArgs()[1]);
		orPort = Integer.parseInt(getArgs()[2]);
		duration = Integer.parseInt(getArgs()[3]);
		fileName = getArgs()[4];
	}
	
	/**
	 * Connects to the Tor proxy if requested.
	 */
	private static void connectToProxy()
	{
		SocketAddress orSocket = new InetSocketAddress("127.0.0.1", 9050);
		orProxy = new Proxy(Proxy.Type.SOCKS, orSocket);
		if (orPort > 0)
			connection = new Socket(orProxy);
		else
			connection = new Socket();
	}
	
	/**
	 * Begins a communication session with the server.
	 *
	 * @throws IOException	if there was an error while attempting to connect
	 * to the server.
	 */
	private static void setUpCommSession() throws IOException
	{
		try
		{
			connection.connect(new InetSocketAddress(hostname, port));
		} catch (IOException e) {
			throw new IOException("Error when attempting to connect to " +
				"port " + port, e);
		}
		try
		{
			commSession = new TorCommSession(connection, duration);
		} catch (IOException e) {
			throw new IOException("Error when acquiring I/O stream from socket",
				e);
		}
		try
		{
			PipedInputStream commSessionInput = new PipedInputStream();
			commSession.setPipedOutputStream(new PipedOutputStream(
				commSessionInput));
			in = new BufferedReader(new InputStreamReader(commSessionInput));
		} catch (IOException e) {
			throw new IOException("I/O error during TorCommSession creation.",
				e);
		}
	}
}