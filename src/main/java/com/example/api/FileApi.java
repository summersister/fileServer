package com.example.api;

import com.example.common.FileMessage;
import com.example.domain.OutRate;
import com.example.util.FileUtil;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.*;

@RequestMapping("/api")
@RestController
public class FileApi {

    @Autowired
    private MyThread base;

    @RequestMapping(value = "/getDirList")
    public List<Map<String, String>> getDirList(String id) {

        List<Map<String, String>> list = new ArrayList();

        if(StringUtils.isBlank(id)){

            this.createCP(list);

        } else {

            if(FileMessage.fileMap.containsKey(id)){

                File file = FileMessage.fileMap.get(id);

                File[] fs = file.listFiles();

                for (int i = 0; i < fs.length; i++) {

                    Map<String, String> map = new HashMap();
                    map.put("name", fs[i].getName());
                    map.put("size", FileUtil.formatFileSize(fs[i].length()));
                    map.put("sizeMax", "");
                    map.put("id", this.createID(fs[i]));
                    map.put("isFile", fs[i].isFile() ? "0" : "1");

                    list.add(map);
                }
            }
        }

        return list;
    }

    @RequestMapping(value = "/getDesktopList")
    public List<Map<String, String>> getDesktopList() {

        List<Map<String, String>> list = new ArrayList();

        FileSystemView fsv = FileSystemView.getFileSystemView();

        // 列出 桌面文件
        File[] fs = fsv.getHomeDirectory().listFiles();

        for (int i = 0; i < fs.length; i++) {

            Map<String, String> map = new HashMap();
            map.put("name", fsv.getSystemDisplayName(fs[i]));
            map.put("size", FileUtil.formatFileSize(fs[i].getFreeSpace()));
            map.put("sizeMax", FileUtil.formatFileSize(fs[i].getTotalSpace()));
            map.put("id", this.createID(fs[i]));
            map.put("isFile", "1");
            map.put("cp", "1");

            list.add(map);
        }

        return list;
    }

    /**
     * 获取windows 磁盘
     *
     * @param list
     */
    private void createCP(List<Map<String, String>> list) {

        FileSystemView fsv = FileSystemView.getFileSystemView();

        // 列出所有windows 磁盘
        File[] fs = File.listRoots();

        for (int i = 0; i < fs.length; i++) {

            Map<String, String> map = new HashMap();
            map.put("name", fsv.getSystemDisplayName(fs[i]));
            map.put("size", FileUtil.formatFileSize(fs[i].getFreeSpace()));
            map.put("sizeMax", FileUtil.formatFileSize(fs[i].getTotalSpace()));
            map.put("id", this.createID(fs[i]));
            map.put("isFile", "1");
            map.put("cp", "1");

            list.add(map);
        }
    }

    private String createID(File f) {

        String uuid = UUID.randomUUID().toString();
        FileMessage.fileMap.put(uuid, f);
        return uuid;
    }

    /**
     * 下载指定文件  打印进度
     *
     * @param id
     * @param response
     */
    @RequestMapping("/download")
    public void download(String id, HttpServletResponse response) {

        File file = FileMessage.fileMap.get(id);

        String uuid = UUID.randomUUID().toString();

        InputStream fis = null;

        OutputStream toClient = null;

        try {


            OutRate vo = new OutRate();

            vo.setFileName(file.getName());
            vo.setTotalSpend(file.length());

            FileMessage.speedMap.put(uuid, vo);

            String filename = file.getName();

            System.out.println("start ----->" + filename);
            // 以流的形式下载文件。
            fis = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[1024 * 10 * 20];

            response.reset();
            response.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes()));
            response.addHeader("Content-Length", "" + file.length());
            response.setContentType("application/octet-stream");
            toClient = new BufferedOutputStream(response.getOutputStream());

            int len;
            long size = 0;

            while ((len = fis.read(buffer)) != -1) {

                if(len < 0){

                    size = size - len;

                } else {

                    size = len + size;
                }

                vo.setNowSpend(size);

                toClient.write(buffer, 0, len);
            }

            FileMessage.speedMap.remove(uuid);

            toClient.flush();
            fis.read(buffer);
            fis.close();
            toClient.close();

            System.out.println("success down ----->" + filename);

        } catch (Exception e) {

            FileMessage.speedMap.remove(uuid);
            e.printStackTrace();
            if(fis != null) {

                try {
                    fis.close();

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            if(toClient != null) {

                try {
                    toClient.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }

    /**
     * 用户清理缓存 (可完善)
     *
     * @return
     */
    @RequestMapping(value = "/cleanMap")
    public String cleanMap() {

        FileMessage.fileMap.clear();

        return "success";
    }

    /**
     * 启动文件 (可完善)
     *
     * @return
     */
    @RequestMapping(value = "/openFile")
    public String openFile(String id) {

        if(!StringUtils.isBlank(id) && FileMessage.fileMap.containsKey(id)){

            File file = FileMessage.fileMap.get(id);

            if(file != null){

                this.base.openFile(file);
            }
        }

        return "success";
    }

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ResponseBody
    public String upload(@RequestParam(value = "file", required = false) MultipartFile file, String id) {

        if(!StringUtils.isBlank(id) && FileMessage.fileMap.containsKey(id)){

            File dirFile = FileMessage.fileMap.get(id);

            if(dirFile == null && dirFile.isFile()){

                return "????????????";
            }

            if (file.isEmpty()) {
                return "上传失败，请选择文件";
            }

            String fileName = file.getOriginalFilename();

            String pathT = dirFile.getPath() + fileName;

            File dest = new File(pathT);

            if(dest.exists()){

                File file1 = new File(dirFile.getPath() + "(1)" + fileName);

                dest = file1;
            }

            String uuid = UUID.randomUUID().toString();

            OutRate vo = new OutRate();

            vo.setFileName(dest.getName());
            vo.setTotalSpend(file.getSize());

            FileMessage.speedMap.put(uuid, vo);


            try {
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(dest));
                int length;
                byte[] buffer = new byte[1024 * 10 * 20];
                BufferedInputStream inputStream = new BufferedInputStream(file.getInputStream());
                long size = 0;

                while ((length = inputStream.read(buffer)) != -1) {

                    if(length < 0){

                        size = size - length;

                    } else {

                        size = length + size;
                    }

                    vo.setNowSpend(size);

                    stream.write(buffer, 0, length);
                }

                FileMessage.speedMap.remove(uuid);

                stream.flush();
                stream.close();
                System.out.println("上传成功");
                return "上传成功";
            } catch (Exception e) {

                FileMessage.speedMap.remove(uuid);
                e.printStackTrace();
            }
        }
        return "上传失败！";
    }
}
