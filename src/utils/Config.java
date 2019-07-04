package utils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import javax.swing.filechooser.FileSystemView;


/**
 * 用户设置
 *
 * @author Wing
 * @version 1.0, 18/09/01
 */
public class Config {
    public static final String DATABASE_URL = "jdbc:sqlite:data" + File.separator + "BookInfo.db";
    public static final String PROJECT_PATH = FileSystemView.getFileSystemView().getHomeDirectory().getPath() + "\\Tooker";
    public static final String REPORT_PATH = PROJECT_PATH + "\\Report";
    public static final String REMAIN_SHELFS_PATH = PROJECT_PATH + "\\Config_I&Q\\RemainShelfs.txt";
    public static String REMAIN_SHELFS_STR;

    public static String DESKTOP_PATH = FileSystemView.getFileSystemView().getHomeDirectory().getPath();
    public static int LOSS_RESET_WEEK = 2;

    public static final String REPORT_END_PATH = REPORT_PATH + "\\REPORT_END";
    public static String REPORT_LOG_PATH = REPORT_PATH + "\\log\\log";
    /**
     * 0 为文件读取,1为数据库
     */
    public static int FLAG = 1;

    /**
     * xml路径
     */
    private static final String XML_PATH = REPORT_PATH + "\\config.xml";

    /**
     * 是否第一次盘点
     */
    public static boolean IS_FIRST = true;

    /**
     * 文件参数
     */
    public static String DB_TXT_PATH;

    /**
     * 数据库ip
     */
    public static String HOST;

    /**
     * 数据库端口
     */
    public static int PORT_NO;

    /**
     * 数据库名
     */
    public static String DB_NAME;

    /**
     * 主表名
     */
    public static String TABLE_MAIN_NAME;

    /**
     * 借出表名
     */
    public static String TABLE_BORROW_NAME;

    /**
     * 用户名
     */
    public static String USERNAME;

    /**
     * 密码
     */
    public static String PASSWORD;

    public static boolean UPDATE_DATABASE;

    public static boolean CHECK_LOSS;

    /**
     * xml的Dom树
     */
    private static Document configDom;

    public static Mail MAIL;
    public static boolean ENABLE_EMAIL;
    public static String EMAIL_HOST;
    public static String EMAIL_PORT;
    public static String EMAIL_FROM_ACCOUNT;
    public static String EMAIL_FROM_NAME;
    public static Map<String, String> EMAIL_TO_ACCOUNT;
    public static String EMAIL_TO_NAME;

    /**
     * Constructs ...
     */
    public Config() {
        try {
            configDom = new SAXReader().read(new File(XML_PATH));
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        Element config = configDom.getRootElement();
        Element configEle = config.element("bookInfo");

        FLAG = configEle.elementText("enableTxt").equals("true")
                ? 0
                : 1;

        Element configDB = configEle.element("databaseConfig");

        HOST = configDB.elementText("host");
        PORT_NO = Integer.parseInt(configDB.elementText("portNo"));
        DB_NAME = configDB.elementText("databaseName");
        TABLE_MAIN_NAME = configDB.elementText("tableMainName");
        TABLE_BORROW_NAME = configDB.elementText("tableBorrowName");
        USERNAME = configDB.elementText("username");
        PASSWORD = configDB.elementText("password");
        DB_TXT_PATH = configEle.element("databaseTxtConfig").elementText("txtPath");
        IS_FIRST = config.elementText("isFirst").equals("true");
        UPDATE_DATABASE = config.elementText("updateDatabase").equals("true");
        CHECK_LOSS = config.elementText("enableCheckLoss").equals("true");
        if (IS_FIRST) {
            config.element("isFirst").setText("false");
        }
        Element configMail = config.element("mailConfig");
        ENABLE_EMAIL = configMail.elementText("enableMail").equals("true");
        EMAIL_HOST = configMail.elementText("emailHost");
        EMAIL_PORT = configMail.elementText("emailPort");
        Element configFrom = configMail.element("from");
        Element configTo = configMail.element("to");
        EMAIL_FROM_ACCOUNT = configFrom.elementText("account");
        EMAIL_FROM_NAME = configFrom.element("account").attributeValue("name");
        EMAIL_TO_ACCOUNT = new HashMap<>();
        List<Element> toList = configTo.elements();
        for (Element toElement : toList) {
            EMAIL_TO_ACCOUNT.put(toElement.getText(), toElement.attributeValue("name"));
        }
        MAIL = new Mail();
        Mail.ENABLE_EMAIL = ENABLE_EMAIL;
        Mail.TO_ACCOUNT_MAP.putAll(EMAIL_TO_ACCOUNT);
        Mail.EMAIL_SMTP_HOST = EMAIL_HOST;
        Mail.EMAIL_SMTP_PORT = EMAIL_PORT;
        Mail.EMAIL_FROM_ACCOUNT = EMAIL_FROM_ACCOUNT;
        Mail.FROM_NAME = EMAIL_FROM_NAME;
    }

    public static void saveXml() {

        //指定文件输出的位置
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(XML_PATH);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 指定文本的写出的格式：
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        //1.创建写出对象
        XMLWriter writer = null;
        try {
            writer = new XMLWriter(out, format);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            //2.写出Document对象
            writer.write(configDom);
            //3.关闭流
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method description
     *
     * @param args
     */
    public static void main(String[] args) {
        Config config = new Config();

        System.out.println(Config.EMAIL_TO_ACCOUNT.size());
        Config.saveXml();
    }
}

