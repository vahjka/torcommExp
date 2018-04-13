package torcomm;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A class which holds some functions in common among the TorComm interface
 * applications. These functions are mostly responsible for setting up the
 * classes so that setup code in the classes is minimized and repetition is
 * avoided.
 *
 * @author Daniel G. Maia Filho
 */
public abstract class RunTorComm extends Closeable
{
	
	private static String[] args;
	private static String[] argsDescr;
	private static PrintWriter out;
	
	/**
	 * Stores the arguments into a class field so that other methods can access
	 * it.
	 *
	 * @param inArgs	the arguments.
	 */
	protected static void setArgs(String[] inArgs)
	{
		args = inArgs;
	}
	
	/**
	 * Closes the output stream.
	 */
	protected static void close()
	{
		if (out != null)
			out.close();
	}
	
	/**
	 * Initializes a class field so that every other method may access the
	 * connection's {@link PrintWriter PrintWriter} output stream.
	 *
	 * @param output the output stream.
	 */
	protected static void setOutput(PrintWriter output)
	{
		out = output;
	}

	/**
	 * Retrieves the arguments that were given.
	 *
	 * @return the arguments.
	 */
	protected static String[] getArgs()
	{
		return args;
	}
	
	/**
	 * Initializes a field with the descriptions of each argument such that the
	 * index of the description in the array corresponds to the argument's
	 * index.
	 *
	 * @param inArgsDescr	the array of arguments descrciptions.
	 */
	protected static void setArgsDescr(String[] inArgsDescr)
	{
		argsDescr = inArgsDescr;
	}
	
	/**
	 * Returns the argument's description array.
	 *
	 * @return the argument's description array.
	 */
	protected static String[] getArgsDescr()
	{
		return argsDescr;
	}
	
	/**
	 * Prints out the arguments that were set up by the method {@link 
	 * #setArgs(String[] inArgs) setArgs(String[] inArgs)} with their respective
	 * descriptions that was set up by {@link #setArgsDescr(String[]
	 * inArgsDescr) setArgsDescr(String[] inArgsDescr)}.
	 */
	protected static void printArgs()
	{
		for (int i = 0; i < getArgs().length; i++)
			printMessage(getArgsDescr()[i] + ": " + getArgs()[i]);
	}
	
	/**
	 * Prints a message with the date and time at the system default output
	 * stream and at the given output file.
	 *
	 * @param message	the message that shall be printed.
	 */
	protected static void printMessage(String message)
	{
		DateTimeFormatter dtf = DateTimeFormatter.
			ofPattern("MM/dd/yyyy HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String outputMessage = "" + dtf.format(now) + "> " + message;
	if (out != null)
		out.println(outputMessage);
	System.out.println(outputMessage);
	}
	
	/**
	 * Prints an error with the date and time at the system default error
	 * stream and at the given output file.
	 *
	 * @param message	the message that shall be printed.
	 */
	protected static void printError(String message)
	{
		DateTimeFormatter dtf = DateTimeFormatter.
			ofPattern("MM/dd/yyyy HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String outputMessage = "" + dtf.format(now) + "> " + message;
	if (out != null)
		out.println(outputMessage);
	System.out.println(outputMessage);
	}
	
	/**
	 * An exception handler that shall print out all exception messages for
	 * debugging purposes.
	 */
	protected static Thread.UncaughtExceptionHandler discHandler = new 
	Thread.UncaughtExceptionHandler()
	{
		/**
		 * {@inheritDoc}
		 */
		public void uncaughtException(Thread commSession, Throwable e)
		{
			printMessage(e.getMessage());
		}
	};
}