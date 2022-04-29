package icu.whereis.ap.ui;

import icu.whereis.ap.StringKit;
import icu.whereis.ap.file.BigFileReader;
import icu.whereis.ap.file.CompleteCallback;
import icu.whereis.ap.file.SwingFileHandleImpl;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 界面主体
 */
public class MainFrame extends JFrame {
    private static Logger logger = LogManager.getLogger(MainFrame.class);

    SwingFileHandleImpl fileHandle = null;

    private JPanel cp = (JPanel)getContentPane();
    private JProgressBar pb = new JProgressBar();
    private JTextField dictTextField = new JTextField();
    private JTextField cpsTextField = new JTextField();
    private JSpinner threadTextField = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private JTextArea msgArea = new JTextArea();
    private JToggleButton okBtn = new JToggleButton("开始试密");

    public MainFrame() throws HeadlessException {
        initUI();
    }

    public void initUI() {
        setResizable(false);
        JFrame.setDefaultLookAndFeelDecorated(true);
        setTitle("ArchivePasswordTestTool");
        // 使用这种方式获取资源，IDE和打包成Jar后都能获取
        ImageIcon imageIcon = new ImageIcon(getClass().getResource("/icon/unlock.png"));
        setIconImage(imageIcon.getImage());

        MigLayout migLayout = new MigLayout("", "[right][grow][]", "[]3[]10[40]20[30][]");
        cp.setLayout(migLayout);

        // 字典
        JLabel dictLabel = new JLabel("字典：");
        cp.add(dictLabel);

        // 此处 growx 需要布局管理器初始化时添加 grow 才有效果
        cp.add(dictTextField, "growx");

        JButton dictBtn = new JButton("选择字典");
        dictBtn.setBackground(Color.decode("#b1ece4"));
        dictBtn.addActionListener(new OpenFileAction(dictTextField, "文本文件", "txt"));
        // 添加 wrap 换行，不然所有组件会在同一行排列
        cp.add(dictBtn, "wrap");

        JLabel cpsLabel = new JLabel("压缩包：");
        cp.add(cpsLabel);

        // 此处 growx 需要布局管理器初始化时添加 grow 才有效果
        cp.add(cpsTextField, "growx");

        JButton cpsBtn = new JButton("选择文件");
        cpsBtn.setBackground(Color.decode("#b1ece4"));
        cpsBtn.addActionListener(new OpenFileAction(cpsTextField, "压缩文件", "7z,zip"));
        cp.add(cpsBtn, "wrap");

        JLabel threadLabel = new JLabel("线程数：");
        cp.add(threadLabel);
        // 先将本行的单元格2切割成2份单元格，再跨越2格，就能把后面的单元格包含进来
        cp.add(threadTextField, "split 2,span 2");
        okBtn.addActionListener(new StartActionListener(this));

        okBtn.setFont(new Font("隶书",Font.PLAIN,20));
        okBtn.setBackground(Color.decode("#26e1c9"));
        okBtn.setForeground(Color.WHITE);
        cp.add(okBtn, "grow,wrap");

        JLabel progressLabel = new JLabel("进度：");
        cp.add(progressLabel);
        pb.setForeground(Color.decode("#32ec82"));
        pb.setBackground(Color.decode("#e6e2f0"));
        // 是否在进度条上显示字符串
        pb.setStringPainted(true);
        // setStringPainted(true)才有效，不设置则显示 xx%
        // pb.setString("46");
        pb.setValue(0);
        cp.add(pb, "span 2,grow,wrap");

        // 内容自动换行
        msgArea.setLineWrap(true);
        // 换行时不截断单词
        //msgArea.setWrapStyleWord(true);
        msgArea.setRows(15);
        msgArea.setEditable(false);
        msgArea.append("消息0：源码 > https://github.com/RiverChu0/ArchivePasswordTestTool\n");
        // 给文本域加滚动条
        JScrollPane scrollPane = new JScrollPane(msgArea);
        cp.add(scrollPane, "span 3,grow");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(490, 308);

        // 居中
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public int getThreads() {
        return Integer.parseInt(this.threadTextField.getValue().toString());
    }
    public void appendMsg(String msg) {
        msgArea.append(StringKit.getMsg(msg));
        // 自动滚到最后面
        msgArea.setCaretPosition(msgArea.getText().length());
    }
    public void setProgressMaxValue(int maxValue) {
        this.pb.setMaximum(maxValue);
    }
    public void setProgressBarValue(String string, int value) {
        this.pb.setString(string);
        this.pb.setValue(value);
    }

    /**
     * 打开文件的操作
     */
    private class OpenFileAction extends AbstractAction {

        private JTextField textField;
        private String extensionDesc;
        private String extension;

        public OpenFileAction(JTextField textField, String extensionDesc, String extension) {
            this.textField = textField;
            this.extensionDesc = extensionDesc;
            this.extension = extension;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(extensionDesc, extension.split(","));
            fileChooser.addChoosableFileFilter(filter);
            // 设置后打开会自动应用指定的文件过滤器而不是“所有文件”
            fileChooser.setFileFilter(filter);
            // 设置为false后，“所有文件”选项消失
            fileChooser.setAcceptAllFileFilterUsed(false);
            // 禁用多选
            fileChooser.setMultiSelectionEnabled(false);
            // 仅可选择文件，而不是文件夹
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int ret = fileChooser.showDialog(cp, "选择文件");

            if (ret == JFileChooser.APPROVE_OPTION) {

                File file = fileChooser.getSelectedFile();
                String text = file.getAbsolutePath();
                appendMsg("选择："+text);

                textField.setText(text);
            }
        }

    }

    public void setEnabledToggleButton(boolean enabled) {
        this.okBtn.setEnabled(enabled);
    }

    public void resetToggleButton(JToggleButton button) {
        if (button == null) {
            button = this.okBtn;
        }
        button.setEnabled(true);
        button.setSelected(false);
        button.setText("开始试密");
        button.setForeground(Color.WHITE);
    }

    public void showMessageDialog(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    private class StartActionListener implements ActionListener {

        private BigFileReader bigFileReader;
        private MainFrame mainFrame;

        public StartActionListener(MainFrame mainFrame) {
            this.mainFrame = mainFrame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JToggleButton source = (JToggleButton) e.getSource();
            boolean selected = source.isSelected();
            if (selected) {
                source.setForeground(Color.GREEN);
                source.setText("停止");
                source.setEnabled(false);

                String dictPath = dictTextField.getText();
                String cpsPath = cpsTextField.getText();
                if (StringUtils.isBlank(dictPath)) {
                    JOptionPane.showMessageDialog(cp, "请选择字典文件！");
                    resetToggleButton(source);
                    return;
                }
                if (StringUtils.isBlank(cpsPath)) {
                    JOptionPane.showMessageDialog(cp, "请选择压缩包文件！");
                    resetToggleButton(source);
                    return;
                }

                File file = new File(cpsPath);
                File dictFile = new File(dictPath);
                if (!file.exists()) {
                    JOptionPane.showMessageDialog(cp, file.getAbsolutePath()+"不存在");
                    resetToggleButton(source);
                    return;
                }
                if (!dictFile.exists()) {
                    JOptionPane.showMessageDialog(cp, dictFile.getAbsolutePath()+"不存在");
                    resetToggleButton(source);
                    return;
                }

                try {
                    fileHandle = new SwingFileHandleImpl(mainFrame, file, new AtomicBoolean(false), StringKit.getFileLineCount(dictFile));
                    BigFileReader.Builder builder = new BigFileReader.Builder(dictFile.getAbsolutePath(), fileHandle);
                    builder.withTreahdSize(getThreads())
                            .withCharset("utf-8")
                            .withBufferSize(1024*1024);
                    BigFileReader bigFileReader = builder.build();

                    this.bigFileReader = bigFileReader;

                    this.bigFileReader.setCompleteCallback(new CompleteCallback() {
                        @Override
                        public void onComplete() {
                            if (fileHandle!=null && fileHandle.getSuccess().get() == false) {
                                appendMsg("很遗憾！未能找到密码！");
                                showMessageDialog("很遗憾！未能找到密码！");
                                fileHandle.stop();
                                fileHandle = null;
                            }
                            resetToggleButton(null);
                        }

                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFinish() {
                            appendMsg("线程池已关闭");
                        }
                    });

                    appendMsg("Power Booster On...");
                    bigFileReader.start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            } else {
                if (bigFileReader != null) {
                    fileHandle.stop();
                    fileHandle = null;

                    bigFileReader.shutdown();
                    bigFileReader = null;
                    setEnabledToggleButton(false);
                    appendMsg("用户停止了任务，等待线程池关闭...");
                    return;
                }
            }
        }

    }

    /**
     * 此事件监听了多个状态变化，导致触发多次。不适用
     */
    private class StartChangeListener implements ChangeListener {

        private BigFileReader bigFileReader;
        private MainFrame mainFrame;

        public StartChangeListener(MainFrame mainFrame) {
            this.mainFrame = mainFrame;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            JToggleButton source = (JToggleButton) e.getSource();
            boolean selected = source.isSelected();
            if (selected) {
                source.setForeground(Color.GREEN);
                source.setText("停止");
                logger.info("进来了");

            } else {
                source.setForeground(Color.WHITE);
                source.setText("开始试密");

                if (bigFileReader != null) {
                    bigFileReader.shutdown(true);
                    bigFileReader = null;
                    appendMsg("用户停止了试密...");
                    return;
                }
            }
        }
    }
}
