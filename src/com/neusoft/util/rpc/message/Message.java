/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package com.neusoft.util.rpc.message;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import org.apache.thrift.*;
import org.apache.thrift.meta_data.*;
import org.apache.thrift.protocol.*;

public class Message implements TBase, java.io.Serializable, Cloneable {
	private static final TStruct STRUCT_DESC = new TStruct("Message");
	private static final TField CLIENT_NAME_FIELD_DESC = new TField("clientName", TType.STRING, (short) 1);
	private static final TField MESSAGE_FIELD_DESC = new TField("message", TType.STRING, (short) 2);

	private String clientName;
	public static final int CLIENTNAME = 1;
	private String message;
	public static final int MESSAGE = 2;

	private final Isset __isset = new Isset();

	private static final class Isset implements java.io.Serializable {
	}

	public static final Map<Integer, FieldMetaData> metaDataMap = Collections.unmodifiableMap(new HashMap<Integer, FieldMetaData>() {
		{
			put(CLIENTNAME, new FieldMetaData("clientName", TFieldRequirementType.DEFAULT, new FieldValueMetaData(TType.STRING)));
			put(MESSAGE, new FieldMetaData("message", TFieldRequirementType.DEFAULT, new FieldValueMetaData(TType.STRING)));
		}
	});

	static {
		FieldMetaData.addStructMetaDataMap(Message.class, metaDataMap);
	}

	public Message() {
	}

	public Message(String message) {
		this();
		this.message = message;
	}

	public Message(String clientName, String message) {
		this();
		this.clientName = clientName;
		this.message = message;
	}

	/**
	 * Performs a deep copy on <i>other</i>.
	 */
	public Message(Message other) {
		if (other.isSetClientName()) {
			this.clientName = other.clientName;
		}
		if (other.isSetMessage()) {
			this.message = other.message;
		}
	}

	@Override
	public Message clone() {
		return new Message(this);
	}

	public String getClientName() {
		return this.clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public void unsetClientName() {
		this.clientName = null;
	}

	// Returns true if field clientName is set (has been asigned a value) and
	// false otherwise
	public boolean isSetClientName() {
		return this.clientName != null;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void unsetMessage() {
		this.message = null;
	}

	// Returns true if field message is set (has been asigned a value) and false
	// otherwise
	public boolean isSetMessage() {
		return this.message != null;
	}

	public void setFieldValue(int fieldID, Object value) {
		switch (fieldID) {
		case CLIENTNAME:
			if (value == null) {
				unsetClientName();
			} else {
				setClientName((String) value);
			}
			break;

		case MESSAGE:
			if (value == null) {
				unsetMessage();
			} else {
				setMessage((String) value);
			}
			break;

		default:
			throw new IllegalArgumentException("Field " + fieldID + " doesn't exist!");
		}
	}

	public Object getFieldValue(int fieldID) {
		switch (fieldID) {
		case CLIENTNAME:
			return getClientName();

		case MESSAGE:
			return getMessage();

		default:
			throw new IllegalArgumentException("Field " + fieldID + " doesn't exist!");
		}
	}

	// Returns true if field corresponding to fieldID is set (has been asigned a
	// value) and false otherwise
	public boolean isSet(int fieldID) {
		switch (fieldID) {
		case CLIENTNAME:
			return isSetClientName();
		case MESSAGE:
			return isSetMessage();
		default:
			throw new IllegalArgumentException("Field " + fieldID + " doesn't exist!");
		}
	}

	@Override
	public boolean equals(Object that) {
		if (that == null)
			return false;
		if (that instanceof Message)
			return this.equals((Message) that);
		return false;
	}

	public boolean equals(Message that) {
		if (that == null)
			return false;

		boolean this_present_clientName = true && this.isSetClientName();
		boolean that_present_clientName = true && that.isSetClientName();
		if (this_present_clientName || that_present_clientName) {
			if (!(this_present_clientName && that_present_clientName))
				return false;
			if (!this.clientName.equals(that.clientName))
				return false;
		}

		boolean this_present_message = true && this.isSetMessage();
		boolean that_present_message = true && that.isSetMessage();
		if (this_present_message || that_present_message) {
			if (!(this_present_message && that_present_message))
				return false;
			if (!this.message.equals(that.message))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	public void read(TProtocol iprot) throws TException {
		TField field;
		iprot.readStructBegin();
		while (true) {
			field = iprot.readFieldBegin();
			if (field.type == TType.STOP) {
				break;
			}
			switch (field.id) {
			case CLIENTNAME:
				if (field.type == TType.STRING) {
					this.clientName = iprot.readString();
				} else {
					TProtocolUtil.skip(iprot, field.type);
				}
				break;
			case MESSAGE:
				if (field.type == TType.STRING) {
					this.message = iprot.readString();
				} else {
					TProtocolUtil.skip(iprot, field.type);
				}
				break;
			default:
				TProtocolUtil.skip(iprot, field.type);
				break;
			}
			iprot.readFieldEnd();
		}
		iprot.readStructEnd();

		validate();
	}

	public void write(TProtocol oprot) throws TException {
		validate();

		oprot.writeStructBegin(STRUCT_DESC);
		if (this.clientName != null) {
			oprot.writeFieldBegin(CLIENT_NAME_FIELD_DESC);
			oprot.writeString(this.clientName);
			oprot.writeFieldEnd();
		}
		if (this.message != null) {
			oprot.writeFieldBegin(MESSAGE_FIELD_DESC);
			oprot.writeString(this.message);
			oprot.writeFieldEnd();
		}
		oprot.writeFieldStop();
		oprot.writeStructEnd();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Message(");
		boolean first = true;

		sb.append("clientName:");
		if (this.clientName == null) {
			sb.append("null");
		} else {
			sb.append(this.clientName);
		}
		first = false;
		if (!first)
			sb.append(", ");
		sb.append("message:");
		if (this.message == null) {
			sb.append("null");
		} else {
			sb.append(this.message);
		}
		first = false;
		sb.append(")");
		return sb.toString();
	}

	public void validate() throws TException {
		// check for required fields
		// check that fields of type enum have valid values
	}

}
