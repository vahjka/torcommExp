package torcomm;

import torcomm.protocol.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.nio.channels.ClosedByInterruptException;

/**
 *  This class is the server application interface that is supposed to test data
 * exchange with a small number of clients over TCP. This application only 
 * supports continuous communication with other applications that supports the 
 * {@link torcomm.protocol.TorCommSession TorCommSession}communication protocol.
 *
 * @author Daniel G. Maia Filho
 */
public class RunTorCommServer extends RunTorComm
{
	
	private static Thread serverThread;
	private static Server server;
	private static int port;
	private static boolean listen;
	
	/**
	 * Initializes the application, thus connecting to the specified server at
	 * the specified port through the 
	 * 
	 * @param args	An array of one string that shall provide the port through
	 * which the server will accept client TCP connections.
	 */
	public static void main(String[] args)
	{
		try
		{
			setup(args);
			printMessage("Initializing server.");
			printArgs();
			printMessage("Start listening to socket connections.");
			startListening();
			BufferedReader reader = new BufferedReader(new 
				InputStreamReader(System.in));
			printMessage("Enter q to close server.");
			String in = reader.readLine();
			while (!in.contentEquals("q"))
			{
				printMessage("Invalid input.");
				printMessage("Enter q to close server.");
				in = reader.readLine();
			}
			printMessage("Closing down server.");
		} catch(Exception e) {
			throw new RuntimeException(e);
			} finally {
				try
				{
					server.servSocket.close();
					serverThread.interrupt();
					serverThread.join();
					printMessage("Server successfully closed.");
					close();
				} catch (Exception e) {
					throw new RuntimeException("Error when closing down " +
						"server.");
				}
		}
	}
	
	/**
	 * Sets up the arguments for the application's execution, thus checking
	 * their validity and making them accessible through the class. It also
	 * creates a .txt file on which it will log the application's status for
	 * debugging purposes.
	 * 
	 * @param args			the args array that comes from the {@link #main
	 * (String[] args) main(String[] args)}.
	 * @throws IOException	if it was not possible to create the log file.
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
		File outPath = new File("output");
		outPath.mkdir();
		String fileName = "output" + File.separator + "mainLog.txt";
		File mainOut = new File(fileName);
		try
		{
			mainOut.createNewFile();
		} catch (IOException e) {
			throw new IOException("An error occurred while creating the file " +
				fileName + ".", e);
		}
		setOutput(new PrintWriter(new FileWriter(mainOut)));
		setArgsDescr(new String[]{"server port"});
	}
	
	/**
	 * Checks the validity of this instance's arguments.
	 *
	 * @throws NumberFormatException	if the <i>port</i> argument is not a valid
	 * port number.
	 */
	private static void checkArgs() throws NumberFormatException
	{
		// Check args[0] - Server port
		try
		{
			int port = Integer.parseInt(getArgs()[0]);
			if (port > 65535 || port < 0)
				throw new IndexOutOfBoundsException("Port value out of range");
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Port value not contained in [1, " +
				"65535].");
		}
	}
	
	/**
	 * Sets up this class fields based on the arguments provided to facilitate
	 * access and code readibility.
	 */
	private static void setClassVars()
	{
		port = Integer.parseInt(getArgs()[0]);
	}
	
	/**
	 * Creates a {@link Server Server} thread that listens for client
	 * connections.
	 *
	 * @throws IOException	if there is an error when opening a socket at the
	 * given argument port.
	 */
	private static void startListening() throws IOException
	{
		try
		{
			listen = true;
			server = new RunTorCommServer().new Server(port);
			serverThread = new Thread(server);
			serverThread.start();
		} catch (IOException e) {
			throw new IOException("Error when opening SOCKET at port " + port, 
				e);
		}
	}
	
	/**
	 * A class capable of holding a thread that can listen for client
	 * connections and start a communication with them through the {@link 
	 * torcomm.protocol.TorCommSession TorCommSession} protocol.
	 */
	private class Server implements Runnable
	{
		private ServerSocket servSocket;
		private TorCommSession session;
		private List<TorCommSession> sessionList;
		private List<Thread> threads;
		private Socket clSocket;
		private Thread comm;
		private Thread pipe;
		private short destID;
		private String logFileName;
		
		/**
		 * Sets up an instance of this class that is capable of listening for
		 * connections through the socket that was set up at the given port.
		 *
		 * @param port			the port at which the server socket shall be set
		 * up at.
		 * @throws IOException	if there was an error while establishing the
		 * socket at the given port.
		 */
		public Server(int port) throws IOException
		{
			destID = -1;
			servSocket = new ServerSocket(port);
		}
		
