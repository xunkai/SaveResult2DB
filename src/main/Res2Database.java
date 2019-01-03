package main;

import common.BookIndex;
import common.BookInfo;
import utils.Config;
import utils.MyZip;

import jxl.Workbook;
import jxl.format.CellFormat;
import jxl.write.*;
import jxl.write.biff.RowsExceededException;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.lang.Boolean;
import java.sql.*;
import java.util.*;

import static utils.Config.CHECK_LOSS;
import static utils.DateUtil.getCurrentWeek;
import static utils.FileUtil.*;
import static utils.MyLogger.LOGGER;
import static utils.MyLogger.getTrace;
import static utils.TextUtil.containChinese;

/**
 * 将盘点结果保存至数据库中，并生成报表
 *
 * @author Wing
 * @version 1.0, 18/05/10
 */
public class Res2Database {
    /**
     * 报表存储路径
     **/
    private String reportPath;
    /**
     * 结果文件路径
     **/
    private String resPath;
    /**
     * 丢失报表路径
     */
    private String lossReportPath;
    /**
     * 文件参数
     **/
    private static String DB_TXT_PATH = "data\\DB_m_transform_tag_2018-09-19.txt";

    /**
     * 是否第一次盘点
     */
    private static boolean IS_FIRST = false;

    /**
     * 0 为文件读取,1为数据库
     */
    private static int ENABLE_DATABASE = 1;
    /**
     * 数据库ip
     **/
    private static String HOST = "202.114.65.49";
    /**
     * 数据库端口
     */
    private static int PORT_NO = 1521;
    /**
     * 数据库名
     */
    private static String DB_MAIN_NAME = "RFID";
    /**
     * 状态数据库名
     */
    private static String DB_STATUS_NAME = "ALEPH";
    /**
     * 主表名
     */
    private static String TABLE_MAIN_NAME = "m_transform_tag";
    /**
     * 借出表名
     */
    private static String TABLE_BORROW_NAME = "borrow_list";

    /**
     * 状态表名，保存书籍状态信息
     */
    private static String TABLE_STATUS_NAME = "z30";
    /**
     * 用户名
     */
    private static String USERNAME = "autopd";
    /**
     * 密码
     */
    private static String PASSWORD = "123456";

    private Connection connect = null;
    private Statement statement = null;
    private ResultSet resultSet = null;
    /**
     * 结果文件解析<br>
     * EPC 区域号 楼层号 列号 排号 架号 层号 顺序号 放错等级 当层数量
     **/
//    private String[][] resInfo;
    private static final int FIELD_NUM = 10;


    /**
     * 书籍信息维度
     */
    private static final int BOOK_FIELD_NUM = 12;

    /**
     * 表格列宽
     */
    private static int[] EXCEL_LENGTH = {20, 25, 80, 6, 6, 6, 6, 6, 15, 15};
    /**
     * 错误馆藏地图书
     */
    private TreeMap<String, String> errorLibBookMap;
    /**
     * 借出却被扫描到的图书，存储的是tag id
     */
    private ArrayList<String> loanBookList;
    /**
     * 预约图书，存储的是tag id
     */
    private ArrayList<String> holdBookList;
    /**
     * 状态异常图书，存储的是tag id
     */
    private ArrayList<String> statusAbnormalBookList;
    /**
     * 丢失书籍集合
     */
//    private HashSet<String> lossSet;
//    private ArrayList<BookInfo> lossList;
    private Map<String, BookInfo> lossMap;
    /**
     * TAG_ID异常图书，存储的是tag id
     */
    private ArrayList<String> tagAbnormalBookList;

    /**
     * 所有借出图书，存储的是book id,patron id
     */
    private Map<String, String> allLoanBookMap;
    /**
     * 所有预约图书，存储的是book id, patron id
     */
    private Map<String, String> allHoldBookMap;
    /**
     * 所有状态异常图书，存储的是tagID, status
     */
    private Map<String, String> allStatusAbnormalBookMap;
    /**
     * 所有初始状态图书，存储的是tagID, bookIndex
     */
    private Map<String, String> allToBeUpdatedMap;
    /**
     * 书的集合
     */
    private Map<String, String[]> bookMap;
    /**
     * 书的有序列表
     */
    private List<Map.Entry<String, String[]>> bookList;
    private LinkedHashMap<String, String> firstBookMap;
    /**
     * 对应图书馆数据库字段
     **/
    private static final String[] DB_FIELD_NAME;

    static {
        DB_FIELD_NAME = new String[]{"TAG_ID", "AREANO", "FLOORNO", "COLUMNNO", "ROWNO", "SHELFNO",
                "LAYERNO", "ORDERNO", "ERRORFLAG", "NUM"};
    }

    private static final int SHEET_NUM = 10;
    /**
     * 错误等级说明
     */
    private static final String[] ERROR_LEVEL = {"暂无实际位置", "位置正确", "区域错误", "楼层错误", "列错误", "排错误", "架错误", "层错误", "顺序错误"};

    private static final BookIndex[] FLOOR_INDEX = {new BookIndex("A"), new BookIndex("F"), new BookIndex("I"), new BookIndex("N")};

    private static final String[] FLOOR_STR = {"A-E","F-H","I-M","N-Z"};
    /**
     * 当前楼层
     */
    private int FLOOR = 2;

    private int countNotInDB = 0, countForeign = 0, countSuccess = 0, countLoss = 0,
            countErrLib = 0, countErr = 0, countLoan = 0, countHold = 0, countStatusAbn = 0;


    /**
     * 书籍信息<br>
     * 条形码 索书号 书名 区域号 楼层号 列号 排号 层号 架号 顺序号 放错等级 数量
     */
    private enum BookFieldName {
        /**
         * 条形码
         */
        BOOK_ID("BOOK_ID", 0),
        /**
         * 索书号
         */
        BOOK_INDEX("BOOK_INDEX", 1),
        /**
         * 书名
         */
        BOOK_NAME("BOOK_NAME", 2),
        AREANO("AREANO", 3),
        FLOORNO("FLOORNO", 4),
        COLUMNNO("COLUMNNO", 5),
        ROWNO("ROWNO", 6),
        SHELFNO("SHELFNO", 7),
        LAYERNO("LAYERNO", 8),
        ORDERNO("ORDERNO", 9),
        ERRORFLAG("ERRORFLAG", 10),
        NUM("NUM", 11);
        private String name;
        private int index;

