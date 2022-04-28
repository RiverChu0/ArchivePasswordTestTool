package icu.whereis.ap.file;

public interface FileHandle {
    void handle(String line, long currentLineCount, BigFileReader bigFileReader);
}
