package common;

import java.util.Map;

/**
 * 针对各个学校的类
 * Created by Wing on 2019/6/10
 * <p>
 * Project: SaveResult2DB
 */
public abstract class School {
    public static String SCHOOL_CODE;

    public School(String schoolCode) {
        SCHOOL_CODE = schoolCode;
    }

    public abstract Map<String, BookInfo> getAllBookInfo();

    public abstract String locationEncode(BookLocation bookLocation);

    public abstract BookLocation locationDecode(String locationCode);

}

