package common;

import java.util.Map;

public class WhuSchool extends School {
    static {
        SCHOOL_CODE = "WHU";
    }

    public WhuSchool(String schoolCode) {
        super(schoolCode);

    }

    public Map<String, BookInfo> getAllBookInfo() {
        return null;
    }

    public String locationEncode(BookLocation bookLocation) {
        return null;
    }

    public BookLocation locationDecode(String locationCode) {
        return null;
    }

    public static void main(String[] args) {
        System.out.println(WhuSchool.SCHOOL_CODE);
    }
}
