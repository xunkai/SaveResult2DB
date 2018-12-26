package utils;

import java.net.InetAddress;

import static utils.MyLogger.LOGGER;

/**
 * Created by Wing on 2018/12/7
 * <p>
 * Project: SaveResult2DB
 */
public class NetUtil {


    private static String remoteInetAddr = "1ib.whu.edu.cn";//需要连接的IP地址

    /**
     * 传入需要连接的IP，返回是否连接成功
     * <p>
     * @param remoteInetAddr
     *
     * @return
     * @author Wing
     * @date 2018/12/7 18:44
     */
    public static boolean isReachable(String remoteInetAddr) {
        LOGGER.info("测试 "+remoteInetAddr+" 是否连通");
        boolean reachable = false;
        try {
            InetAddress address = InetAddress.getByName(remoteInetAddr);
            reachable = address.isReachable(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reachable;
    }

    public static void main(String[] args) {
        Boolean bon = false;
        bon = isReachable(remoteInetAddr);
        System.out.println("pingIP：" + bon);
    }
}
