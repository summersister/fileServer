package com.example.task;

import com.example.common.FileMessage;
import com.example.domain.OutRate;
import com.example.util.FileUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class MyTask {

    @Scheduled(fixedRate = 2000)
    public void outRate() {

        Set<Map.Entry<String, OutRate>> list = FileMessage.speedMap.entrySet();

        if(list != null && list.size() > 0) {

            System.out.println("-------- 进度提醒 -------");

            for(Map.Entry<String, OutRate> map : list) {

                OutRate vo = map.getValue();

                float aaa = (float) vo.getNowSpend() / vo.getTotalSpend() * 100;

                if (vo.getTotalSpend() > 0) {

                    String str;

                    str = String.valueOf(aaa).length() > 5 ? String.valueOf(aaa).substring(0, 5) : String.valueOf(aaa);

                    System.out.println(vo.getFileName() + "--" +
                            FileUtil.formatFileSize(vo.getNowSpend()) + "/"
                            + FileUtil.formatFileSize(vo.getTotalSpend())
                            + "--" +
                            str + "%");
                }
            }
        }
    }
}
