package torcomm.protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;

/**
 * The protocol for establishing and maintaining the communication with another host that is also 
 * able to communicate through an instance of this class. This class exists only for experimental 
 * purposes as it only exchanges metadata from the {@link torcomm.protocol.TorCommCell TorCommCell}.
 *
 * <p> This class may be instantiated as Client or Server. The constructor requiring duration, that
 * is, {@link #TorCommSession(Socket connection, int duration) TorCommSession(Socket connection, 
 * int duration)}, is used for instantiating this class as a client, while the one that does not require,
 * {@link #TorCommSession(Socket connection) TorCommSession(Socket connection)}, instantiates this
 * class as a server.
 *
 * @author Daniel G. Maia Filho
 */
public class TorCommSession implements Closeable
{
	// IO fields
	private Socket connection;
	private DataInputStream in;
	private DataOutputStream out;
	private String cmd;
	private String currentTask;
	private PrintWriter writer;
	
	// Connection session fields
	private short sessionID;
	private short destID;
	
	// Properties fields
	private int duration;
	
	// Communication fields
	TorCommCell clientCell;
	TorCommCell serverCell;
	
	/**
	 * Creates an instance of this class that enables exchange of {@link torcomm.protocol.TorCommCell 
	 * TorCommCells} as bytes through the given {@link java.net.Socket Socket}. Note that this
	 * Socket must be connected with a destination host.
	 *
	 * <p> This constructor assumes, as it does not requires communication duration, that the
	 * localhost is a server <em>unless</em> called by the {@link #TorCommSession(Socket connection, 
	 * int duration) TorCommSession(Socket connection, int duration)} method.
	 *
	 * @param connection		the socket which must be used to multiplex the communication.
	 * @throws	IOException	if the socket is not connected.
	 */
	public TorCommSession(Socket connection) throws IOException
	{
		this.connection = connection;
		if (!connection.isConnected())
			throw new IOException("Disconnected socket.");
		Random randomGen = new Random();
		this.sessionID = (short)(randomGen.nextInt(Short.MAX_VALUE));
		in = new DataInputStream(connection.getInputStream());
		out = new DataOutputStream(connection.getOutputStream());
	}
	
	/**
	 * Creates an instance of this class that enables exchange of {@link torcomm.protocol.TorCommCell 
	 * TorCommCells} as bytes through the given {@link java.net.Socket Socket}. Note that this
	 * Socket must be connected with a destination host.
	 *
	 * <p> This constructor assumes, as it requires communication duration (which is not defined by the
	 * server, but by the client) that this object will take the role of a client.
	 *
	 * @param connection		the socket through which information will be multiplexed.
	 * @param duration		the communication's duration.
	 * @throws IOException	if the socket is not connected.
	 */
	public TorCommSession(Socket connection, int duration) throws IOException
	{
		this(connection);
		this.duration = duration * 1000;
	}
	
	/**
	 * Establishes a handshake with the server. It is convenient and recommended, though not necessary, 
	 * to handshake as it not only makes sure data is correctly arriving and being sent, but it also
	 * provides with the destination ID before communication begins.
	 *
	 * <p> Note that this is an innapropriate method for servers. Server should use, instead, the method
	 * {@link #serverHandshake() serverHandshake()}.
	 *
	 * @throws IOException	If there is a connection or I/O error while sening or receiving data.
	 */
	public void clientHandshake() throws IOException
	{
		send(ByteBuffer.allocate(8).putShort(this.sessionID).
			array());
		this.destID = ByteBuffer.wrap(retrieve()).getShort();
	}
	
	/**
	 * Establishes a handshake with the client. It is convenient and recommended, though not necessary,
	 * to handshake as it not only makes sure data is correctly arriving and being sent, but it also
	 * provides with the destination ID before communication begins.
	 *
	 * <p> Note that this is an innapropriate method for clients. Clients should use, instead, the method
	 * {@link #clientHandshake() clientHandshake()}.
	 * @throws IOException	If there is a connection or I/O error while sending or receiving data.
	 */
	public void serverHandshake() throws IOException
	{
		this.destID = ByteBuffer.wrap(retrieve()).getShort();
		send(ByteBuffer.allocate(8).putShort(this.sessionID).array());
	}
	
	/**
	 * Retrieves this instance's ID.
	 *
	 * @return	the session ID.
	 */
	public short getSessionID()
	{
		return this.sessionID;
	}
	
