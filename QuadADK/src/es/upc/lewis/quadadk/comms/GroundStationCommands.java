package es.upc.lewis.quadadk.comms;

public interface GroundStationCommands {
	public static final byte START_MISSION = 0x01;
	public static final byte ABORT_MISSION = 0x0F;
	
	public static final byte SENSOR_TEMPERATURE = 0x11;
	public static final byte SENSOR_HUMIDITY    = 0x12;
	public static final byte SENSOR_NO2         = 0x13;
	public static final byte SENSOR_CO          = 0x14;
	
	public static final byte PICTURE_START = 0x20;
	public static final byte PICTURE_END   = 0x21;
	
	public static final byte MISSION_START = 0x30;
	public static final byte MISSION_END   = 0x31;
	
	public static final byte ACK = (byte) 0xFF;
}
