package es.upc.lewis.quadadk.mission;

public class AbortException extends Exception {
	private static final long serialVersionUID = 1L;

	public AbortException() {
		super();
	}
	
	public AbortException(String message) {
		super(message);
	}
}
