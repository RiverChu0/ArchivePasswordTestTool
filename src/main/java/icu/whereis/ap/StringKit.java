package icu.whereis.ap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DecimalFormat;

public class StringKit {
    public static String division(int num1, int num2) {
        String rate = "0.00";
        String format = "0.00";
        if (num2 != 0 && num1 != 0) {
            DecimalFormat dec = new DecimalFormat(format);
            rate = dec.format((double) num1/num2*100);
            while (true) {
                if (rate.equals(format)) {
                    format = format + "0";
                    DecimalFormat dec1 = new DecimalFormat(format);
                    rate = dec1.format((double) num1/num2*100);
                } else {
                    break;
                }
            }
        } else if (num1 != 0 && num2 == 0) {
            rate = "100";
        }

        return rate;
    }

    public static int getFileLineCount(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
        lineNumberReader.skip(Integer.MAX_VALUE);
        int dictCount = lineNumberReader.getLineNumber()+1;
        fileReader.close();
        lineNumberReader.close();
        return dictCount;
    }

    public static String getMsg(String msg) {
        return "消息：["+Thread.currentThread().getName()+"]"+msg+"\n";
    }
}