		/**
		 * Starts a thread of this object that will listen for client
		 * connections and start communicating through the {@link torcomm.protocol.TorCommSession
		 * TorCommSession} protocol.
		 */
		public void run()
		{
			try
			{
				sessionList = new ArrayList<TorCommSession>();
				threads = new ArrayList<Thread>();
				while (listen)
				{
					printMessage("Listening for new connections.");
					clSocket = servSocket.accept();
					printMessage("Connection established from " +
						clSocket.getInetAddress());
					session = new TorCommSession(clSocket);
					sessionList.add(session);
					printMessage("Performing handshake.");
					session.serverHandshake();
					printMessage("New session set up. Client ID: " +
						session.getDestID());
					printMessage("Setting up new log file.");
					setUpLogFile();
					printMessage("Log file " + logFileName + " created.");
					printMessage("Starting communication.");
					comm = new Thread(session.SERVER_COMMUNICATE);
					comm.setUncaughtExceptionHandler(discHandler);
					pipe.start();
					comm.start();
				}
				printMessage("Closing down server.");
			} catch (SocketException e) {
				printMessage("Listening has stopped.");
			} catch(Exception e) {
				throw new RuntimeException("Server error. " + 
					e.getMessage(), e);
			} finally {
				try
				{
					servSocket.close();
					for (Thread thread : threads)
						thread.interrupt();
					for (TorCommSession session : sessionList)
						session.close();
				} catch (Exception e) {
					throw new RuntimeException("An error occurred while " +
						"closing down server thread. " + e.getMessage(), e);
				}
			}
		}
		
		/**
		 * Creates a log file that will be stored in a folder named "output"
		 * such that the log file will contain the client's temporary ID. The
		 * log will have information regarding the application status and the
		 * communication that is being carried out between the server and the
		 * client.
		 *
		 * @throws IOException				if there was an error on piping the
		 * output.
		 * @throws FileNotFoundException	if it was not able to create the log
		 * file.
		 */
		private void setUpLogFile() throws IOException, FileNotFoundException
		{
			try
			{
				DateTimeFormatter dtf = DateTimeFormatter.
					ofPattern("yyyy-MM-dd");
				LocalDateTime now = LocalDateTime.now();
				logFileName = "output" + File.separator + dtf.format(now) + 
					"_" + session.getDestID() + ".txt";
				File sessionOutLog = new File(logFileName);
				PipedInputStream inSession = new PipedInputStream();
				PipedOutputStream outSession = new PipedOutputStream(inSession);
				session.setPipedOutputStream(outSession);
				DataOutputStream outFile = new DataOutputStream(new 
					FileOutputStream(sessionOutLog));
				pipe = new Thread(new Pipe(inSession, outFile));
				threads.add(pipe);
			} catch (FileNotFoundException e) {
				throw new FileNotFoundException("Failed to open file " +
					logFileName);
			} catch (IOException e) {
				throw new IOException("An I/O piping error occurred.", e);
			}
		}
		
		/**
		 * An instance that will handle the piping of the {@link torcomm.protocol.TorCommSession 
		 * TorCommSession} protocol and output it to the log file.
		 */
		private class Pipe implements Runnable
		{
			private InputStream in;
			private OutputStream out;
			
			/**
			 * Sets up the fields of this object.
			 *
			 * @param in	the {@link java.io.InputStream InputStream} from
			 * which the output will be piped.
			 * @param out	the {@link java.io.OutputStream OutputStream}
			 * through which the input will be piped.
			 */
			public Pipe(InputStream in, OutputStream out)
			{
				this.in = in;
				this.out = out;
			}
			
			/**
			 * Starts a piping thread that will get all data from the {@link
			 * torcomm.protocol.TorCommSession TorCommSession} and outputs it 
			 * to the log
			 * file.
			 */
			public void run()
			{
				try
				{
					byte[] buffer = new byte[1024];
					int read = in.read(buffer);
					while (read > -1)
					{
						out.write(buffer, 0, read);
						buffer = new byte[1024];
						read = in.read(buffer);
					}
				} catch (IOException e) {}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					try
					{
						in.close();
						out.close();
					} catch (ClosedByInterruptException e) {}
					catch (Exception e) {
						throw new RuntimeException("Error when closing down " +
							"I/O pipes.", e);
					}
				}
			}
		}
	}
}