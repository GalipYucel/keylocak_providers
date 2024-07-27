package keycloakverfiyapi.dto;

public class VerificationData {
    private final String code;
    private final long timestamp;

    public VerificationData(String code, long timestamp) {
        this.code = code;
        this.timestamp = timestamp;
    }

	public long getTimestamp() {
		return timestamp;
	}

	public String getCode() {
		return code;
	}
}