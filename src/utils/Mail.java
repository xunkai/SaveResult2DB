package utils;

import java.util.*;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.*;

import static utils.MyLogger.LOGGER;
import static utils.MyLogger.getTrace;

public class Mail {
    public static boolean ENABLE_EMAIL = false;
    public static String EMAIL_FROM_ACCOUNT = "scan@lib.whu.edu.cn";
    public static String EMAIL_FROM_PASSWORD = "2018rfid";

    public static String EMAIL_FROM_ACCOUNT_STANDBY = "netlab624@163.com";
    public static String EMAIL_FROM_PASSWORD_STANDBY = "netlab624";
    public static String FROM_NAME = "TOOKER";
    public static String TO_NAME = "WHU";
    //QQSMTP服务器地址为:smtp.qq.com
    public static String EMAIL_SMTP_HOST = "lib.whu.edu.cn";
    public static String EMAIL_SMTP_HOST_STANDBY = "smtp.163.com";
    public static String EMAIL_SMTP_PORT = "25";
    //收件人邮箱
    public static String TO_ACCOUNT = "netlab624@163.com";
    public static Map<String, String> TO_ACCOUNT_MAP = new HashMap<>();
    public static String WHU_ACCOUNT1 = "liquan@lib.whu.edu.cn";
    public static String WHU_ACCOUNT2 = "hqh@lib.whu.edu.cn";
    public static Properties props;

    private static int TRY_SEND = 3;

    public Mail() {
        props = System.getProperties();


//        props.put("mail.smtp.ssl.enable", "true");
        //需要请求认证
        props.put("mail.smtp.auth", "true");
        props.put("mail.transport.protocol", "smtp");
        //6分钟超时，预防网络较差时传输较慢
        props.setProperty("mail.smtp.timeout", "360000");
        //发件人的邮箱的SMTP服务器地址
        props.put("mail.smtp.host", EMAIL_SMTP_HOST);
        props.put("mail.smtp.port", EMAIL_SMTP_PORT);
    }

