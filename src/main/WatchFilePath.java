package main;

import java.io.File;
import java.io.IOException;

import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

import common.CuhkszSchool;
import utils.Config;

import static utils.FileUtil.deleteExpiredFile;
import static utils.FileUtil.deleteFile;
import static utils.MyLogger.LOGGER;

/**
 * 文件夹监听服务
 *
 * @author Wing
 * @version 1.0, 18/05/10
 */
public class WatchFilePath {

    /**
     * 传输结束标志
     */
    private static final String END_FILE = "end";

    /**
     * 监听路径
     */
    private static String PATH;

    /**
     * 是否为结果文件
     */
    private boolean isRes = false;

    /**
     * 监听服务
     */
    private WatchService watcher;

    /**
     * Constructs
     *
     * @param path 监听路径
     * @throws IOException IO异常
     */
    public WatchFilePath(Path path) throws IOException {
        PATH = path.toString();
        watcher = FileSystems.getDefault().newWatchService();
        path.register(watcher, ENTRY_DELETE, ENTRY_CREATE, ENTRY_MODIFY);
    }

    /**
     * Method description
     *
     * @throws InterruptedException
     */
    public void handleEvents() throws InterruptedException {
        String resFile = "",floor = "";

        LOGGER.info("正在监听文件夹：" + PATH);

        // 删除结束标志文件
        deleteFile(PATH+File.separator+END_FILE);
        while (true) {
            WatchKey key = watcher.take();
            String fileName = "";
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // 事件可能lost or discarded
                if (kind == OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> e = (WatchEvent<Path>) event;
                fileName = e.context().toString();
                String entryCreate = "ENTRY_CREATE";

                if (entryCreate.equals(kind.name())) {
                    if (fileName.matches(".*\\.res$")) {
                        // 监听到结果文件创建
                        resFile = PATH + File.separator + fileName;
                        floor = fileName.substring(0,2);
                        isRes = true;
                        LOGGER.info("检测到res文件:" + fileName + "，等待传输完成...");
                    }


                }

            }

            if (!key.reset()) {
                break;
            }
            File[] files = new File(PATH).listFiles();
            for(File file:files){
                if(file.getName().equals(END_FILE)){
                    // 监听到结束标志文件创建
                    LOGGER.info("检测到end文件，等待解析...");
                    Thread.sleep(100);

                    // 删除结束标志文件
                    deleteFile(PATH+File.separator+END_FILE);
                    Config.MAIL.sendEmail(CuhkszSchool.SCHOOL_CODE + "," + floor + ",Report generation starts", "已经监听到res文件:" + resFile + "，即将生成报表，请稍候！");
                    Res2Database res2Database = new Res2Database(resFile);
                    resFile = "";
                    isRes = false;
                    LOGGER.info("正在监听...");
                }
            }

        }
    }

    /**
     * Method description
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        String Version = "1.15";
        String Time = "2019/5/29 13:30";
        String Update = "修复了丢失报表为0的bug";
        LOGGER.info("Version: " + Version);
        LOGGER.info("Update at " + Time);
        LOGGER.info("Updating info " + Update);
        Config config = new Config();
        //clean expired files
        LOGGER.info("清理过期文件");
        deleteExpiredFile(Config.DESKTOP_PATH + "\\Tooker\\result", 60);
        deleteExpiredFile(Config.DESKTOP_PATH + "\\Tooker\\data", 60);
        deleteExpiredFile(Config.DESKTOP_PATH + "\\Tooker\\log_I&Q", 30);
        deleteExpiredFile(Config.DESKTOP_PATH + "\\Tooker\\Report\\log\\", 30);

        LOGGER.info("清理上次传输文件夹");
        deleteFile(Config.DESKTOP_PATH+"\\Tooker\\Config_I&Q\\failed_files");
        deleteFile(Config.DESKTOP_PATH+"\\Tooker\\Config_I&Q\\failed_res");
        deleteFile(Config.DESKTOP_PATH+"\\Tooker\\Config_I&Q\\lastdate.f");

        deleteFile(Config.REPORT_END_PATH);

        //上线模式
//        String filepath = Config.DESKTOP_PATH + "\\Tooker\\result";
//        File file = new File(filepath);
//        if (!file.exists()) {
//            file.mkdirs();
//        }
//        new WatchFilePath(Paths.get(filepath)).handleEvents();

        //test
        Res2Database res2Database = new Res2Database(
//                修改这个
//                Config.DESKTOP_PATH+"\\A4_2018-12-05_22-20-56_full.res");
                "C:\\Users\\77236\\Desktop\\Tooker\\result\\CUHKSZ\\1\\A4_2019-07-07_17-35-49_full.res");
    }
}

