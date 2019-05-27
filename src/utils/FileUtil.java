package utils;

import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static utils.MyLogger.LOGGER;

/**
 * 文件操作工具类
 *
 * @author Wing
 * @version 1.0, 18/07/19
 */
public final class FileUtil {

    /**
     * 清空文件
     *
     * @param fileName 文件路径
     */
    public static void clearFile(String fileName) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false));

            writer.write("");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 按行读取文件
     *
     * @param fileName 文件路径
     * @return 行字符串列表
     */
    public static List<String> readFileByLine(String fileName) {
        ArrayList<String> result = new ArrayList<>();
        if (!new File(fileName).exists()) {
            result.add("");
            return result;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String text;

            while ((text = reader.readLine()) != null) {
                result.add(text);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 将字符串列表写入文件
     *
     * @param fileName    文件路径
     * @param isAddToTail 是否续写文件
     * @param contents    写入内容列表
     */
    public static void writeFile(String fileName, boolean isAddToTail, List<?> contents) {
        try {
            File file = new File(fileName);

            if (!file.exists()) {
                file.createNewFile();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, isAddToTail));

            for (Object o : contents) {
                writer.write(o.toString());
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String fileName, boolean isAddToTail, Map<?, ?> contents) {
        try {
            File file = new File(fileName);

            if (!file.exists()) {
                file.createNewFile();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, isAddToTail));

            for (Map.Entry<?, ?> entry : contents.entrySet()) {
                writer.write(entry.getKey().toString() + " " + entry.getValue().toString());
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将字符串列表写入文件
     *
     * @param fileName    文件路径
     * @param isAddToTail 是否续写文件
     * @param content     写入内容
     */
    public static void writeFile(String fileName, boolean isAddToTail, String content) {
        try {
            File file = new File(fileName);

            if (!file.exists()) {
                file.createNewFile();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, isAddToTail));

            writer.write(content);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * deleteFile 删除文件
     *
     * @param fileName 要删除的文件路径
     * @return void
     * @author Wing
     * @date 2018/10/18 17:07
     */
    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    deleteFile(f.getPath());
                }
            }
            file.delete();
            LOGGER.info("delete file:" + file.getPath());
        } else {
            LOGGER.info("not exist such file:" + file.getPath());
        }
    }

    /**
     * deleteEmptyDirectory 删除文件夹下所有空目录（包括自己）
     * <p>
     *
     * @param dirName 文件夹
     * @author Wing
     * @date 2018/12/19 19:13
     */
    public static void deleteEmptyDirectory(String dirName) {
        int count = 0;
        File dir = new File(dirName);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                deleteEmptyDirectory(file.getPath());
                if (file.isDirectory() && file.list().length == 0) {
                    file.delete();
                    count++;
                    LOGGER.info("delete empty dir:" + file.getPath());
                }
            }
        }
//        LOGGER.info("已删除" + count + "个空目录！");
    }

    /**
     * deleteExpiredFile 删除文件夹下所有过期文件
     *
     * @param dirName 要删除的文件夹
     * @param day     过期时间设置，单位：天
     * @return void
     * @author Wing
     * @date 2018/12/4 14:15
     */
    public static void deleteExpiredFile(String dirName, int day) {
        int count = 0;
        long time = (long) day * 24 * 60 * 60 * 1000;
        File dir = new File(dirName);
        if (!dir.exists()) {
            LOGGER.info(dirName + "文件夹不存在");
            return;
        }
        List<File> files = getAllFiles(dir);
        for (File file : files) {
            long tmp = System.currentTimeMillis() - file.lastModified();
            if (tmp > time) {
                //Expired
                file.delete();
                count++;
                LOGGER.info("delete " + file.getAbsolutePath());
            }
        }
        deleteEmptyDirectory(dirName);
        LOGGER.info("已删除" + count + "个过期文件！");

    }

    /**
     * getAllFiles 递归获取文件夹下所有文件
     *
     * @param dir 文件夹
     * @return java.util.List<java.io.File>
     * @author Wing
     * @date 2018/12/4 14:53
     */
    public static List<File> getAllFiles(File dir) {
        ArrayList<File> files = new ArrayList<>();
        if (dir.isFile()) {
            files.add(dir);
            return files;
        }
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                files.addAll(getAllFiles(file));
            }
        }
        return files;
    }

    /**
     * getAllFiles 递归获取文件夹下{day}天内的所有文件
     *
     * @param dir
     * @param day
     * @return java.util.List<java.io.File>
     */
    public static List<File> getAllFiles(File dir, int day) {
        List<File> files = getAllFiles(dir);
        List<File> filesCopy = new ArrayList<>();
        filesCopy.addAll(files);
        long time = (long) day * 24 * 60 * 60 * 1000;
        long curTime = System.currentTimeMillis();
        for (File file : filesCopy) {
            long tmp = curTime - file.lastModified();
            if (tmp > time) {
                files.remove(file);
            }
        }
        return files;
    }

    /**
     * 创建新文件
     *
     * @param fileName
     */
    public static void createFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * main
     *
     * @param args
     * @return void
     * @author Wing
     * @date 2018/12/4 18:47
     */
    public static void main(String[] args) {
        deleteExpiredFile("E:\\whu\\", 45);
//        deleteEmptyDirectory("E:\\whu\\");
//        deleteFile("F:\\whu\\新建文件夹");
//        System.out.println(Config.REMAIN_SHELFS_STR);
//        List<File> files = getAllFiles(new File("D:\\Wing\\IdeaProjects\\SaveResult2DB\\log"),5);
//        System.out.println(files.size());
//        FileUtil.createFile(Config.REPORT_END_PATH);
    }
}