        BookFieldName(String name, int index) {
            this.name = name;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 初始化
     *
     * @param filePath 结果文件路径
     */

    Res2Database(String filePath) {
//        lossSet = new HashSet<>();
//        lossList = new ArrayList<>();
        lossMap = new HashMap<>();
        statusAbnormalBookList = new ArrayList<>();
        tagAbnormalBookList = new ArrayList<>();
        loanBookList = new ArrayList<>();
        holdBookList = new ArrayList<>();
        allToBeUpdatedMap = new HashMap<>();
        allStatusAbnormalBookMap = new HashMap<>();
        allLoanBookMap = new HashMap<>();
        allHoldBookMap = new HashMap<>();
        errorLibBookMap = new TreeMap<>();
        this.resPath = filePath;
        String fileName = resPath.replaceAll("^.+\\\\", "").replace(".res", ".xls");
        String lossFileName = fileName.replace(".xls", "_loss.xls");
        String fileDir = FileSystemView.getFileSystemView().getHomeDirectory().getPath() + "\\报表";
        File file = new File(fileDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        reportPath = fileDir + File.separator + fileName;
        lossReportPath = fileDir + File.separator + lossFileName;
        File reportFile = new File(reportPath);
        LOGGER.info("报表位置：" + reportPath + "\n丢失报表位置：" + lossReportPath);
        FLOOR = Integer.parseInt(reportFile.getName().substring(1, 2));
        initFromXml();
        LOGGER.info("从XML中初始化设置成功！");
        if (Config.UPDATE_DATABASE) {
            getToBeUpdatedList();
        }
        getAllLoanHoldList();
        getStatusAbnormalList();
        if (getCurrentWeek() == Config.LOSS_RESET_WEEK||getCurrentWeek() == Config.LOSS_RESET_WEEK-1) {
            //重置前两天出丢失报表
            CHECK_LOSS = true;
            getLossList();
        } else {
            CHECK_LOSS = false;
        }
            processRes();
//        } else {
//            getResInfo();
//        }
        //生成报表
        if (CHECK_LOSS) {
            generateLossReport();
        }
        generateReportTemp();
        zipSend();
        if (Config.UPDATE_DATABASE) {
            write2DB();
        }
        //重置LOSS
        if (getCurrentWeek() == Config.LOSS_RESET_WEEK) {
            resetDatabaseLoss();
        }
    }

    /**
     * method_name: initFromXml
     * package: main
     * <p>
     * 从XML文件初始化设置
     *
     * @author: Wing
     * date: 2018/9/5
     * time: 20:30
     */
    private void initFromXml() {
        DB_TXT_PATH = Config.DB_TXT_PATH;
        ENABLE_DATABASE = Config.FLAG;
        HOST = Config.HOST;
        PORT_NO = Config.PORT_NO;
        DB_MAIN_NAME = Config.DB_NAME;
        TABLE_MAIN_NAME = Config.TABLE_MAIN_NAME;
        TABLE_BORROW_NAME = Config.TABLE_BORROW_NAME;
        USERNAME = Config.USERNAME;
        PASSWORD = Config.PASSWORD;
        IS_FIRST = Config.IS_FIRST;
        Config.IS_FIRST = false;
        Config.saveXml();
    }

    /**
     * 重置楼层：FLOOR的丢失状态为丢失
     *
     * @author Wing
     * @date 2018/12/5 20:26
     */
    private void resetDatabaseLoss() {
        String[] floorBookIndex = {"A-E", "E-H", "I-M", "N-Z"};
        LOGGER.info("重置数据中" + FLOOR + "层丢失数据...");
        LOGGER.info("连接数据库...");
        getDBConnection();
        LOGGER.info("连接成功...");
        createStatement();
        if (statement == null) {
            LOGGER.error("statement创建失败！");
        }
        try {
            String sql = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME +
                    " SET IS_LOSS=0 WHERE regexp_like( book_index,'^[" + floorBookIndex[FLOOR - 2] + "]') and CURRENT_LIBRARY= 'WL30'";
            resultSet = statement.executeQuery(sql);
            closeStatement();
            closeDBConnection();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        LOGGER.info("楼层" + FLOOR + "图书数据库字段IS_LOSS重置成功");
    }

    /**
     * 从数据库中获取当前楼层图书信息
     *
     * @param bookInfos 图书信息保存的Map
     */
    private void getAllBookInfoFromDB(Map<String, String[]> bookInfos) {
        String sql = "SELECT TAG_ID, BOOK_ID, BOOK_INDEX, BOOK_NAME, CURRENT_LIBRARY FROM " +
                DB_MAIN_NAME + "." + TABLE_MAIN_NAME +
                " WHERE CURRENT_LIBRARY= 'WL30' and regexp_like( book_index,'^["+FLOOR_STR[FLOOR-2]+"]')";
        LOGGER.info("获取A区"+FLOOR+"楼图书信息...");
        getDBConnection();
        createStatement();
        try {
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String tagID = resultSet.getString("TAG_ID");
                String bookID = resultSet.getString("BOOK_ID");
                String bookIndex = resultSet.getString("BOOK_INDEX");
                String bookName = resultSet.getString("BOOK_NAME");
                String currentLibrary = resultSet.getString("CURRENT_LIBRARY");
                String[] tmp = {bookID,bookIndex,currentLibrary,bookName};
                bookInfos.put(tagID,tmp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        LOGGER.info("获取A区"+FLOOR+"楼图书完毕，图书共"+bookInfos.size()+"册");
    }

    /**
     * 从TXT中获取所有图书信息
     * @param bookInfos 图书信息保存的Map
     */
    private void getAllBookInfoFromTxt(Map<String,String[]> bookInfos){
        //读取TXT中的图书信息
        LOGGER.info("读取TXT中的图书信息...");
        List<String> txtInfos = readFileByLine(DB_TXT_PATH);
        for (String data : txtInfos) {
            String[] infos = data.split("\\|");
            String tagID = infos[0];
            String[] tmp = Arrays.copyOfRange(infos, 1, 5);
            bookInfos.put(tagID, tmp);
        }
        txtInfos.clear();
        LOGGER.info("读取成功");
    }
    
    /**
     * 处理res文件信息
     */
    private void processRes() {

        Map<String, String[]> bookInfosTxt = new TreeMap<>();
        if(ENABLE_DATABASE == 0){
            getAllBookInfoFromTxt(bookInfosTxt);
        }
        else {
            getAllBookInfoFromDB(bookInfosTxt);
        }
        LOGGER.info("读取res文件...");
        bookMap = new TreeMap<>();
        List<String> result = readFileByLine(resPath);
        LOGGER.info("共有" + result.size() + "条待处理");
//        resInfo = new String[result.size()][];
        String[] resIn;

        int i = 0;
        int countRC = 0,countQuery=0;
        LOGGER.info("连接数据库...");
        getDBConnection();
        LOGGER.info("连接成功...");
        try {
            statement = connect.createStatement();
            for (String data : result) {
                data = data.replaceAll("[^\\w|\\d| ]", "");
                String[] bookInfos = new String[BOOK_FIELD_NUM];
//                resInfo[i] = data.split(" ");
//                String tagID = resInfo[i][0];
                resIn = data.split(" ");
                String tagID = resIn[0];
                if (tagID.length() > 0) {
                    //处理借出图书，将首位8改为0
                    char[] arr = tagID.toCharArray();
                    arr[0] = arr[0] == '8' ? '0' : arr[0];
                    tagID = new String(arr);
                    if (tagID.matches("^CD[\\d\\w]+$")) {
                        countRC++;
                        LOGGER.info("层架标：" + tagID);
                        continue;
                    }
                }
//                    if(tagID.equals("010200A10C130041BCF90200")){
//                        break;
//                    }
                if (resIn.length != 10) {
                    System.out.println(resIn.length);
                    LOGGER.info("TAG_ID:" + tagID + "数据异常");
                    continue;
                }
                System.arraycopy(resIn, 1, bookInfos, BookFieldName.AREANO.getIndex(), FIELD_NUM - 1);
                String[] tmp = bookInfosTxt.get(tagID);
                if (tmp == null) {
                    //WL30无此书,查询所有库
//                    tmp = getBookInfo(tagID);
                    tmp = new String[4];
                    tmp[0] = null;
                    String sql = "SELECT BOOK_ID, BOOK_INDEX, BOOK_NAME, CURRENT_LIBRARY FROM " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " WHERE TAG_ID " +
                            "" + "" + "= '" + tagID + "'";
                    resultSet = statement.executeQuery(sql);
                    if (resultSet.next()) {
                        tmp[0] = resultSet.getString(BookFieldName.BOOK_ID.getName());
                        tmp[1] = resultSet.getString(BookFieldName.BOOK_INDEX.getName());
                        tmp[3] = resultSet.getString(BookFieldName.BOOK_NAME.getName());
                        tmp[2] = resultSet.getString("CURRENT_LIBRARY");
                    }
                    countQuery++;
                }
                bookInfos[0] = tmp[0];
                bookInfos[1] = tmp[1];
                bookInfos[2] = tmp[3];
                if (bookInfos[1] == null || bookInfos[0] == null) {
                    //数据库无此书
                    countNotInDB++;
                    LOGGER.warn("数据库未找到TagID=" + tagID + "的书");
                    bookInfos[0] = "数据库未找到";
                    bookInfos[1] = "";
                    bookInfos[2] = "";
                    bookMap.put(tagID, bookInfos);
                    tagAbnormalBookList.add(tagID);
                    continue;
                }
                if(isForeignBook(bookInfos[1],bookInfos[2])){
                    countForeign++;
                    LOGGER.info("外文书："+bookInfos[2]);
                    continue;
                }

                bookMap.put(tagID, bookInfos);
                //也需要更新非本层图书的数据库信息
                if (lossMap.containsKey(tagID) || BookIndex.getFloor(bookInfos[BookFieldName.BOOK_INDEX.getIndex()]) != FLOOR) {
                    lossMap.remove(tagID);
                    //更新丢失列表
                    String sql = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " SET IS_LOSS=1 WHERE TAG_ID = '" + tagID + "'";
                    statement.executeQuery(sql);
                }
                //错误馆藏地的图书
                if (tmp[2] == null) {
                    System.out.println("TagID=" + tagID + "的书,CURRENT_LIBRARY为空");
                }
                else if (!tmp[2].equals("WL30")) {
                    errorLibBookMap.put(tagID, tmp[2]);
                }
                //借出预约列表
                if (allLoanBookMap.containsKey(bookInfos[0])) {
                    loanBookList.add(tagID);
                } else if (allHoldBookMap.containsKey(bookInfos[0])) {
                    holdBookList.add(tagID);
                }
                countSuccess++;
                i++;
                if (i % 1000 == 0) {
                    LOGGER.info("已处理" + i);
                }
            }
            statement.close();
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        LOGGER.info("额外查询数据库"+countQuery+"次;有" + countRC + "个层架标;有" + countNotInDB + "个EPC不在数据库中;有"+countForeign+"外文书;" + countSuccess + "本书处理成功！");
        bookInfosTxt.clear();
        sortBooks();
        getFirstBooks();
    }

    /**
     * 从数据库中获取图书信息
     */
    private void getResInfo() {
        LOGGER.info("获取图书信息...");
        LOGGER.info("连接数据库...");
        bookMap = new TreeMap<>();
        LOGGER.info("读取res文件...");
        List<String> result = readFileByLine(resPath);
        LOGGER.info("共有" + result.size() + "条待处理");
//            resInfo = new String[result.size()][];
        String[] resIn;
        int i = 0;
        countNotInDB = 0;
        int countRC = 0;
        countSuccess = 0;
        getDBConnection();
        createStatement();
        for (String data : result) {
            data = data.replaceAll("[^\\w|\\d| ]", "");
            String[] bookInfos = new String[BOOK_FIELD_NUM];
//                resInfo[i] = data.split(" ");
//                String tagID = resInfo[i][0];
            resIn = data.split(" ");
            String tagID = resIn[0];
            if (tagID.length() > 0) {
                //处理借出图书，将首位8改为0
                char[] arr = tagID.toCharArray();
                arr[0] = arr[0] == '8' ? '0' : arr[0];
                tagID = new String(arr);
                if (tagID.matches("^CD[\\d\\w]+$")) {
                    //排除层架标
                    countRC++;
                    LOGGER.info("层架标：" + tagID);
                    continue;
                }
            }
            if (resIn.length < 10) {
                System.out.println(resIn.length);
                LOGGER.info("TAG_ID:" + tagID + "数据异常");
                continue;
            }
            String sql;
            System.arraycopy(resIn, 1, bookInfos, BookFieldName.AREANO.getIndex(), FIELD_NUM - 1);
            sql = "SELECT BOOK_ID, BOOK_INDEX, BOOK_NAME, CURRENT_LIBRARY FROM " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " WHERE TAG_ID = '" + tagID + "'";

            try {
                resultSet = statement.executeQuery(sql);
                if (resultSet.next()) {
                    bookInfos[0] = resultSet.getString("BOOK_ID");
                    bookInfos[1] = resultSet.getString("BOOK_INDEX");
                    bookInfos[2] = resultSet.getString("BOOK_NAME");
                    String currentLibrary = resultSet.getString("CURRENT_LIBRARY");
                    if (bookInfos[1] == null || bookInfos[0] == null) {
                        //数据库无此书
                        countNotInDB++;
                        LOGGER.warn("数据库未找到TagID=" + tagID + "的书");
                        bookInfos[0] = "数据库未找到";
                        bookInfos[1] = "";
                        bookInfos[2] = "";
                        bookMap.put(tagID, bookInfos);
                        tagAbnormalBookList.add(tagID);
                        continue;
                    }
                    bookMap.put(tagID, bookInfos);
                    if (lossMap.containsKey(tagID) || BookIndex.getFloor(bookInfos[BookFieldName.BOOK_INDEX.getIndex()]) != FLOOR) {
//                        BookInfo bookInfo = new BookInfo(tagID, bookInfos[0], bookInfos[1], bookInfos[2]);
//                        lossList.remove(bookInfo);
//                        lossSet.remove(bookInfos[0]);
                        lossMap.remove(tagID);
                        //更新丢失列表
                        sql = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " SET IS_LOSS=1 WHERE TAG_ID = '" + tagID + "'";
                        statement.executeQuery(sql);
                    }

//                    if(tagID.startsWith("010200A18C2300414C7A0200")){
//                        LOGGER.info(sql);
//                    }
                    //错误馆藏地的图书
                    if (currentLibrary == null) {
                        System.out.println("TagID=" + tagID + "的书,CURRENT_LIBRARY为空");
                    }
                    if (currentLibrary != null && !currentLibrary.equals("WL30")) {
                        errorLibBookMap.put(tagID, currentLibrary);
                    }
                    //借出预约列表
                    if (allLoanBookMap.containsKey(bookInfos[0])) {
                        loanBookList.add(tagID);
                    } else if (allHoldBookMap.containsKey(bookInfos[0])) {
                        holdBookList.add(tagID);
                    }

                } else {
                    //数据库无此书
                    countNotInDB++;
                    LOGGER.warn("数据库未找到TagID=" + tagID + "的书");
                    bookInfos[0] = "数据库未找到";
                    bookInfos[1] = "";
                    bookInfos[2] = "";
                    bookMap.put(tagID, bookInfos);
                    tagAbnormalBookList.add(tagID);
                    continue;
                }
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
                LOGGER.info("尝试重连");
                closeStatement();
                closeDBConnection();
                getDBConnection();
                createStatement();
            }
            i++;
            countSuccess++;
            if (i % 1000 == 0) {
                LOGGER.info("已处理" + i);
            }
        }
        closeStatement();
        closeDBConnection();
        LOGGER.info("有" + countRC + "个层架标;" + "有" + countNotInDB + "个EPC不在数据库中;" + countSuccess + "本书处理成功！");
        sortBooks();
        getFirstBooks();
    }

    /**
     * 排序
     */
    private void sortBooks() {
        LOGGER.info("结果排序中...");
        //排序
        bookList = new ArrayList<>(bookMap.entrySet());
        //升序排序
        bookList.sort((o1, o2) ->
        {
            String[] b1 = o1.getValue().clone();
            String[] b2 = o2.getValue().clone();
            long i1 = 0, i2 = 0;
            //排序，从楼层号到书架层号
            for (int i3 = BookFieldName.FLOORNO.getIndex(); i3 <= BookFieldName.ORDERNO.getIndex(); i3++) {
                if (i3 == BookFieldName.ORDERNO.getIndex()) {
                    i1 *= 2;
                    i2 *= 2;
                }
                if (b1[i3].equals("") || b2[i3].equals("")) {
                    System.out.println(b1.toString());
                    System.out.println(b2.toString());
                }
                i1 = i1 * 100 + Integer.valueOf(b1[i3]);
                i2 = i2 * 100 + Integer.valueOf(b2[i3]);
            }
            int tmp = (int) (i1 - i2);
            if (tmp == 0) {
                tmp = b1[BookFieldName.BOOK_INDEX.getIndex()].compareTo(b2[BookFieldName.BOOK_INDEX.getIndex()]);
            }
            return tmp;
        });
        LOGGER.info("排序完成");
    }

    /**
     * 获取首书列表
     */
    private void getFirstBooks() {
        firstBookMap = new LinkedHashMap<>();
        int preOrder = 99;
        for (Map.Entry<String, String[]> entry : bookList) {
            String[] bookInfos = entry.getValue();
            int order = Integer.parseInt(bookInfos[BookFieldName.ORDERNO.getIndex()]);
            if (order < preOrder) {
                //首书
                String bookIndex = bookInfos[BookFieldName.BOOK_INDEX.getIndex()];
                String bookPlace = locationEncode(bookInfos);
                if (bookIndex == null) {
                    System.out.println(bookPlace);
                }
                if (bookPlace.length() < 6) {
                    LOGGER.warn("bookPlace:" + bookPlace);
                }
                firstBookMap.put(bookPlace, bookIndex);
            }
            preOrder = order;
        }
    }

    /**
     * 从数据库中获取借出和预约图书列表
     */
    private void getAllLoanHoldList() {
        LOGGER.info("获取所有借出预约列表");
        LOGGER.info("连接数据库...");
        getDBConnection();
        LOGGER.info("连接成功...");
        try {
            statement = connect.createStatement();
            if (statement == null) {
                LOGGER.error("statement创建失败！");
            }
            String sql = "SELECT BOOK_ID,FLAG,patronid FROM " + DB_MAIN_NAME + "." + TABLE_BORROW_NAME;
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String flag = resultSet.getString("FLAG");
                String bookID = resultSet.getString("BOOK_ID").replaceAll(" ", "");
                String patronID = resultSet.getString("patronid");
                if (flag.equals("loan")) {
                    allLoanBookMap.put(bookID, patronID);
                } else if (flag.equals("hold")) {
                    allHoldBookMap.put(bookID, patronID);
                }
            }
            statement.close();
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        LOGGER.info("所有借出预约列表");
        LOGGER.info("借出图书共有" + allLoanBookMap.size() + "册.预约图书共有" + allHoldBookMap.size() + "册");
    }

    /**
     * 从数据库中获取丢失列表信息
     */
    private void getLossList() {
        LOGGER.info("获取丢失列表");
        LOGGER.info("连接数据库...");
        getDBConnection();
        LOGGER.info("连接成功...");
        try {
            statement = connect.createStatement();
            String sql = "SELECT TAG_ID,BOOK_ID,BOOK_INDEX,BOOK_NAME" +
                    " FROM " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME +
                    " WHERE IS_LOSS = 0 and CURRENT_LIBRARY= 'WL30'";
//            LOGGER.info(sql);
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String tagID = resultSet.getString("TAG_ID");
                String bookID = resultSet.getString("BOOK_ID");
                String bookIndex = resultSet.getString("BOOK_INDEX");
                String bookName = resultSet.getString("BOOK_NAME");
//                String status = resultSet.getString("Z30_ITEM_PROCESS_STATUS");
                BookInfo bookInfo = new BookInfo(tagID, bookID, bookIndex, bookName);
                if (!allLoanBookMap.containsKey(bookID) && !isForeignBook(bookIndex, bookName) && bookIndex != null) {
                    if (BookIndex.getFloor(bookIndex) == FLOOR) {
                        //丢失图书在当前层
                        if (!allStatusAbnormalBookMap.containsKey(tagID)) {
                            //去除状态异常的图书
                            lossMap.put(tagID, bookInfo);
                        }
                    }
                }
            }
            statement.close();
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
            LOGGER.info("获取丢失列表失败");
            return;
        }
        LOGGER.info("成功获取丢失列表");
        LOGGER.info("目前A" + FLOOR + "丢失图书共计" + lossMap.size() + "册");
    }

    /**
     * 获取初始位置未更新列表
     *
     * @author Wing
     * @date 2018/12/6 20:26
     */
    private void getToBeUpdatedList() {
        LOGGER.info("获取初始位置未更新列表");
        LOGGER.info("连接数据库...");
        getDBConnection();
        LOGGER.info("连接成功...");
        try {
            statement = connect.createStatement();
            if (statement == null) {
                LOGGER.error("statement创建失败！");
            }
            String sql = "SELECT TAG_ID,BOOK_INDEX" +
                    " FROM " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME +
                    " WHERE CURRENT_LIBRARY= 'WL30' and BOOK_PLACE IS NULL";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String tagID = resultSet.getString("TAG_ID");
                String bookIndex = resultSet.getString("BOOK_INDEX");
                allToBeUpdatedMap.put(tagID, bookIndex);
            }
            statement.close();
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        LOGGER.info("获取初始位置未更新列表");
        LOGGER.info("初始位置未更新图书共有" + allToBeUpdatedMap.size() + "册.");
    }

    /**
     * 获取状态异常列表
     * <p>
     *
     * @author Wing
     * @date 2018/12/6 20:26
     */
    private void getStatusAbnormalList() {
        LOGGER.info("获取所有状态异常列表");
        LOGGER.info("连接数据库...");
        getDBConnection();
        LOGGER.info("连接成功...");
        try {
            statement = connect.createStatement();
            if (statement == null) {
                LOGGER.error("statement创建失败！");
            }
            String sql = "SELECT TAG_ID,Z30_ITEM_PROCESS_STATUS" +
                    " FROM " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + "," + DB_STATUS_NAME + "." + TABLE_STATUS_NAME +
                    " WHERE CURRENT_LIBRARY= 'WL30' and trim(" + TABLE_STATUS_NAME + ".Z30_barcode) = " +
                    TABLE_MAIN_NAME + ".BOOK_ID and z30.Z30_ITEM_PROCESS_STATUS is not null and z30.Z30_ITEM_PROCESS_STATUS <> '  '";
            LOGGER.info("SQL:" + sql);
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String tagID = resultSet.getString("TAG_ID");
                String status = resultSet.getString("Z30_ITEM_PROCESS_STATUS");
                if (status == null) {
                    status = "null";
                }
                if (!status.equals("null") && !status.equals("  ") && !status.equals("")) {
                    allStatusAbnormalBookMap.put(tagID, status);
//                    LOGGER.info(status);
                }
            }
            statement.close();
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        LOGGER.info("获得状态异常列表");
        LOGGER.info("状态异常图书共有" + allStatusAbnormalBookMap.size() + "册.");
    }

    /**
     * 是否是外文书
     *
     * @param bookIndex 索书号
     * @param bookName  书名
     * @return 是否是外文书
     */
    private boolean isForeignBook(String bookIndex, String bookName) {
        return  (bookIndex != null && (bookIndex.endsWith("Y") )|| (bookName != null &&!containChinese(bookName)));
    }

    /**
     * 将位置信息更改写回数据库
     */
    private void write2DB() {
        LOGGER.info("将更改写回数据库");
        LOGGER.info("连接数据库...");
        int i = 0, countUpdated = 0;
        if (IS_FIRST) {
            LOGGER.info("First Scan");
        }
        getDBConnection();
        createStatement();
        for (Map.Entry<String, String[]> entry : bookList) {
            String tagID = entry.getKey();
            String[] bookInfos = entry.getValue();
            String checkPlace = locationEncode(bookInfos);
            String sql = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " SET " +
                    "check_level" + "='" + checkPlace + "', " +
                    "BOOK_ORDER_NO" + "='" + bookInfos[BookFieldName.ORDERNO.getIndex()] + "' " +
                    "WHERE " + DB_FIELD_NAME[0] + "='" + tagID + "'";
//            LOGGER.info(sql);
            if (IS_FIRST || allToBeUpdatedMap.containsKey(tagID)) {
                String sqlBookPlace = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " SET " +
                        "book_place='" + checkPlace + "', " +
                        "BOOK_ORDER_NO='" + bookInfos[BookFieldName.ORDERNO.getIndex()] + "' " +
                        "WHERE " + DB_FIELD_NAME[0] + "='" + tagID + "'";

                Boolean isError = bookInfos[BookFieldName.ERRORFLAG.getIndex()].equals("1");

                if (isError) {
                    String bookPlace = getRightBookPlace(bookInfos[BookFieldName.BOOK_INDEX.getIndex()]);
                    sqlBookPlace = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " SET " +
                            "book_place='" + bookPlace + "', " +
                            "BOOK_ORDER_NO" + "='" + bookInfos[BookFieldName.ORDERNO.getIndex()] + "' " +
                            "WHERE " + DB_FIELD_NAME[0] + "='" + tagID + "'";
                }
                if (errorLibBookMap.containsKey(tagID)) {
                    sqlBookPlace = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " SET " +
                            "book_place='', " +
                            "BOOK_ORDER_NO" + "='" + bookInfos[BookFieldName.ORDERNO.getIndex()] + "' " +
                            "WHERE " + DB_FIELD_NAME[0] + "='" + tagID + "'";
                }
                countUpdated++;
                try {
                    statement.executeUpdate(sqlBookPlace);
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                    LOGGER.info("尝试重连");
                    closeStatement();
                    closeDBConnection();
                    getDBConnection();
                    createStatement();
                }
//                try {
//                    statement.executeUpdate(sql);
//                } catch (SQLException e) {
//                    LOGGER.error(e.getMessage());
//                    LOGGER.info("尝试重连");
//                    closeStatement();
//                    closeDBConnection();
//                    getDBConnection();
//                    createStatement();
//                }
            }
            try {
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
                LOGGER.info("尝试重连");
                closeStatement();
                closeDBConnection();
                getDBConnection();
                createStatement();
            }
            if (++i % 1000 == 0) {
                LOGGER.info("已更新" + i);
            }
        }
        int countUpdate = 0;
        for (Map.Entry<String, String> entry : allToBeUpdatedMap.entrySet()) {
            String tagID = entry.getKey();
            String bookIndex = entry.getValue();
            //图书在该层，且图书无初始位置
            if (BookIndex.getFloor(bookIndex) == FLOOR &&
                    allToBeUpdatedMap.containsKey(tagID)) {
                countUpdate++;
                String bookPlace = getRightBookPlace(bookIndex);
                String sqlBookPlace = "UPDATE " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " SET " +
                        "book_place='" + bookPlace +
                        "' WHERE " + DB_FIELD_NAME[0] + "='" + tagID + "'";
                try {
                    statement.executeUpdate(sqlBookPlace);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        closeStatement();
        closeDBConnection();
        LOGGER.info("数据库更新完成!图书初始位置更新" + countUpdated + "册，估计更新" + countUpdate + "册");
        Config.MAIL.sendEmail("A" + FLOOR + ",Database finishes update", "数据库更新已完成");
    }

    /**
     * 通过EPC来获取图书信息
     *
     * @param tagID EPC号
     * @return 书ID 书索引号 书名
     */
    private String[] getBookInfo(String tagID) {
        String bookID = null, bookIndex = "", bookName = "", currentLibrary="";
        getDBConnection();
        try {
            statement = connect.createStatement();
            String sql = "SELECT BOOK_ID, BOOK_INDEX, BOOK_NAME, CURRENT_LIBRARY FROM " + DB_MAIN_NAME + "." + TABLE_MAIN_NAME + " WHERE TAG_ID " +
                    "" + "" + "= '" + tagID + "'";
            resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                bookID = resultSet.getString(BookFieldName.BOOK_ID.getName());
                bookIndex = resultSet.getString(BookFieldName.BOOK_INDEX.getName());
                bookName = resultSet.getString(BookFieldName.BOOK_NAME.getName());
                currentLibrary = resultSet.getString("CURRENT_LIBRARY");
            }
            statement.close();
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        return new String[]{bookID,bookIndex,currentLibrary,bookName};
    }

    /**
     * 通过书籍条形码来获取图书状态信息
     *
     * @param bookID 条形码
     * @return 书籍状态
     */
    private String getBookStatusByBookID(String bookID) {
        String status = "";
        try {
            String sql = "SELECT Z30_ITEM_PROCESS_STATUS FROM " + DB_STATUS_NAME + "." + TABLE_STATUS_NAME +
                    " WHERE Z30_BARCODE like '" + bookID + "%'";
//            LOGGER.info(sql);
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                status = resultSet.getString("Z30_ITEM_PROCESS_STATUS");
            }
        } catch (SQLException e) {
            LOGGER.error(getTrace(e));
        }
        return status;
    }

    /**
     * 通过和首书列表索书号比较，得到正确的图书位置
     *
     * @param bookIndex 索书号
     * @return 正确位置编码
     */
    private String getRightBookPlace(String bookIndex) {
        String bookPlace = "";
        BookIndex b2 = new BookIndex(bookIndex);
        boolean isFirst = true;
        //记录连续小于首书的次数
        int count = 0;
        for (Map.Entry<String, String> entry : firstBookMap.entrySet()) {
            BookIndex b1 = new BookIndex(entry.getValue());
            if (isFirst || b2.compareTo(b1) >= 0) {
                bookPlace = entry.getKey();
                isFirst = false;
                count = 0;
            } else {
                count++;
                if (count == 3) {
                    break;
                }
            }
        }
        if (bookPlace.length() < 6) {
            LOGGER.warn("bookplace不合法：" + bookPlace);
            bookPlace = null;
        }
        return bookPlace;
    }

    /**
     * 将图书信息写入表格中
     *
     * @param sheet     要写入的表格
     * @param bookInfos 图书信息
     * @param format    表格格式
     */
    private void addBookToSheet(WritableSheet sheet, String[] bookInfos, CellFormat format) {
        int rowNo = sheet.getRows();
        if (bookInfos[BookFieldName.BOOK_ID.getIndex()] == null) {
            //数据库图书未找到
            return;
        }
        Label labBookID = new Label(0, rowNo, bookInfos[BookFieldName.BOOK_ID.getIndex()], format);
        Label labBookIndex = new Label(1, rowNo, bookInfos[BookFieldName.BOOK_INDEX.getIndex()], format);
        Label labBookName = new Label(2, rowNo, bookInfos[BookFieldName.BOOK_NAME.getIndex()], format);
        Label labColumnNo = new Label(3, rowNo, bookInfos[BookFieldName.COLUMNNO.getIndex()], format);
        Label labRowNo = new Label(4, rowNo, bookInfos[BookFieldName.ROWNO.getIndex()], format);
        Label labShelfNo = new Label(5, rowNo, bookInfos[BookFieldName.SHELFNO.getIndex()], format);
        Label labLayerNo = new Label(6, rowNo, bookInfos[BookFieldName.LAYERNO.getIndex()], format);
        Label labOrderNo = new Label(7, rowNo, bookInfos[BookFieldName.ORDERNO.getIndex()], format);
        Label labNum = new Label(8, rowNo, bookInfos[BookFieldName.NUM.getIndex()], format);
        Label[] labels = {labBookID, labBookIndex, labBookName, labColumnNo, labRowNo, labShelfNo, labLayerNo,
                labOrderNo, labNum};
        try {
            for (Label label : labels) {
                sheet.addCell(label);
            }
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
    }

    /**
     * 按行和列生成报表
     */
    public void generateReportTemp() {
        LOGGER.info("开始生成报表...");
        WritableWorkbook book = null;
        try {
            book = Workbook.createWorkbook(new File(reportPath));
        } catch (IOException e) {
            LOGGER.error(getTrace(e));
        }
        int count = 0;
        int sheetNum = 0;
        if (book == null) {
            LOGGER.error("报表创建失败！");
            return;
        }
        //统计信息列表
        WritableSheet sheetDashboard = book.createSheet("统计信息", sheetNum);
        sheetNum++;
        //错误列表
        WritableSheet sheetErr = book.createSheet("错架列表", sheetNum);
        sheetNum++;
        //错误馆藏地列表
        WritableSheet sheetErrLib = book.createSheet("错误馆藏地列表", sheetNum);
        sheetNum++;
        //状态异常列表
        WritableSheet sheetStatusAbn = book.createSheet("状态异常列表", sheetNum);
        sheetNum++;
        //tag异常列表
        WritableSheet sheetTagAbn = book.createSheet("TAG异常列表", sheetNum);
        sheetNum++;
        //首书列表
        WritableSheet sheetFirstBook = book.createSheet("首书列表", sheetNum);
        sheetNum++;
        boolean isFirstBook = true;

        //借出列表
        WritableSheet sheetLoan = book.createSheet("借出列表", sheetNum);
        sheetNum++;
        //预约列表
        WritableSheet sheetHold = book.createSheet("预约列表", sheetNum);
        sheetNum++;

        //定义样式
        WritableCellFormat formatText = new WritableCellFormat();
        try {
            formatText.setAlignment(Alignment.CENTRE);
            formatText.setBackground(jxl.format.Colour.GRAY_25);
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
        WritableCellFormat format = new WritableCellFormat();
        // true自动换行，false不自动换行
        try {
            format.setWrap(true);
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
        Label labTagIDText, labBookIDText, labBookIndexText, labBookNameText, labColumnNoText,
                labRowNoText, labShelfNoText, labLayerNoText, labOrderNoText,
                labNumText, labLibraryText, labStatusText;
        WritableSheet[] sheets = {sheetErr, sheetErrLib, sheetStatusAbn, sheetTagAbn, sheetFirstBook, sheetLoan, sheetHold};
        for (WritableSheet sheet : sheets) {
            //添加第一行信息
            // Label labTitle_ = new Label(0, 0, "架号"+data[3]+" 层号"+data[4]);
            labTagIDText = new Label(0, 0, "TagID", formatText);
            labBookIDText = new Label(0, 0, "条形码", formatText);
            labBookIndexText = new Label(1, 0, "索书号", formatText);
            labBookNameText = new Label(2, 0, "书名", formatText);
            labColumnNoText = new Label(3, 0, "列号", formatText);
            labRowNoText = new Label(4, 0, "排号", formatText);
            labShelfNoText = new Label(5, 0, "架号", formatText);
            labLayerNoText = new Label(6, 0, "层号", formatText);
            labOrderNoText = new Label(7, 0, "顺序号", formatText);
            labNumText = new Label(8, 0, "书格图书总数", formatText);
            labLibraryText = new Label(9, 0, "应在馆藏地", formatText);
            labStatusText = new Label(9, 0, "状态", formatText);
            Label labRightColumnNoText = new Label(9, 0, "列号", formatText);
            Label labRightRowNoText = new Label(10, 0, "排号", formatText);
            Label labRightShelfNoText = new Label(11, 0, "架号", formatText);
            Label labRightLayerNoText = new Label(12, 0, "层号", formatText);
            Label labRightLibraryText = new Label(13, 0, "应在馆藏地", formatText);
            Label labPatronIDText = new Label(9, 0, "借书ID", formatText);
            Label[] labelsText = {labBookIDText, labBookIndexText, labBookNameText, labColumnNoText, labRowNoText, labShelfNoText,
                    labLayerNoText, labOrderNoText, labNumText};
            try {
                for (Label labelText : labelsText) {
                    sheet.addCell(labelText);
                }
                if (sheet.getName().equals("错误馆藏地列表")) {
                    sheet.addCell(labLibraryText);
                }
                if (sheet.getName().equals("状态异常列表")) {
                    sheet.addCell(labStatusText);
                }
                if (sheet.getName().equals("TAG异常列表")) {
                    sheet.addCell(labTagIDText);
                }
                if (sheet.getName().equals("错架列表")) {
                    sheet.addCell(labRightColumnNoText);
                    sheet.addCell(labRightRowNoText);
                    sheet.addCell(labRightShelfNoText);
                    sheet.addCell(labRightLayerNoText);
                    sheet.addCell(labRightLibraryText);
                }
                if (sheet.getName().equals("借出列表") || sheet.getName().equals("预约列表")) {
                    sheet.setColumnView(9, 12);
                    sheet.addCell(labPatronIDText);
                }
            } catch (WriteException e) {
                LOGGER.error(getTrace(e));
            }
            //表格格式设置，固定列宽和自动换行
            for (int i = 0; i < EXCEL_LENGTH.length; i++) {
                sheet.setColumnView(i, EXCEL_LENGTH[i]);
            }
        }
        int preOrder = 99;
        getDBConnection();
        createStatement();
        for (Map.Entry<String, String[]> entry : bookList) {
//            count++;
//            if (count % 2000 == 0) {
//                LOGGER.info("已处理" + count);
//            }
            String tagID = entry.getKey();
            String[] bookInfos = entry.getValue();
            String tmp = bookInfos[BookFieldName.COLUMNNO.getIndex()] + "列 " + bookInfos[BookFieldName.ROWNO.getIndex
                    ()] + "排";
            int currOrder = Integer.parseInt(bookInfos[BookFieldName.ORDERNO.getIndex()]);
            if (currOrder < preOrder) {
                isFirstBook = true;
            }
            preOrder = currOrder;
            WritableSheet sheet;
            if ((sheet = book.getSheet(tmp)) == null) {
                //不存在此sheet
                sheet = book.createSheet(tmp, sheetNum);
                sheetNum++;
                // Label labTitle_ = new Label(0, 0, "架号"+data[3]+" 层号"+data[4]);
                labBookIDText = new Label(0, 0, "条形码", formatText);
                labBookIndexText = new Label(1, 0, "索书号", formatText);
                labBookNameText = new Label(2, 0, "书名", formatText);
                labColumnNoText = new Label(3, 0, "列号", formatText);
                labRowNoText = new Label(4, 0, "排号", formatText);
                labShelfNoText = new Label(5, 0, "架号", formatText);
                labLayerNoText = new Label(6, 0, "层号", formatText);
                labOrderNoText = new Label(7, 0, "顺序号", formatText);
                labNumText = new Label(8, 0, "书格图书总数", formatText);
                Label[] newLabelsText = {labBookIDText, labBookIndexText, labBookNameText, labColumnNoText, labRowNoText, labShelfNoText,
                        labLayerNoText, labOrderNoText, labNumText};
                try {
                    for (Label aNewLabelsText : newLabelsText) {
                        sheet.addCell(aNewLabelsText);
                    }

                } catch (WriteException e) {
                    LOGGER.error(getTrace(e));
                }
                //表格格式设置，固定列宽和自动换行
                for (int i = 0; i < EXCEL_LENGTH.length; i++) {
                    sheet.setColumnView(i, EXCEL_LENGTH[i]);
                }
            }
            //添加书本信息
            addBookToSheet(sheet, bookInfos, format);
            String str1 = "1";
            if (str1.equals(bookInfos[BookFieldName.ERRORFLAG.getIndex()])||errorLibBookMap.containsKey(tagID)) {
                //将错架图书和错误馆藏地图书加入错架列表
                addBookToSheet(sheetErr, bookInfos, format);
                String bookP = getRightBookPlace(bookInfos[BookFieldName.BOOK_INDEX.getIndex()]);
                if (bookP == null) {
                    continue;
                }
                String[] bookPlace = locationDecode(bookP);
                //加入正确位置
                int rowNo = sheetErr.getRows() - 1;
//                Label labAreaNo = new Label(9, rowNo, bookPlace[0], format);
//                Label labFloorNo = new Label(10, rowNo, bookPlace[1], format);
                Label labColumnNo = new Label(9, rowNo, bookPlace[2], format);
                Label labRowNo = new Label(10, rowNo, bookPlace[3], format);
                Label labShelfNo = new Label(11, rowNo, bookPlace[4], format);
                Label labLayerNo = new Label(12, rowNo, bookPlace[5], format);
                Label labRightLibrary;
                if (errorLibBookMap.containsKey(tagID)) {
                    labRightLibrary = new Label(13, rowNo, errorLibBookMap.get(tagID), format);
                } else {
                    labRightLibrary = new Label(13, rowNo, "", format);
                }
                Label[] labels = {labColumnNo, labRowNo, labShelfNo, labLayerNo, labRightLibrary};
                try {
                    for (Label label : labels) {
                        sheetErr.addCell(label);
                    }
                } catch (WriteException e) {
                    LOGGER.error(getTrace(e));
                }
            }
            if (errorLibBookMap.containsKey(tagID)) {
                //错误馆藏地的图书
                addBookToSheet(sheetErrLib, bookInfos, format);
                Label labLibrary = new Label(9, sheetErrLib.getRows() - 1, errorLibBookMap.get(tagID), format);
                try {
                    sheetErrLib.addCell(labLibrary);
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            }
            if (allStatusAbnormalBookMap.containsKey(tagID)) {
                //状态异常图书
                String status = allStatusAbnormalBookMap.get(tagID);
//                LOGGER.info(status);
                addBookToSheet(sheetStatusAbn, bookInfos, format);
                Label labStatus = new Label(9, sheetStatusAbn.getRows() - 1, status, format);
                try {
                    sheetStatusAbn.addCell(labStatus);
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            }
            if (tagAbnormalBookList.contains(tagID)) {
                addBookToSheet(sheetTagAbn, bookInfos, format);
                Label labTagID = new Label(0, sheetTagAbn.getRows() - 1, tagID, format);
                try {
                    sheetTagAbn.addCell(labTagID);
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            }
            if (loanBookList.contains(tagID)) {
                //借出图书
                addBookToSheet(sheetLoan, bookInfos, format);
                Label labLibrary = new Label(9, sheetLoan.getRows() - 1, allLoanBookMap.get(bookInfos[0]), format);
                try {
                    sheetLoan.addCell(labLibrary);
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            }
            if (holdBookList.contains(tagID)) {
                //预约图书
                addBookToSheet(sheetHold, bookInfos, format);
                Label labLibrary = new Label(9, sheetHold.getRows() - 1, allHoldBookMap.get(bookInfos[0]), format);
                try {
                    sheetHold.addCell(labLibrary);
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            }
            if (isFirstBook) {
                //首书列表
                addBookToSheet(sheetFirstBook, bookInfos, format);
                isFirstBook = false;
            }
        }
        closeStatement();
        closeDBConnection();
        countErr = sheetErr.getRows() - 1;
        countErrLib = sheetErrLib.getRows() - 1;
        countHold = sheetHold.getRows() - 1;
        countLoan = sheetLoan.getRows() - 1;
        countStatusAbn = sheetStatusAbn.getRows() - 1;
        WritableCellFormat formatTitle = new WritableCellFormat();
        try {
            formatTitle.setAlignment(jxl.format.Alignment.CENTRE);
            formatTitle.setBorder(jxl.format.Border.BOTTOM, jxl.format.BorderLineStyle.THICK, jxl.format.Colour.RED);
        } catch (WriteException e) {
            e.printStackTrace();
        }
        sheetDashboard.setColumnView(0, 25);
        sheetDashboard.setColumnView(1, 15);
        try {
            sheetDashboard.setRowView(0, 500);
        } catch (RowsExceededException e) {
            e.printStackTrace();
        }

        //获取跳过书架
        Config.REMAIN_SHELFS_STR = readFileByLine(Config.REMAIN_SHELFS_PATH).get(0);
        deleteFile(Config.REMAIN_SHELFS_PATH);
        Label labTitle = new Label(0, 0, "盘点统计信息", formatTitle),
                labFloorText = new Label(0, 1, "盘点楼层", formatText),
                labTotalText = new Label(0, 2, "扫描图书总数", formatText),
                labLossText = new Label(0, 3, "丢失图书总数", formatText),
                labQueryFailureText = new Label(0, 4, "TAG_ID查询失败", formatText),
                labErrorText = new Label(0, 5, "错架图书", formatText),
                labErrorLibText = new Label(0, 6, "错馆图书", formatText),
                labLoanText = new Label(0, 7, "借出但被扫描到", formatText),
                labHoldText = new Label(0, 8, "预约图书", formatText),
                labStatusAbnText = new Label(0, 9, "状态异常图书", formatText),
                labRemainShelfText = new Label(0, 10, "跳过书架", formatText),
                labFloor = new Label(1, 1, FLOOR + "", format),
                labTotal = new Label(1, 2, countSuccess + "", format),
                labQueryFailure = new Label(1, 4, countNotInDB + "", format),
                labError = new Label(1, 5, countErr + "", format),
                labErrorLib = new Label(1, 6, countErrLib + "", format),
                labLoan = new Label(1, 7, countLoan + "", format),
                labHold = new Label(1, 8, countHold + "", format),
                labStatusAbn = new Label(1, 9, countStatusAbn + "", format),
                labRemainShelf = new Label(1, 10, Config.REMAIN_SHELFS_STR + "", format);
        Label labLoss;
        if (CHECK_LOSS) {
            labLoss = new Label(1, 3, countLoss + "", format);
        } else {
            labLoss = new Label(1, 3, "请等待周" + Config.LOSS_RESET_WEEK + "的丢失报表", format);
        }
        Label[] labs = {labTitle, labFloorText, labFloor,
                labTotalText, labTotal,
                labLossText, labLoss,
                labQueryFailureText, labQueryFailure,
                labErrorText, labError,
                labErrorLibText, labErrorLib,
                labLoanText, labLoan,
                labHoldText, labHold,
                labStatusAbnText, labStatusAbn,
                labRemainShelfText, labRemainShelf};
        for (Label lab : labs) {
            try {
                sheetDashboard.addCell(lab);
            } catch (WriteException e) {
                e.printStackTrace();
            }
        }
        try {
            sheetDashboard.mergeCells(0, 0, 1, 0);
        } catch (WriteException e) {
            e.printStackTrace();
        }
        try {
            book.write();
            book.close();
        } catch (IOException | WriteException e) {
            LOGGER.error(getTrace(e));
        }
        LOGGER.info("报表生成！");
        LOGGER.info("错架图书：" + countErr);
        LOGGER.info("错馆图书：" + countErrLib);
        LOGGER.info("借出图书：" + countLoan);
        LOGGER.info("预约图书：" + countHold);
    }

    /**
     * 生成丢失报表
     */
    public void generateLossReport() {
        LOGGER.info("生成丢失报表...");
        List<Map.Entry<String, BookInfo>> lossList = new ArrayList<>(lossMap.entrySet());
        Collections.sort(lossList, Map.Entry.comparingByValue());
//        Collections.sort(lossList);
        WritableWorkbook book = null;
        try {
            book = Workbook.createWorkbook(new File(lossReportPath));
        } catch (IOException e) {
            LOGGER.error(getTrace(e));
        }
        int sheetNum = 0;
        //丢失列表
        if (book == null) {
            LOGGER.error("丢失报表创建失败！");
        }
        WritableSheet sheetLoss = book.createSheet("丢失列表" + (sheetNum + 1), sheetNum);
        sheetNum++;
        //定义样式
        WritableCellFormat format = new WritableCellFormat();
        // true自动换行，false不自动换行
        try {
            format.setWrap(true);
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
        WritableCellFormat format1 = new WritableCellFormat();
        try {
            format1.setAlignment(jxl.format.Alignment.CENTRE);
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
        // Label labTitle_ = new Label(0, 0, "架号"+data[3]+" 层号"+data[4]);
        Label labBookIDText = new Label(0, 0, "条形码", format1),
                labBookIndexText = new Label(1, 0, "索书号", format1),
                labBookNameText = new Label(2, 0, "书名", format1),
                labColumnNoText = new Label(3, 0, "列号", format1),
                labRowNoText = new Label(4, 0, "排号", format1),
                labShelfNoText = new Label(5, 0, "架号", format1),
                labLayerNoText = new Label(6, 0, "层号", format1);
        Label[] labelsText = {labBookIDText, labBookIndexText, labBookNameText, labColumnNoText, labRowNoText, labShelfNoText, labLayerNoText};
        try {

            for (Label aLabelsText : labelsText) {
                sheetLoss.addCell(aLabelsText);
            }
        } catch (WriteException e) {
            LOGGER.error(getTrace(e));
        }
        //表格格式设置，固定列宽和自动换行
        for (int i = 0; i < EXCEL_LENGTH.length; i++) {
            sheetLoss.setColumnView(i, EXCEL_LENGTH[i]);
        }
//        for (BookInfo bookInfo : lossList) {
        for (Map.Entry<String, BookInfo> entry : lossList) {
            BookInfo bookInfo = entry.getValue();
            String bookID = bookInfo.bookId;
            String bookIndex = bookInfo.bookIndex;
            String bookName = bookInfo.bookName;
            if (!allLoanBookMap.containsKey(bookID) && !isForeignBook(bookIndex, bookName)) {
                //不在借出列表中，也不是外文书
                String bookPlace = getRightBookPlace(bookIndex);
                if (bookPlace == null) {
                    continue;
                }
                String[] bookInfos = locationDecode(bookPlace);
//                if (bookID.equals("2100052523")) {
//                    System.out.println("");
//                }
                countLoss++;
                if (countLoss % 60000 == 0) {
                    sheetLoss = book.createSheet("丢失列表" + (sheetNum + 1), sheetNum);
                    sheetNum++;
                    //表格格式设置，固定列宽和自动换行
                    for (int i = 0; i < EXCEL_LENGTH.length; i++) {
                        sheetLoss.setColumnView(i, EXCEL_LENGTH[i]);
                    }
                }
                int rowNo = sheetLoss.getRows();
                Label labBookID = new Label(0, rowNo, bookID, format),
                        labBookIndex = new Label(1, rowNo, bookIndex, format),
                        labBookName = new Label(2, rowNo, bookName, format),
                        labColumnNo = new Label(3, rowNo, bookInfos[2], format),
                        labRowNo = new Label(4, rowNo, bookInfos[3], format),
                        labShelfNo = new Label(5, rowNo, bookInfos[4], format),
                        labLayerNo = new Label(6, rowNo, bookInfos[5], format);
                Label[] labels = {labBookID, labBookIndex, labBookName, labColumnNo, labRowNo, labShelfNo, labLayerNo};
                try {
                    for (Label label : labels) {
                        sheetLoss.addCell(label);
                    }
                } catch (WriteException e) {
                    LOGGER.error(getTrace(e));
                }
            }
        }
        try {
            book.write();
            book.close();
        } catch (IOException | WriteException e) {
            LOGGER.error(getTrace(e));
        }
        LOGGER.info("丢失报表生成！");
        LOGGER.info("丢失图书共计" + countLoss + "册");
    }

    /**
     * zipSend 将生成的丢失列表和报表压缩并发送
     *
     * @author Wing
     * @date 2018/11/13 16:24
     */
    private void zipSend() {
        String zipFilePath = reportPath.replaceAll("xls", "zip");
        ArrayList<String> dataPath = new ArrayList<>();
        dataPath.add(reportPath);
        if (CHECK_LOSS) {
            dataPath.add(lossReportPath);
        }
        try {
            MyZip.zip(zipFilePath, dataPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Config.MAIL.trySendEmail("A" + FLOOR + ",DataSheet", "这是此次盘点生成的报表。" +
                "\r\n盘点楼层：A" + FLOOR +
                "\r\n扫描图书数量：" + countSuccess +
                "\r\nTAG_ID不在数据库中的数量：" + countNotInDB +
                "\r\n错架图书：" + countErr +
                "\r\n错馆图书：" + countErrLib +
                "\r\n借出图书：" + countLoan +
                "\r\n预约图书：" + countHold, zipFilePath);

        deleteFile(zipFilePath);
    }

    /**
     * 将位置信息解码
     *
     * @param code 位置信息的编码
     * @return 图书信息数组
     */
    public String[] locationDecode(String code) {
        String[] bookPlace = new String[6];
        bookPlace[0] = code.substring(2, 3);
        bookPlace[1] = code.substring(3, 4);
        bookPlace[2] = code.substring(5, 6);
        for (int i = 6; i < 12; i++) {
            bookPlace[i / 2] = Integer.parseInt(code.substring(i, i + 2)) + "";
            i++;
        }
        return bookPlace;
    }

    /**
     * 将位置信息进行编码
     *
     * @param bookInfos 图书信息数组
     * @return 编码字符串
     */
    private String locationEncode(String[] bookInfos) {
        StringBuilder code = new StringBuilder("WL");
        for (int i = BookFieldName.AREANO.getIndex(); i <= BookFieldName.LAYERNO.getIndex(); i++) {
            if (i == BookFieldName.FLOORNO.getIndex()) {
                code.append(bookInfos[i]).append("F");
            } else if (i == BookFieldName.AREANO.getIndex() || i == BookFieldName.COLUMNNO.getIndex()) {
                code.append(bookInfos[i]);
            } else {
                String tmp = bookInfos[i];
                if (tmp.length() == 1) {
                    tmp = "0" + tmp;
                }
                code.append(tmp);
            }
        }
        return code.toString();
    }


    /**
     * 判断EPC是否正确
     *
     * @param epc epc
     * @return EPC是否符合规则
     */
    private boolean isEpc(String epc) {
        /*之后可采用正则表达式判断*/
        return epc.length() >= 20;
    }

    /**
     * 连接数据库
     */
    private void getDBConnection() {
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

    private void closeDBConnection() {
        try {
            connect.close();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void createStatement() {
        try {
            statement.close();
            statement = connect.createStatement();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void closeStatement() {
        try {
            statement.close();
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        Config config = new Config();
        Res2Database res2Database = new Res2Database("C:\\Users\\Wing\\Desktop\\result\\A2.res");
        res2Database.getToBeUpdatedList();

    }
}