	/**
	 * Retrieves the server's ID.
	 *
	 * @return 						the server ID.
	 * @throws NullPointerException	if the handshake was not established 
	 * beforehand.
	 */
	public short getDestID()
	{
		if (this.destID != null)
			return this.destID;
		else
			throw new RunTimeException("Handshake not established. Unknown " +
				"destination ID.");
	}
	
	/**
	 * An implementation of the {@link java.lang.Runnable Runnable} interface that can be run on a {@link
	 * java.lang.Thread Thread} instance to establish client communication with a server.
	 */
	public final Runnable CLIENT_COMMUNICATE = new Runnable()
	{
		
		/**
		 * Starts a communication session with a server and maintain it for the duration set in the
		 * constructor {@link #TorCommSession(Socket connection, int duration) TorCommSession(Socket
		 * connection, int duration)}. After the time has exceeded the set up duration, the client sends
		 * a request to terminate the connection.
		 *
		 * <p> This method will constantly write information regarding the communication to a {@link 
		 * java.io.PipedOutputStream PipedOutputStream} which can be set through the method {@link
		 * #setPipedOutputStream setPipedOutputStream}.
		 */
		public void run()
		{
			if (duration == null)
				throw new RunTimeException("Duration not defined. Is this " +
					"instance a server?");
			try
			{
				long initTime = System.currentTimeMillis();
				int i = 0;
				writeMessage("Communication begun.");
				while (System.currentTimeMillis() - initTime < duration)
				{
					writeMessage("Creating new cell.");
					clientCell = createCell(false);
					writeMessage("Client Cell " + i + " of connection with " +
						"session " + destID + "\n" + clientCell);
					writeMessage("Sending cell to server.");
					send(TorCommDataTranslator.translate(clientCell));
					writeMessage("Waiting for server reply...");
					serverCell = TorCommDataTranslator.translate(retrieve());
					writeMessage("Cell received.");
					writeMessage("Server Cell " + i + " of connection with " + 
						"session " + destID + "\n" + serverCell);
					i++;
				}
				writeMessage("Time out.");
				clientCell = createCell(true);
				writeMessage("Sending terminate request...");
				send(TorCommDataTranslator.translate(clientCell));
				writeMessage("Acknowledging end of connection by server...");
				serverCell = TorCommDataTranslator.translate(retrieve());
				if (serverCell.endConnection > 0)
					writeMessage("Termination acknowledged.");
				else
					writeMessage("Termination was not acknowledged.");
				writeMessage("End of communication.");
			} catch (IOException e) {
				throw new RuntimeException("" + connection.getInetAddress() + 
					" disconnected.", e);
			}
		}
	};
	
	/**
	 * An implementation of the {@link java.lang.Runnable Runnable} interface that can be run on a {@link
	 * java.lang.Thread Thread} instance to establish server communication with a client.
	 */
	public final Runnable SERVER_COMMUNICATE = new Runnable()
	{
		/**
		 * Starts a communication session with a client and maintains it for the duration defined by the
		 * client. After the time has exceeded the set up duration, the client sends a request to 
		 * terminate the connection.
		 *
		 * <p> This method will constantly write information regarding the communication to a {@link 
		 * java.io.PipedOutputStream PipedOutputStream} which can be set through the method {@link
		 * #setPipedOutputStream setPipedOutputStream}.
		 */
		public void run()
		{
			try
			{
				int i = 0;
				TorCommCell clientCell, serverCell;
				writeMessage("Communication has begun.");
				writeMessage("Waiting for client reply...");
				clientCell = TorCommDataTranslator.translate(retrieve());
				writeMessage("Client cell received.");
				while (clientCell.endConnection <= 0)
				{
					writeMessage("Client Cell " + i + " of connection with " + 
						"session " + destID + "\n" + clientCell);
					writeMessage("Creating new cell.");
					serverCell = createCell(false);
					writeMessage("Server Cell " + i + " of connection with " + 
						"session " + destID + "\n" + serverCell);
					writeMessage("Sending new cell.");
					send(TorCommDataTranslator.translate(serverCell));
					writeMessage("Waiting for client reply...");
					clientCell = TorCommDataTranslator.translate(retrieve());
					i++;
				}
				writeMessage("Client requested connection termination.");
				writeMessage("End of connection acknowledged.");
				serverCell = createCell(true);
				send(TorCommDataTranslator.translate(serverCell));
				writeMessage("End of communication.");
			} catch (IOException e) {
				throw new RuntimeException("" + connection.getInetAddress() + 
					" disconnected.", e);
			}
		}
	};
	
