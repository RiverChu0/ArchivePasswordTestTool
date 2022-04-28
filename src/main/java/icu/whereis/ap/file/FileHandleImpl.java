package icu.whereis.ap.file;

import icu.whereis.ap.StringKit;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileHandleImpl implements FileHandle {

    private static Logger logger = LogManager.getLogger(FileHandleImpl.class);

    private File file;
    private String fileType;
    private AtomicBoolean success;
    private int lineTotalCount;

    public FileHandleImpl(File file, AtomicBoolean success, int lineTotalCount) {
        this.file = file;
        this.success = success;
        try {
            this.fileType = new Tika().detect(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.lineTotalCount = lineTotalCount;
    }

    @Override
    public void handle(String line, long currentLineCount, BigFileReader bigFileReader) {
        if (success.compareAndSet(true, true)) {
            //logger.info("密码已找到，不再执行");
            return;
        }

        String percent = StringKit.division(Math.toIntExact(currentLineCount), lineTotalCount);
        if ("application/x-7z-compressed".equals(fileType)) {
            SevenZFile sevenZFile = null;
            try {
                sevenZFile = new SevenZFile(file, line.toCharArray());
                logger.info("找到密码[" + line + "]！");
                success.set(true);

                bigFileReader.shutdown();
            } catch (IOException e) {
                logger.info("尝试密码["+line+"]，错！已尝试 "+currentLineCount+"("+percent+"%) 个");
            }
        } else if ("application/zip".equals(fileType)) {
            ZipFile zipFile = new ZipFile(file, line.toCharArray());

            try {
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
                    } else {
                        logger.info("压缩包没文件，试个J8！");
                    }
                } else {
                    logger.info("无密码！");
                }

                if (success.compareAndSet(false, true)) {
                    logger.info("找到密码[" + line + "]！");
                    bigFileReader.shutdown();
                }
            } catch (IOException e) {
                // 为了在其中一个线程找到密码后不打印多余的日志，加上此条件。不过，并非100%。只是大大降低了几率
                if (!success.compareAndSet(true, true)) {
                    String msg = e.getMessage();
                    logger.info("尝试密码[" + line + "]，错！已尝试 " + currentLineCount + "(" + percent + "%) 个");
                }
            }
        } else {
            logger.warn("不支持的文件类型："+fileType);
            bigFileReader.shutdown();
        }
    }

    private File createTempFile(String userDir, String filename) {
        File file = new File(userDir, filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
}
