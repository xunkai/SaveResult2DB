package utils;

import java.io.*;
import java.util.List;
import java.util.zip.*;

import static utils.MyLogger.LOGGER;

/**
 * 压缩工具类
 *
 * @author Wing
 * @date 2018/11/13 17:11
*/
public class MyZip {
    /**
     * zip 压缩文件
     *
     * @param zipFileName 压缩得到的文件
     * @param inputFileName	要压缩的文件
     * @return void
     * @author Wing
     * @date 2018/10/18 17:09
    */
    public static void zip(String zipFileName, String inputFileName) throws Exception {
        File inputFile = new File(inputFileName);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        LOGGER.info(inputFileName+" 压缩中...");
        zip(out, inputFile, inputFile.getName());
        out.close();
    }


    public static void zip(String zipFileName, List<String> inputFileNames) throws Exception {

        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        for(String inputFileName:inputFileNames){
            File inputFile = new File(inputFileName);
            LOGGER.info(inputFile+" 压缩中...");
            zip(out, inputFile, inputFile.getName());
        }
        out.close();
    }

    /**
     * zip 压缩
     *
     * @param out
     * @param f
     * @param base
     * @return void
     * @author Wing
     * @date 2018/12/4 18:34
    */
    private static void zip(ZipOutputStream out, File f, String base) throws Exception {
        if (f.isDirectory()) {
            // 目录
            File[] fl = f.listFiles();
            out.putNextEntry(new ZipEntry(base + "/"));
            base = base.length() == 0 ? "" : base + "/";
            for (int i = 0; i < fl.length; i++) {
                zip(out, fl[i], base + fl[i]);
            }
        } else {
            //文件
//            LOGGER.info(base);
            out.putNextEntry(new ZipEntry(base));
            FileInputStream in = new FileInputStream(f);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            in.close();
        }
    }

    public static void main(String[] temp) { // 主方法
        try {
            // 调用方法，参数为压缩后文件与要压缩文件
            MyZip.zip("F:\\hello.zip", "C:\\Users\\NetLab624\\Desktop\\err.log");
            System.out.println("压缩完成");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
