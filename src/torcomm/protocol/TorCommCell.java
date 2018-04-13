package torcomm.protocol;

/**
 * This is a simple class developed uniquely for experimental purposes and it holds no other function than
 * simply organizing all the information corresponding to its fields into one single structure. There is
 * no special reason for encapsulation in this class as it is its design intention to give the greatest
 * possibe control and access for all classes that intend to use it.
 *
 * @author Daniel G. Maia Filho
 */
public class TorCommCell
{
	public short senderID;
	public short receiverID;
	public short year;
	public byte month;
	public byte day;
	public byte hour;
	public byte minute;
	public byte second;
	public short millisecond;
	public byte endConnection;
	public int payload;
	
	/**
	* Prints out this object's properties into a {@link String String}.
	*/
	@Override
	public String toString()
	{
		return
			"senderID: " + senderID + "\n" +
			"receiverID: " + receiverID + "\n" +
			"year: " + year + "\n" +
			"month: " + month + "\n" +
			"day: " + day + "\n" +
			"hour: " + hour + "\n" +
			"minute: " + minute + "\n" +
			"second: " + second + "\n" +
			"millisecond: " + millisecond + "\n" +
			"endConnection: " + endConnection + "\n" +
			"payload: " + payload + "\n";
	}
}