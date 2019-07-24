package common;

import java.io.*;
import java.util.ArrayList;


/**
 * @program: database
 * @description: 从txt文本中读取BookInfo信息, 并在数据库中查询，若没有查询要即插入
 * @author: Mr.Qi
 * @create: 2019-07-18 16:07
 **/
public class ReadBookInfo {
    private String filePath = "C:\\Users\\zhouxiaolun\\Desktop\\laboratory\\robot\\database\\data\\20190716\\Nanjing_RFID.txt";

    public void readBookInfo() throws IOException {
        FileInputStream fstream = null;
        BufferedReader br = null;
        try {
            fstream = new FileInputStream(filePath);
            br = new BufferedReader(new InputStreamReader(fstream, "utf-8"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        SQLiteUtil.createOrConnect();
        String strLine = "";

        ArrayList<BookInfo> bookInfoList = new ArrayList<>();

        Long beginTime = System.currentTimeMillis();

        while ((strLine = br.readLine()) != null) {
            String[] segments = strLine.split("\t");
            BookInfo bookInfo = new BookInfo();
            bookInfo.bookName = segments[0];
            bookInfo.barcode = segments[1];
            bookInfo.bookIndex = segments[4];
//            System.out.println(bookInfo.bookName +" + "+ bookInfo.barcode +" + "+ bookInfo.bookIndex);
//            System.out.println(bookInfo.barcode);
            bookInfoList.add(bookInfo);
        }
        System.out.println("bookInfoList共有数据：" + bookInfoList.size() + ".");
//        System.out.println(bookInfoList.get(11).bookName+"+"+bookInfoList.get(10).bookName);
//        SQLiteUtil.batchInsertBookInfo(bookInfoList);

        System.out.println("count:" + SQLiteUtil.queryCount());
        long endTime = System.currentTimeMillis();
        System.out.println("Insert " + bookInfoList.size() + " records takes :" + (endTime - beginTime) / 1000 + " seconds!");
        br.close();
        fstream.close();

    }

    public static void main(String[] args) throws IOException {
        ReadBookInfo r = new ReadBookInfo();
        r.readBookInfo();

    }
}