	/**
	 * Sets up a {@link java.io.PrintWriter PrintWriter} which will write data to a given {@link java.io.PipedOutputStream
     *	 PipedOutputStream}. The provided PipedOutputStream will constantly output data
	 * from the communication.
	 *
	 * @param outStream	The PipedOutputStream to which communication data should be directed to.
	 */
	public void setPipedOutputStream(PipedOutputStream outStream)
	{
		writer = new PrintWriter(outStream, true);
	}
	
	/**
	 * Sends a message through a {@link java.io.PipedOutputStream PipedOutputStream} that is set up by
	 * {@link #setPipedOutputStream(PipedOutputStream outStream) setPipedOutputStream(PipedOutputStream
	 * outStream)}. If the PipedOutputStream wasn't set, then this method won't send any message at all.
	 *
	 * @param message	the message to be sent through the PipedOutputStream.
	 */
	private void writeMessage(String message)
	{
		if (writer != null)
			writer.println(message);
	}
	
	/**
	 * Generates a {@link torcomm.protocol.TorCommCell TorCommCell} with information that correctly
	 * corresponds to its fields to be used either in the {@link #CLIENT_COMMUNICATE CLIENT_COMMUNICATE}
	 * or {@link #SERVER_COMMUNICATE SERVER_COMMUNICATE} {@link Runnable Runnable} implementation.
	 *
	 * @param endConnection	<i>true</i> if the {@link torcomm.protocol.TorCommCell 
	 * TorCommCell} is supposed to send a connection termination request, and
	 * false otherwise.
	 * @return 				The generated TorCommCell.
	 */
	private TorCommCell createCell(boolean endConnection)
	{
		DateTimeFormatter yearF = DateTimeFormatter.ofPattern("yyyy");
		DateTimeFormatter monthF = DateTimeFormatter.ofPattern("MM");
		DateTimeFormatter dayF = DateTimeFormatter.ofPattern("dd");
		DateTimeFormatter hourF = DateTimeFormatter.ofPattern("HH");
		DateTimeFormatter minuteF = DateTimeFormatter.ofPattern("mm");
		DateTimeFormatter secondF = DateTimeFormatter.ofPattern("ss");
		DateTimeFormatter millisecondF = DateTimeFormatter.ofPattern("SSS");
		
		LocalDateTime now = LocalDateTime.now();
		short cYear = Short.parseShort(yearF.format(now));
		byte cMonth = Byte.parseByte(monthF.format(now));
		byte cDay = Byte.parseByte(dayF.format(now));
		byte cHour = Byte.parseByte(hourF.format(now));
		byte cMinute = Byte.parseByte(minuteF.format(now));
		byte cSecond = Byte.parseByte(secondF.format(now));
		short cMillisecond = Short.parseShort(millisecondF.format(now));
		byte cEndConnection;
		if (endConnection)
			cEndConnection = 1;
		else
			cEndConnection = 0;
		
		TorCommCell cell = new TorCommCell();
		cell.senderID = this.sessionID;
		cell.receiverID = this.destID;
		cell.year = cYear;
		cell.month = cMonth;
		cell.day = cDay;
		cell.hour = cHour;
		cell.minute = cMinute;
		cell.second = cSecond;
		cell.millisecond = cMillisecond;
		cell.endConnection = cEndConnection;
		cell.payload = (new Random()).nextInt(Integer.MAX_VALUE);
		
		return cell;
	}
	
	/**
	 * Inputs a byte array into the connection's output stream.
	 *
	 * @param data 			the byte array to be sent.
	 * @throws IOException	if an I/O or connection error occurs.
	 */
	private void send(byte[] data) throws IOException
	{
		out.write(ByteBuffer.allocate(4).putInt(data.length).array());
		out.write(data);
	}
	
	/**
	 * Retrieves the connection's last output byte array data.
	 *
	 * @return 				the received byte array.
	 * @throws IOException	when an I/O or connection error occurs.
	 * @throws EOFException	when it reaches the end of the stream.
	 */
	private byte[] retrieve() throws IOException, EOFException
	{
		int dataLength = in.readInt();
		byte[] data = new byte[dataLength];
		in.readFully(data);
		return data;
	}
	
	
	/**
	 * Closes the connection and all I/O streams.
	 *
	 * @throws IOException	if an I/O or connection error occurs when closing
	 * down the sockets.
	 */
	public void close() throws IOException
	{
		if (connection != null)
			connection.close();
		if (out != null)
			out.close();
		if (in != null)
			in.close();
	}
}