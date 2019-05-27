package common;

import utils.FileUtil;

import java.awt.print.Book;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static common.DataBaseUtil.*;
import static utils.Config.REPORT_PATH;
import static utils.MyLogger.LOGGER;

/**
 * Created by Wing on 2019/5/24
 * 负责处理书籍丢失情况
 * <p>
 * Project: SaveResult2DB
 */
public class BookLoss {
    /**
     * 定义几天未出现算丢失
     */
    public static int MAX_LOSS_DAY = 7;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    public static Date REPORT_DATE;
//    public static Map<String, String> BOOK_LAST_DATE;

    public static Map<String, Map<String, String>> BOOK_ALL_LAST_DATE;
    private static String BOOK_LAST_DATE_PATH = REPORT_PATH + "\\lastdate.txt";
    private static String BOOK_LAST_DATE_DIR = REPORT_PATH + "\\Lastdate";


    public BookLoss(String reportDate) {
        BOOK_ALL_LAST_DATE = new HashMap<>();
//        Map<String,String> map1 = new HashMap<>();
//        Map<String,String> map2 = new HashMap<>();
//        Map<String,String> map3 = new HashMap<>();
//        Map<String,String> map4 = new HashMap<>();
//        BOOK_ALL_LAST_DATE.put("A2",map1);
//        BOOK_ALL_LAST_DATE.put("A3",map2);
//        BOOK_ALL_LAST_DATE.put("A4",map3);
//        BOOK_ALL_LAST_DATE.put("A5",map4);

//        BOOK_LAST_DATE = new HashMap<>();
        try {
            REPORT_DATE = df.parse(reportDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        File file = new File(BOOK_LAST_DATE_PATH);
        File dir = new File(BOOK_LAST_DATE_DIR);
        if (!file.exists() || !dir.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            dir.mkdirs();
            String sql = "SELECT TAG_ID, BOOK_INDEX FROM " +
                    DB_MAIN_NAME + "." + TABLE_MAIN_NAME +
                    " WHERE CURRENT_LIBRARY= 'WL30'";
            LOGGER.info("获取所有图书信息...");
            getDBConnection();
            createStatement();
            try {
                int count = 0;
                resultSet = DataBaseUtil.statement.executeQuery(sql);
                while (resultSet.next()) {
                    String tagID = resultSet.getString("TAG_ID");
                    String bookIndex = resultSet.getString("BOOK_INDEX");
                    Date date = new Date(System.currentTimeMillis() - (MAX_LOSS_DAY + 3) * 24 * 3600 * 1000);

//                    BOOK_LAST_DATE.put(tagID, df.format(date));
                    String floor = "A" + BookIndex.getFloor(bookIndex);
                    if (!BOOK_ALL_LAST_DATE.containsKey(floor)) {
                        Map<String, String> map = new HashMap<>();
                        BOOK_ALL_LAST_DATE.put(floor, map);
                    }
                    BOOK_ALL_LAST_DATE.get(floor).put(tagID, df.format(date));
                    count++;
                    if (count % 5000 == 0) {
                        LOGGER.info("已获取" + count);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
//            FileUtil.writeFile(BOOK_LAST_DATE_PATH, false, BOOK_LAST_DATE);
            for (Map.Entry<String, Map<String, String>> bookLastDate : BOOK_ALL_LAST_DATE.entrySet()) {
                String floorStr = bookLastDate.getKey();
                FileUtil.writeFile(BOOK_LAST_DATE_DIR + "\\" + floorStr + ".txt", false, bookLastDate.getValue());
            }

        } else {
            File fileDir = new File(BOOK_LAST_DATE_DIR);
            for (String path : fileDir.list()) {
                File f = new File(path);
                List<String> lines = FileUtil.readFileByLine(BOOK_LAST_DATE_DIR + File.separator + path);
                Map<String, String> map = new HashMap<>();
                for (String line : lines) {
                    String s[] = line.split(" ");
                    map.put(s[0], s[1]);
                }
                BOOK_ALL_LAST_DATE.put(f.getName().replaceAll(".txt", ""), map);
            }
        }
        LOGGER.info("获取所有图书最后一次出现时间完毕，共" + BOOK_ALL_LAST_DATE.size() + "楼");
    }

    public void updateDate(String tagID, String bookIndex) {
        int floor = BookIndex.getFloor(bookIndex);
        updateDate(tagID, floor);
    }

    public void updateDate(String tagID, int floor) {
        BOOK_ALL_LAST_DATE.get("A" + floor).put(tagID, df.format(REPORT_DATE));
    }

    public List<String> getAllLoss() {
        List<String> lossTagIdList = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : BOOK_ALL_LAST_DATE.entrySet()) {
            int floor = Integer.parseInt(entry.getKey().substring(1, 2));
            lossTagIdList.addAll(getLoss(floor));
        }
        LOGGER.info("WL30丢失图书总数为" + lossTagIdList.size());
        return lossTagIdList;
    }

    public List<String> getLoss(int floor) {
        List<String> list = new ArrayList<>();
        Map<String, String> map = BOOK_ALL_LAST_DATE.get("A" + floor);
        for (Map.Entry<String, String> m : map.entrySet()) {
            Date oldDate = null;
            try {
                oldDate = df.parse(m.getValue());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (REPORT_DATE.getTime() - oldDate.getTime() > MAX_LOSS_DAY * 24 * 3600 * 1000) {
                list.add(m.getKey());
            }
        }
        return list;
    }

    public void saveBookLossDate() {
        for (Map.Entry<String, Map<String, String>> entry : BOOK_ALL_LAST_DATE.entrySet()) {
            FileUtil.writeFile(BOOK_LAST_DATE_DIR + "\\" + entry.getKey() + ".txt", false, entry.getValue());
        }
    }

    public static void main(String[] args) {
        BookLoss bookLoss = new BookLoss("2019-05-27");
        List<String> list = bookLoss.getLoss(2);
    }
}
