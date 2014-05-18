package es.upc.lewis.quadadk;

public interface GroundStationCommands {
	public static final byte START_MISSION = 0x01;
	public static final byte ABORT_MISSION = 0x0F;
	
	public static final byte SENSOR_1 = 0x11;
	public static final byte SENSOR_2 = 0x12;
	public static final byte SENSOR_3 = 0x13;
	
	public static final byte PICTURE_START = 0x20;
	public static final byte PICTURE_END = 0x21;
}
