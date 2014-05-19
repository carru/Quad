package es.upc.lewis.GroundStation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveData {
	private static final String sensor1 = "Sensor_1";
	
	public static void saveSensor1(int data) {
		PrintWriter writer;
		
		try {
			writer = new PrintWriter(sensor1 + ".txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		writer.println(sensor1);
		writer.println(data);

		writer.close();
	}
	
	public static void savePicture(byte[] picture) {
		String fileName = new SimpleDateFormat("'IMG_'yyyyMMdd_hhmmss'.jpg'").format(new Date());
		FileOutputStream writer;
		
		try {
			writer = new FileOutputStream(fileName);
			writer.write(picture);
			writer.close();
		} catch (IOException e) {
			return;
		}
	}
}
