package icu.whereis.ap;

import com.formdev.flatlaf.FlatLightLaf;
import icu.whereis.ap.file.BigFileReader;
import icu.whereis.ap.file.FileHandleImpl;
import icu.whereis.ap.ui.MainFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static Logger logger = LogManager.getLogger(Main.class);

    public static void createGui() {
        MainFrame mainFrame = new MainFrame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FlatLightLaf.setup();
                createGui();
            }
        });
    }

    private static void runCli() {
        //File file = new File("C:\\Users\\Administrator\\Desktop\\QuickStart.7z");
        File file = new File("C:\\Users\\Administrator\\Desktop\\1.zip");
        File dictFile = new File("C:\\Users\\Administrator\\Desktop\\dict.txt");
        if (!file.exists()) {
            logger.info(file.getAbsolutePath()+"不存在");
            return;
        }
        if (!dictFile.exists()) {
            logger.info(dictFile.getAbsolutePath()+"不存在");
            return;
        }

        FileHandleImpl fileHandle = null;
        try {
            fileHandle = new FileHandleImpl(file, new AtomicBoolean(false), StringKit.getFileLineCount(dictFile));
            BigFileReader.Builder builder = new BigFileReader.Builder(dictFile.getAbsolutePath(), fileHandle);
            builder.withTreahdSize(100)
                    .withCharset("utf-8")
                    .withBufferSize(1024*1024);
            BigFileReader bigFileReader = builder.build();
            bigFileReader.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