    /**
     * 发送纯文本邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 是否发送成功
     */
    public boolean sendEmail(String subject, String content) {
        LOGGER.info("开始发送邮件");
        Session session = Session.getInstance(props);
        session.setDebug(false);//设置为debug模式，可以查看详细的发送Log

        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(EMAIL_FROM_ACCOUNT, FROM_NAME, "UTF-8"));
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(TO_ACCOUNT, TO_NAME, "UTF-8"));
//            }
            // Subject: 邮件主题（标题有广告嫌疑，避免被邮件服务器误认为是滥发广告以至返回失败，请修改标题）
            String subjectEncode = MimeUtility.encodeWord(subject, "UTF-8", "Q");
            message.setSubject(subjectEncode);
            message.setContent(content, "text/html;charset=utf-8");
            // 设置发件时间
            message.setSentDate(new Date());
            // 保存设置
            message.saveChanges();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.error("邮件发送失败");
            return false;
        }
        //根据Session获取邮件传输对象
        Transport transport = null;
        try {
            transport = session.getTransport();
            transport.connect(EMAIL_FROM_ACCOUNT, EMAIL_FROM_PASSWORD);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.error("邮件发送失败");
            return false;
        }
        LOGGER.info("邮件发送成功!");
        return true;
    }

    /**
     * 发送带有附件的邮件
     *
     * @param subject  邮件主题
     * @param content  邮件内容
     * @param fileName 附件路径
     * @return 是否成功发送
     */
    public boolean sendEmail(String subject, String content, String fileName) {
        LOGGER.info("开始发送邮件，邮件主题：" + subject);
        Session session = Session.getInstance(props);
        session.setDebug(false);
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(EMAIL_FROM_ACCOUNT, FROM_NAME, "UTF-8"));
            if (ENABLE_EMAIL) {
                List<InternetAddress> internetAddresseList = new ArrayList<>();
                for (Map.Entry<String, String> entry : TO_ACCOUNT_MAP.entrySet()) {
                    internetAddresseList.add(new InternetAddress(entry.getKey(), entry.getValue(), "UTF-8"));
                }
                internetAddresseList.add(new InternetAddress(TO_ACCOUNT, TO_NAME, "UTF-8"));
                InternetAddress[] internetAddresses = new InternetAddress[internetAddresseList.size()];
                message.setRecipients(MimeMessage.RecipientType.TO, internetAddresseList.toArray(internetAddresses));
            } else {
                message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(TO_ACCOUNT, TO_NAME, "UTF-8"));
            }
            String subjectEncode = MimeUtility.encodeWord(subject, "UTF-8", "Q");
            message.setSubject(subjectEncode);
            //创建附件"
            MimeBodyPart attachment = new MimeBodyPart();
            DataHandler dataHandler = new DataHandler(new FileDataSource(fileName));
            attachment.setDataHandler(dataHandler);
            attachment.setFileName(MimeUtility.encodeText(dataHandler.getName()));
            //文本
            MimeBodyPart text = new MimeBodyPart();
            text.setContent(content, "text/html;charset=UTF-8");
            //设置文本和 附件 的关系（合成一个大的混合"节点" / Multipart ）
            MimeMultipart allMultipart = new MimeMultipart();
            allMultipart.addBodyPart(attachment);
            allMultipart.addBodyPart(text);
            message.setContent(allMultipart, "text/html;charset=utf-8");
            message.setSentDate(new Date());
            message.saveChanges();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.error("邮件发送失败");
            return false;
        }
        //根据Session获取邮件传输对象
        Transport transport = null;
        try {
            transport = session.getTransport();
            transport.connect(EMAIL_FROM_ACCOUNT, EMAIL_FROM_PASSWORD);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.error("邮件发送失败");
            return false;
        }
        LOGGER.info("邮件发送成功");
        return true;
    }

    /**
     * 使用备用邮箱发送带有附件的邮件
     *
     * @param subject  邮件主题
     * @param content  邮件内容
     * @param fileName 附件路径
     * @return 是否成功发送
     */
    public boolean sendEmailStandby(String subject, String content, String fileName) {
        props.put("mail.smtp.host", EMAIL_SMTP_HOST_STANDBY);
        LOGGER.info("开始发送邮件（使用备用邮箱），邮件主题：" + subject);
        Session session = Session.getInstance(props);
        session.setDebug(false);
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(EMAIL_FROM_ACCOUNT_STANDBY, FROM_NAME, "UTF-8"));
            if (ENABLE_EMAIL) {
                List<InternetAddress> internetAddresseList = new ArrayList<>();
                for (Map.Entry<String, String> entry : TO_ACCOUNT_MAP.entrySet()) {
                    internetAddresseList.add(new InternetAddress(entry.getKey(), entry.getValue(), "UTF-8"));
                }
                internetAddresseList.add(new InternetAddress(TO_ACCOUNT, TO_NAME, "UTF-8"));
                InternetAddress[] internetAddresses = new InternetAddress[internetAddresseList.size()];
                message.setRecipients(MimeMessage.RecipientType.TO, internetAddresseList.toArray(internetAddresses));
            } else {
                message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(TO_ACCOUNT, TO_NAME, "UTF-8"));
            }
            String subjectEncode = MimeUtility.encodeWord(subject, "UTF-8", "Q");
            message.setSubject(subjectEncode);
            //创建附件"
            MimeBodyPart attachment = new MimeBodyPart();
            DataHandler dataHandler = new DataHandler(new FileDataSource(fileName));
            attachment.setDataHandler(dataHandler);
            attachment.setFileName(MimeUtility.encodeText(dataHandler.getName()));
            //文本
            MimeBodyPart text = new MimeBodyPart();
            text.setContent(content, "text/html;charset=UTF-8");
            //设置文本和 附件 的关系（合成一个大的混合"节点" / Multipart ）
            MimeMultipart allMultipart = new MimeMultipart();
            allMultipart.addBodyPart(attachment);
            allMultipart.addBodyPart(text);
            message.setContent(allMultipart, "text/html;charset=utf-8");
            message.setSentDate(new Date());
            message.saveChanges();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.error("邮件发送失败");
            return false;
        }
        //根据Session获取邮件传输对象
        Transport transport = null;
        try {
            transport = session.getTransport();
            transport.connect(EMAIL_FROM_ACCOUNT_STANDBY, EMAIL_FROM_PASSWORD_STANDBY);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (Exception e) {
//            e.printStackTrace();
            LOGGER.error(getTrace(e));
            LOGGER.error("邮件发送失败");
            return false;
        }
        LOGGER.info("邮件发送成功");
        return true;
    }

    public void trySendEmail(String subject, String content, String fileName) {
        int tryCount = 0;
        while (!sendEmail(subject, content, fileName)) {
            tryCount++;
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (tryCount >= TRY_SEND) {
                break;
            }
        }
        if (tryCount >= TRY_SEND) {
            tryCount = 0;
            //使用备用邮箱尝试三次
            while (!sendEmailStandby(subject, content, fileName)) {
                tryCount++;
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (tryCount >= TRY_SEND) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        Config config = new Config();
        Mail mail = new Mail();
        mail.sendEmailStandby("Test", "打扰了，这是一封测试邮件，请无视！", "config.xml");
    }
}


