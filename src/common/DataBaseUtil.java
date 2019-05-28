package common;

import java.sql.*;

import static utils.MyLogger.LOGGER;
import static utils.MyLogger.getTrace;

/**
 * Created by Wing on 2019/5/24
 * 数据库类
 * <p>
 * Project: SaveResult2DB
 */
public class DataBaseUtil {
    /**
     * 数据库配置信息
     **/
    public static String HOST = "202.114.65.49";
    /**
     * 数据库端口
     */
    public static int PORT_NO = 1521;
    /**
     * 数据库名
     */
    public static String DB_MAIN_NAME = "RFID";
    /**
     * 状态数据库名
     */
    public static String DB_STATUS_NAME = "ALEPH";
    /**
     * 主表名
     */
    public static String TABLE_MAIN_NAME = "m_transform_tag";
    /**
     * 借出表名
     */
    public static String TABLE_BORROW_NAME = "borrow_list";
    /**
     * 状态表名，保存书籍状态信息
     */
    public static String TABLE_STATUS_NAME = "z30";
    /**
     * 用户名
     */
    public static String USERNAME = "autopd";
    /**
     * 密码
     */
    public static String PASSWORD = "123456";

    public static Connection connect = null;
    public static Statement statement = null;
    public static ResultSet resultSet = null;


    public static void getDBConnection() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            LOGGER.error(getTrace(e));
        }
        try {
            //实例化驱动程序类
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            //驱动程序名：@主机名/IP：端口号：数据库实例名
            String url = "jdbc:oracle:thin:@" + HOST + ":" + PORT_NO + ":" + DB_MAIN_NAME;
//            LOGGER.info(url);
            connect = DriverManager.getConnection(url, USERNAME, PASSWORD);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void closeDBConnection() {
        if (connect == null) {
            return;
        }
        try {
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void createStatement() {
        try {
            if (connect.isClosed()) {
                getDBConnection();
            }
            statement = connect.createStatement();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void closeStatement() {
        if (statement == null) {
            return;
        }
        try {
            if (!statement.isClosed()) {
                statement.close();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void closeResultSet() {
        if (resultSet == null) {
            return;
        }
        try {
            if (!resultSet.isClosed()) {
                resultSet.close();
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
