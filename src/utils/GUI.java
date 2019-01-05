package utils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Wing on 2019/1/4
 * <p>
 * Project: SaveResult2DB
 * 程序的显示功能，例如弹出消息框等
 */
public class GUI {
    private static Thread thread;
    private static JDialog dialog;

    /**
     * 显示对话框，不可自主关闭
     *
     * @param content 消息框内容
     */
    public static void showMessageDialog(String content) {
        //使用新线程防止主线程被阻塞
        if (thread != null && thread.isAlive()) {
            thread.stop();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, content, "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        thread.start();
    }

    /**
     * 显示一个自定义的对话框
     *
     * @param owner           对话框的拥有者
     * @param parentComponent 对话框的父级组件
     */
    public static void showCustomDialog(Frame owner, Component parentComponent, String content) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.dispose();
                }
                // 创建一个模态对话框
                dialog = new JDialog(owner, "提示", true);
                // 设置对话框的宽高
                dialog.setSize(300, 200);
                // 设置对话框大小不可改变
                dialog.setResizable(false);
                // 设置对话框相对显示的位置
                dialog.setLocationRelativeTo(parentComponent);
                // 创建一个标签显示消息内容
                JLabel messageLabel = new JLabel(content);
                // 创建一个按钮用于关闭对话框
                JButton okBtn = new JButton("确定");
                okBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 关闭对话框
                        dialog.dispose();
                    }
                });
                // 创建对话框的内容面板, 在面板内可以根据自己的需要添加任何组件并做任意是布局
                JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
                JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
                Box vBox = Box.createVerticalBox();
                // 添加组件到面板

                panel1.add(messageLabel);
                panel2.add(okBtn);
                //留白一点
                Border border = panel1.getBorder();
                Border margin = new EmptyBorder(20, 0, 0, 0);
                panel1.setBorder(new CompoundBorder(border, margin));
                vBox.add(panel1);
                vBox.add(panel2);
                // 设置对话框的内容面板
                dialog.setContentPane(vBox);
                // 显示对话框
                dialog.setVisible(true);
            }
        }).start();

    }

    /**
     * 关闭正在显示的对话框
     */
    public static void closeCustomDialog() {
        if (dialog.isActive()) {
            dialog.dispose();
        }
    }

    public static void main(String[] args) {
//        GUI.showMessageDialog("我是内容1");
        GUI.showCustomDialog(null, null, "我是内容1");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        GUI.showCustomDialog(null, null, "我是内容2");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        GUI.showCustomDialog(null, null, "我是内容3");
    }
}
