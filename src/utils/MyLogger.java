package utils;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 日志类
 *
 * @author Wing
 * date: 2018.07.19
 */
public class MyLogger {
    public static final Logger LOGGER = Logger.getLogger(MyLogger.class.getName());

    /**
     * 获取异常的堆栈信息
     *
     * @param t 异常
     * @return 异常信息
     * @author Wing
     * @date 2018/12/13 13:56
     */
    public static String getTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        StringBuffer buffer = stringWriter.getBuffer();
        String[] str = buffer.toString().split("\\r\\n\\t");
        buffer = new StringBuffer();

        buffer.append("Exception");
        for (String s : str) {
            if (buffer.length() > 100) {
                break;
            }
            buffer.append("\r\n\t").append(s);
        }
        writer.close();
        return buffer.toString();
    }
}

