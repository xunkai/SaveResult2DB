package common;

import utils.Config;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static common.BookLocation.encodeLocation;
import static common.CuhkszSchool.MAX_LOSS_DAY;
import static common.CuhkszSchool.getBarcodeFromEpc;
import static common.CuhkszSchool.getBookInfo;
import static common.CuhkszSchool.isLegalEPC;
import static utils.DateUtil.getFormatDateString;
import static utils.DateUtil.getTimeFromString;
import static utils.MyLogger.LOGGER;
import static utils.MyLogger.getTrace;

public class SQLiteUtil {
    private static Connection conn = null;
    private static Statement stmt = null;
    private static ResultSet resultSet = null;
    private static String TABLE_NAME = "bookInfo";

    public static boolean createOrConnect() {
        String url = Config.DATABASE_URL;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            LOGGER.error(getTrace(e));
            return false;
        }
        createTable();
        return true;
    }

    public static boolean closeAutoCommit(){
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean commit(){
        try {
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean createStatement() {
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
            return false;
        }
        return true;
    }

    public static void closeConnect() {
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static List<String> getLoss(int floor) {
        List<String> list = new ArrayList<>();
        String sql = "select * from " + TABLE_NAME + " where currentPosition like 'A " + floor + " %'";
        createOrConnect();
        createStatement();
        try {
            resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                String updateTime = resultSet.getString("updateTime");
                String epc = resultSet.getString("EPC");
                long oldTime = getTimeFromString(updateTime);
                if (new Date().getTime() - oldTime > MAX_LOSS_DAY * 24 * 3600 * 1000) {
                    list.add(epc);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (EPC text, Barcode text PRIMARY KEY, BookName text, BookIndex text, CurrentLibrary text, CurrentPosition text, OrderNo text, ShelfBlockNum text, UpdateTime text);";
        try {
            if (conn == null || conn.isClosed()) {
                LOGGER.warn("数据库连接断开，重新连接...");
                createOrConnect();
            }
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        try {
            stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
    }

    public static void insertBookInfo(BookInfo bookInfo) {
        try {
            if (conn == null || conn.isClosed()) {
                createOrConnect();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
        String sql = "INSERT INTO bookInfo(epc, barcode, bookName, bookIndex) VALUES(?,?,?,?);";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, bookInfo.tagId);
            pstmt.setString(2, bookInfo.barcode);
            pstmt.setString(3, bookInfo.bookName);
            pstmt.setString(4, bookInfo.bookIndex);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static BookInfo queryBook(String epc) {
        if (!isLegalEPC(epc)) {
            return null;
        }
        String barcode = getBarcodeFromEpc(epc);
        try {
            if (conn == null || conn.isClosed()) {
                LOGGER.warn("数据库连接断开，重新连接...");
                createOrConnect();
            }
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE EPC=?";
        BookInfo bookInfo = new BookInfo(barcode);
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, epc);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String bookName = rs.getString("bookName");
                String bookIndex = rs.getString("bookIndex");
                bookInfo.barcode = barcode;
                bookInfo.bookName = bookName;
                bookInfo.bookIndex = bookIndex;
            } else {
                bookInfo = getBookInfo(barcode);
                if(bookInfo==null){
                    LOGGER.error("连接图书馆服务器失败！");
                    Thread.sleep(5000);
                    bookInfo = getBookInfo(barcode);
                    if(bookInfo==null){
                        return null;
                    }
                }
                bookInfo.tagId = epc;
                insertBookInfo(bookInfo);
            }

        } catch (Exception e) {
            LOGGER.error(getTrace(e));
            return null;
        }
        return bookInfo;
    }

    public static void updateBookLocation(BookInfo bookInfo) {
        try {
            if (conn == null || conn.isClosed()) {
                LOGGER.warn("数据库连接断开，重新连接...");
                createOrConnect();
            }
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        String sql = "UPDATE " + TABLE_NAME + " SET " +
                "CurrentPosition='" + encodeLocation(bookInfo.location) + "', " +
                "OrderNo='" + bookInfo.location.orderNo + "', " +
                "ShelfBlockNum='" + bookInfo.location.totalNum + "', " +
                "UpdateTime='" + getFormatDateString() + "' " +
                "WHERE Barcode='" + bookInfo.barcode + "'";
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
    }

    public static void main(String[] args) {

        SQLiteUtil.createOrConnect();
        SQLiteUtil.createTable();
        BookInfo b1 = new BookInfo("123", "12333", "12213243", "3535");
        BookInfo b2 = new BookInfo("1233", "3533", "46457", "235346");
        BookInfo b3 = new BookInfo("213", "54855", "45346", "345");
        SQLiteUtil.insertBookInfo(b1);
        SQLiteUtil.insertBookInfo(b2);
        SQLiteUtil.insertBookInfo(b3);

        //该barcode为远程库读取并存储在本地库
        BookInfo test = SQLiteUtil.queryBook("01073");
        System.out.println(test.barcode + " ," + test.bookName + " ," + test.bookIndex);
    }

}
