package es.upc.lewis.quadadk;

public interface Commands {
	// SENSOR RELATED COMMANDS
	public static final byte READ_SENSOR_1 = 0x01;
	public static final byte DATA_SENSOR_1 = 0x11;
	
	public static final byte READ_SENSOR_2 = 0x02;
	public static final byte DATA_SENSOR_2 = 0x12;
	
	public static final byte READ_SENSOR_3 = 0x03;
	public static final byte DATA_SENSOR_3 = 0x13;
	
	
	// RC RELATED COMMANDS
	public static final byte SET_CH1 = (byte) 0xF1;
	public static final byte SET_CH2 = (byte) 0xF2;
	public static final byte SET_CH3 = (byte) 0xF3;
	public static final byte SET_CH4 = (byte) 0xF4;
	public static final byte SET_CH5 = (byte) 0xF5;
	public static final byte SET_CH6 = (byte) 0xF6;
	public static final byte SET_CH7 = (byte) 0xF7;
	public static final byte SET_CH8 = (byte) 0xF8;
	
	
	// GROUNDSTATION COMMANDS
	public static final byte START_MISSION = 0x01;
	public static final byte SENSOR_1 = 0x11;
	public static final byte SENSOR_2 = 0x12;
	public static final byte SENSOR_3 = 0x13;
}
