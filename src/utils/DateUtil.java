package utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static utils.MyLogger.LOGGER;

/**
 * 日期工具类
 *
 * Created by Wing on 2018/12/5
 *
 * Project: SaveResult2DB
 */
public class DateUtil {

    /**
     * getCurrentWeek 获取当前日期的星期几
     * <br>星期天：0
     *
     * @param
     * @return int
     * @author IQ624
     * @date 2018/12/5 17:33
     */
    public static int getCurrentWeek(){
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return w;
    }


    /**
     * 获取当前时间字符串，刑如yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getFormatDateString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }

    /**
     * 以特定格式获取当前时间字符串
     *
     * @param format
     * @return
     */
    public static String getFormatDateString(String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(new Date());
    }

    public static long getTimeFromString(String time) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long t = 0;
        try {
            t = dateFormat.parse(time).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return t;
    }

    public static void main(String[] args) {
        System.out.println(getCurrentWeek());
    }
}
