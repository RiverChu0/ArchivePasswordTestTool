package icu.whereis.ap.file;

public interface FileHandle {
    void init();
    void handle(String line, long currentLineCount, BigFileReader bigFileReader);
}
