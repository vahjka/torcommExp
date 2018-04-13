package torcomm.protocol;
import java.nio.ByteBuffer;

/**
 * This class holds two static methods that shall simplify the conversion of {@link torcomm.protocol.TorCommCell
 * TorCommCell} between  a {@link java.lang.String String} and a byte array so that its
 * sending and retrieval from sockets is facilitated.
 *
 * @author Daniel G. Maia Filho
 */
public class TorCommDataTranslator
{
	
	/**
	 * Translates a byte array to a {@link torcomm.protocol.TorCommCell TorCommCell}.
	 *
	 * @param data	The byte array equivalent to the {@link torcomm.protocol.TorCommCell TorCommCell}.
	 * @return		The {@link torcomm.protocol.TorCommCell TorCommCell}.
	 */
	public static TorCommCell translate(byte[] data)
	{
		TorCommCell cell = new TorCommCell();
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		cell.senderID = dataBuffer.getShort();
		cell.receiverID = dataBuffer.getShort();
		cell.year = dataBuffer.getShort();
		cell.month = dataBuffer.get();
		cell.day = dataBuffer.get();
		cell.hour = dataBuffer.get();
		cell.minute = dataBuffer.get();
		cell.second = dataBuffer.get();
		cell.millisecond = dataBuffer.getShort();
		cell.endConnection = dataBuffer.get();
		cell.payload = dataBuffer.getInt();
		return cell;
	}
	
	/**
	 * Translates a {@link torcomm.protocol.TorCommCell TorCommCell} into a byte array.
	 * 
	 * @param cell	The {@link torcomm.protocol.TorCommCell TorCommCell} to be translated to a byte
	 * array.
	 * @return		The byte array equivalent to the given {@link torcomm.protocol.TorCommCell
	 * TorCommCell}.
	 */
	public static byte[] translate(TorCommCell cell)
	{
		ByteBuffer data = ByteBuffer.allocate(22);
		data.putShort(cell.senderID);
		data.putShort(cell.receiverID);
		data.putShort(cell.year);
		data.put(cell.month);
		data.put(cell.day);
		data.put(cell.hour);
		data.put(cell.minute);
		data.put(cell.second);
		data.putShort(cell.millisecond);
		data.put(cell.endConnection);
		data.putInt(cell.payload);
		return data.array();
	}
}