package com.example.api;

import com.example.util.FileUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
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

                    System.out.println(FileUtil.formetFileSize(time) + "/"
                            + FileUtil.formetFileSize(length)
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




}
