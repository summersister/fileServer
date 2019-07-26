package com.example.api;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;

@Component
public class MyThread {

    @Async
    void outRate(String uuid, long length, Map<String, Long> speedMap) throws InterruptedException {

        while (true){

            if(speedMap.containsKey(uuid)){

                Long time = speedMap.get(uuid);

                float aaa = (float) time / length * 100 ;

                if(time > 0){

                    String str;

                    str = String.valueOf(aaa).length() > 5 ? String.valueOf(aaa).substring(0,5) : String.valueOf(aaa);

                    System.out.println(this.FormetFileSize(time) + "/"
                            + this.FormetFileSize(length)
                            + "--" +
                            str + "%");
                } else {

                    System.out.println("咳咳咳咳");
                }

                Thread.sleep(1000);

            } else {

                return;
            }
        }
    }

    @Async
    void openFile(File file) {

        try {

            Runtime.getRuntime().exec(file.getPath());

        } catch (IOException e) {

            e.printStackTrace();
        }

    }


    private String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "K";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "G";
        }
        return fileSizeString;
    }

}
