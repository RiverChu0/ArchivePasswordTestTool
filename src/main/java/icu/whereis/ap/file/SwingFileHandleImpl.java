package icu.whereis.ap.file;

import icu.whereis.ap.StringKit;
import icu.whereis.ap.ui.MainFrame;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Swing界面
 */
public class SwingFileHandleImpl implements FileHandle {

    private static Logger logger = LogManager.getLogger(SwingFileHandleImpl.class);

    private File file;
    private String fileType;
    private AtomicBoolean success;
    private int lineTotalCount;
    private boolean stop = false;
    private String sevenzNullPassword = "";
    private AtomicBoolean sevenzNullPasswordUse = new AtomicBoolean(false);

    private MainFrame mainFrame;

    public SwingFileHandleImpl(MainFrame mainFrame, File file, AtomicBoolean success, int lineTotalCount) {
        this.mainFrame = mainFrame;
        this.file = file;
        this.success = success;

        mainFrame.setProgressMaxValue(lineTotalCount);
        try {
            this.fileType = new Tika().detect(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.lineTotalCount = lineTotalCount;
    }

    public void stop() {
        this.stop = true;
    }

    public AtomicBoolean getSuccess() {
        return this.success;
    }

    @Override
    public void handle(String line, long currentLineCount, BigFileReader bigFileReader) {
        if (success.compareAndSet(true, true) || stop) {
            //logger.info("密码已找到，不再执行");
            return;
        }
        this.mainFrame.setProgressBarValue(String.valueOf(currentLineCount), Math.toIntExact(currentLineCount));

        String percent = StringKit.division(Math.toIntExact(currentLineCount), lineTotalCount);
        if ("application/x-7z-compressed".equals(fileType)) {
            SevenZFile sevenZFile = null;
            try {
                if (sevenzNullPasswordUse.compareAndSet(false, true)) {
                    sevenZFile = new SevenZFile(file, sevenzNullPassword.toCharArray());
                    if (success.compareAndSet(false, true)) {
                        mainFrame.appendMsg("压缩包无密码！");
                        logger.info("压缩包无密码！");
                        mainFrame.resetToggleButton(null);
                        bigFileReader.shutdown();
                        mainFrame.showMessageDialog("压缩包无密码！");
                        return;
                    }
                } else {
                    sevenZFile = new SevenZFile(file, line.toCharArray());
                    if (success.compareAndSet(false, true)) {
                        mainFrame.appendMsg("找到密码[" + line + "]！");
                        logger.info("找到密码[" + line + "]！");
                        mainFrame.resetToggleButton(null);
                        bigFileReader.shutdown();
                        mainFrame.showMessageDialog("找到密码[" + line + "]！");
                        return;
                    }
                }
            } catch (IOException e) {
                if (!success.compareAndSet(true, true) && !stop) {
                    mainFrame.appendMsg("尝试密码[" + line + "]，错！");
                }
            }
        } else if ("application/zip".equals(fileType)) {
            ZipFile zipFile = new ZipFile(file, line.toCharArray());

            try {
                StringBuilder msg = new StringBuilder();
                if (zipFile.isEncrypted()) {
                    List<FileHeader> fileHeaders = zipFile.getFileHeaders();
                    if (fileHeaders.size() > 0) {
                        ZipInputStream inputStream = zipFile.getInputStream(fileHeaders.get(0));
                        byte[] bytes = new byte[1024];
                        // 必须要读几下，才能确保是真密码，有些假密码也能获取到ZipInputStream对象，还能读出几个字节，真是奇怪？
                        for (int i = 0; i < 6; i++) {
                            inputStream.read(bytes);
                        }

                        inputStream.close();

                        if (success.compareAndSet(false, true)) {
                            mainFrame.appendMsg("找到密码[" + line + "]！");
                            logger.info("找到密码[" + line + "]！");
                            mainFrame.resetToggleButton(null);
                            mainFrame.showMessageDialog("找到密码[" + line + "]！");
                            bigFileReader.shutdown();
                            return;
                        }
                    } else {
                        msg.append("压缩包没文件，试个J8！");
                    }
                } else {
                    msg.append("压缩包无密码！");
                }

                if (success.compareAndSet(false, true)) {
                    mainFrame.resetToggleButton(null);
                    bigFileReader.setCompleteCallback(new CompleteCallback() {

                        @Override
                        public void onComplete() {

                        }

                        @Override
                        public void onSuccess() {
                            mainFrame.appendMsg(msg.toString());
                        }
                    });
                    bigFileReader.shutdown();
                    return;
                }
            } catch (IOException e) {
                // 为了在其中一个线程找到密码后不打印多余的日志，加上此条件。不过，并非100%。只是大大降低了几率
                if (!success.compareAndSet(true, true) && !stop) {
                    mainFrame.appendMsg("尝试密码[" + line + "]，错！");
                }
                logger.error(e);
            }
        } else {
            mainFrame.appendMsg("不支持的文件类型："+fileType);
            bigFileReader.shutdown();
        }
    }

}
