package es.upc.lewis.quadadk;

public class MissionThread extends Thread {
	CommunicationsThread comms;
	
	public MissionThread(CommunicationsThread comms) {
		this.comms = comms;
	}
	
	@Override
	public void run() {
		arm();
		
		try {
			sleep((4+5) * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		disarm();
	}
	
	private void arm() {
		final int timeToArm = 4; // Seconds
		
		//if (mAccessory == null) { return; }
		
		//Toast.makeText(this, "Arming...", Toast.LENGTH_SHORT).show();
		
		new Thread() {
			public void run() {
				comms.send(Commands.SET_MODE_ALTHOLD);
				
				comms.send(Commands.SET_CH1, 1500);
				comms.send(Commands.SET_CH2, 1500);
				comms.send(Commands.SET_CH3, 1000);
				comms.send(Commands.SET_CH4, 2000);
				
				try {
					sleep(timeToArm * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				comms.send(Commands.SET_CH1, 1500);
				comms.send(Commands.SET_CH2, 1500);
				comms.send(Commands.SET_CH3, 1000);
				comms.send(Commands.SET_CH4, 1500);
			};
		}.start();
	}
	
	private void disarm() {
		final int timeToArm = 4; // Seconds
		
		//if (mAccessory == null) { return; }
		
		//Toast.makeText(this, "Disarming...", Toast.LENGTH_SHORT).show();
		
		new Thread() {
			public void run() {
				comms.send(Commands.SET_CH1, 1500);
				comms.send(Commands.SET_CH2, 1500);
				comms.send(Commands.SET_CH3, 1000);
				comms.send(Commands.SET_CH4, 1000);
				
				try {
					sleep(timeToArm * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				comms.send(Commands.SET_CH1, 1500);
				comms.send(Commands.SET_CH2, 1500);
				comms.send(Commands.SET_CH3, 1000);
				comms.send(Commands.SET_CH4, 1500);
			};
		}.start();
	}
}
