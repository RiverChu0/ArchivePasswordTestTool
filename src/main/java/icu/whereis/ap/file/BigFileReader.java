package icu.whereis.ap.file;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @from https://github.com/yongzhidai/ToolClass/blob/master/src/main/java/cn/dyz/tools/file/BigFileReader.java
 */
public class BigFileReader {
    private int threadSize;
    private String charset;
    private int bufferSize;
    private FileHandle handle;
    private ScheduledThreadPoolExecutor executorService;
    private long fileLength;
    private RandomAccessFile rAccessFile;
    private Set<StartEndPair> startEndPairs;
    private CyclicBarrier cyclicBarrier;
    private AtomicLong counter = new AtomicLong(0);
    // 任务跑完后的回调
    private CompleteCallback completeCallback;

    private BigFileReader(File file, FileHandle handle, String charset, int bufferSize, int threadSize){
        this.fileLength = file.length();
        this.handle = handle;
        this.charset = charset;
        this.bufferSize = bufferSize;
        this.threadSize = threadSize;
        try {
            this.rAccessFile = new RandomAccessFile(file,"r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.executorService = new ScheduledThreadPoolExecutor(threadSize);
        startEndPairs = new HashSet<StartEndPair>();
    }

    public void setCompleteCallback(CompleteCallback completeCallback) {
        this.completeCallback = completeCallback;
    }

    public void start() {
        long everySize = this.fileLength/this.threadSize;
        try {
            calculateStartEnd(0, everySize);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final long startTime = System.currentTimeMillis();
        cyclicBarrier = new CyclicBarrier(startEndPairs.size(),new Runnable() {

            @Override
            public void run() {
                System.out.println("use time: "+(System.currentTimeMillis()-startTime)+"ms");
                System.out.println("all line: "+counter.get());
                completeCallback.onComplete();
                completeCallback.onSuccess();
            }
        });
        for (StartEndPair pair:startEndPairs) {
            System.out.println("分配分片："+pair);
            this.executorService.execute(new SliceReaderTask(pair));
        }
        this.handle.init();
    }

    private void calculateStartEnd(long start,long size) throws IOException{
        if(start>fileLength-1){
            return;
        }
        StartEndPair pair = new StartEndPair();
        pair.start=start;
        long endPosition = start+size-1;
        if(endPosition>=fileLength-1){
            pair.end=fileLength-1;
            startEndPairs.add(pair);
            return;
        }

        rAccessFile.seek(endPosition);
        byte tmp =(byte) rAccessFile.read();
        while(tmp!='\n' && tmp!='\r'){
            endPosition++;
            if(endPosition>=fileLength-1){
                endPosition=fileLength-1;
                break;
            }
            rAccessFile.seek(endPosition);
            tmp =(byte) rAccessFile.read();
        }
        pair.end=endPosition;
        startEndPairs.add(pair);

        calculateStartEnd(endPosition+1, size);
    }



    public void shutdown(boolean now){
        try {
            this.rAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (now) {
            this.executorService.shutdownNow();
        } else {
            this.executorService.shutdown();
        }
        System.out.println("关闭了线程池");
    }

    public void shutdown(){
        shutdown(false);
    }

    private void handle(byte[] bytes) throws UnsupportedEncodingException {
        String line = null;
        if(this.charset == null){
            line = new String(bytes);
        }else{
            line = new String(bytes,charset);
        }
        if(line!=null && !"".equals(line)){
            this.handle.handle(line, counter.incrementAndGet(), this);
        }
    }

    private static class StartEndPair {
        public long start;
        public long end;

        @Override
        public String toString() {
            return "star="+start+";end="+end;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (end ^ (end >>> 32));
            result = prime * result + (int) (start ^ (start >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StartEndPair other = (StartEndPair) obj;
            if (end != other.end)
                return false;
            if (start != other.start)
                return false;
            return true;
        }

    }
    private class SliceReaderTask implements Runnable {
        private long start;
        private long sliceSize;
        private byte[] readBuff;
        /**
         * @param start 	read position (include)
         * @param end 	the position read to(include)
         */
        public SliceReaderTask(StartEndPair pair) {
            this.start = pair.start;
            this.sliceSize = pair.end-pair.start+1;
            this.readBuff = new byte[bufferSize];
        }

        @Override
        public void run() {
            try {
                MappedByteBuffer mapBuffer = rAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY,start, this.sliceSize);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                for(int offset=0; offset<sliceSize; offset+=bufferSize){
                    int readLength;
                    if(offset+bufferSize<=sliceSize){
                        readLength = bufferSize;
                    }else{
                        readLength = (int) (sliceSize-offset);
                    }
                    mapBuffer.get(readBuff, 0, readLength);
                    for(int i=0; i<readLength; i++){
                        byte tmp = readBuff[i];
                        if (tmp=='\n' || tmp=='\r') {
                            handle(bos.toByteArray());
                            bos.reset();
                        } else {
                            bos.write(tmp);
                        }
                    }
                }
                if(bos.size() > 0){
                    handle(bos.toByteArray());
                }
                cyclicBarrier.await();//测试性能用
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static class Builder {
        private int threadSize=1;
        private String charset=null;
        private int bufferSize=1024*1024;
        private FileHandle handle;
        private File file;
        public Builder(String file, FileHandle handle){
            this.file = new File(file);
            if(!this.file.exists())
                throw new IllegalArgumentException("文件不存在！");
            this.handle = handle;
        }

        public Builder withTreahdSize(int size){
            this.threadSize = size;
            return this;
        }

        public Builder withCharset(String charset){
            this.charset= charset;
            return this;
        }

        public Builder withBufferSize(int bufferSize){
            this.bufferSize = bufferSize;
            return this;
        }

        public BigFileReader build(){
            return new BigFileReader(this.file,this.handle,this.charset,this.bufferSize,this.threadSize);
        }
    }
}
