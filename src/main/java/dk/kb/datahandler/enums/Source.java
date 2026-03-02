package dk.kb.datahandler.enums;

public enum Source {
    DR_ARCHIVE("dr_archive");

    private String value;

    private Source(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public String toString() {
        return String.valueOf(this.value);
    }

    public static Source fromValue(String value) {
        for(Source b : values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }

        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
