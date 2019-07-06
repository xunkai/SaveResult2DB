package common;

import java.sql.*;

import static utils.MyLogger.LOGGER;
import static utils.MyLogger.getTrace;

/**
 * Created by Wing on 2019/5/24
 * 数据库类，以后主要获取一些图书动态信息，如借出状态等
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


    public static boolean getDBConnection() {
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
            LOGGER.error(getTrace(e));
            return false;
        }
        return true;
    }

    public static void closeDBConnection() {
        if (connect == null) {
            return;
        }
        try {
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
    }

    public static boolean createStatement() {
        if (getDBConnection()) {
            try {
                statement = connect.createStatement();
            } catch (SQLException e) {
                LOGGER.error(getTrace(e));
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 尝试重新连接数据库，请求三次
     *
     * @return
     */
    public static boolean tryCreateStatement() {
        int tryCount = 3;
        while (true) {
            if (!getDBConnection()) {
                tryCount--;
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (tryCount < 0) {
                    break;
                }
            } else {
                try {
                    statement = connect.createStatement();
                } catch (SQLException e) {
                    LOGGER.error(getTrace(e));
                    return false;
                }
                return true;
            }
        }
        return false;
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
            LOGGER.error(getTrace(e));
        }
        closeDBConnection();
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
            LOGGER.error(getTrace(e));
        }
    }
}
