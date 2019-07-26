package com.example.api;

import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

@RequestMapping("/api")
@RestController
public class FileApi {

    @Autowired
    private MyThread base;

    private static final Map<String, File> fileMap = new HashMap();

    private static final Map<String, Long> speedMap = new HashMap();

    @RequestMapping(value = "/getDirList")
    public List<Map<String, String>> getDirList(String id) {

        List<Map<String, String>> list = new ArrayList();

        if(StringUtils.isBlank(id)){

            this.createCP(list);

        } else {

            if(fileMap.containsKey(id)){

                File file = fileMap.get(id);

                File[] fs = file.listFiles();

                for (int i = 0; i < fs.length; i++) {

                    Map<String, String> map = new HashMap();
                    map.put("name", fs[i].getName());
                    map.put("size", FormetFileSize(fs[i].length()));
                    map.put("sizeMax", "");
                    map.put("id", this.createID(fs[i]));
                    map.put("isFile", fs[i].isFile() ? "0" : "1");

                    list.add(map);
                }
            }
        }

        return list;
    }

    private void createCP(List<Map<String, String>> list) {

        FileSystemView fsv = FileSystemView.getFileSystemView();

        // 列出所有windows 磁盘
        File[] fs = File.listRoots();

        for (int i = 0; i < fs.length; i++) {

            Map<String, String> map = new HashMap();
            map.put("name", fsv.getSystemDisplayName(fs[i]));
            map.put("size", FormetFileSize(fs[i].getFreeSpace()));
            map.put("sizeMax", FormetFileSize(fs[i].getTotalSpace()));
            map.put("id", this.createID(fs[i]));
            map.put("isFile", "1");
            map.put("cp", "1");
            list.add(map);
        }
    }

    private String createID(File f) {

        String uuid = UUID.randomUUID().toString();
        fileMap.put(uuid, f);
        return uuid;
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

    /**
     * 下载指定文件  打印进度
     *
     * @param id
     * @param response
     */
    @RequestMapping("/download")
    public void download(String id, HttpServletResponse response) {

        File file = fileMap.get(id);

        String uuid = UUID.randomUUID().toString();

        try {

            speedMap.put(uuid, 0L);

            String filename = file.getName();

            System.out.println("start ----->" + filename);
            // 以流的形式下载文件。
            InputStream fis = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[1024 * 10 * 20];

            response.reset();
            response.addHeader("Content-Disposition", "attachment;filename=" + new String(filename.getBytes()));
            response.addHeader("Content-Length", "" + file.length());
            response.setContentType("application/octet-stream");
            OutputStream toClient = new BufferedOutputStream(response.getOutputStream());

            int len;
            long size = 0;

            this.base.outRate(uuid, file.length(), speedMap);

            while ((len = fis.read(buffer)) != -1) {

                if(len < 0){

                    size = size - len;

                } else {

                    size = len + size;
                }

                speedMap.put(uuid, new Long(size));

                toClient.write(buffer, 0, len);
            }

            speedMap.remove(uuid);

            toClient.flush();
            fis.read(buffer);
            fis.close();
            toClient.close();

            System.out.println("success down ----->" + filename);
        } catch (Exception e) {

            speedMap.remove(uuid);
            e.printStackTrace();
        }
    }

    /**
     * 用户清理缓存 (可完善)
     *
     * @return
     */
    @RequestMapping(value = "/cleanMap")
    public String cleanMap() {

        this.fileMap.clear();

        return "success";
    }

    /**
     * 启动文件 (可完善)
     *
     * @return
     */
    @RequestMapping(value = "/openFile")
    public String openFile(String id) {

        if(!StringUtils.isBlank(id) && fileMap.containsKey(id)){

            File file = this.fileMap.get(id);

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

        if(!StringUtils.isBlank(id) && fileMap.containsKey(id)){

            File dirFile = fileMap.get(id);

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

            speedMap.put(uuid, 0L);


            try {
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(dest));
                int length;
                byte[] buffer = new byte[1024 * 10 * 20];
                BufferedInputStream inputStream = new BufferedInputStream(file.getInputStream());
                long size = 0;

                this.base.outRate(uuid, file.getSize(), speedMap);

                while ((length = inputStream.read(buffer)) != -1) {

                    if(length < 0){

                        size = size - length;

                    } else {

                        size = length + size;
                    }

                    speedMap.put(uuid, new Long(size));

                    stream.write(buffer, 0, length);
                }

                speedMap.remove(uuid);
                stream.flush();
                stream.close();
                System.out.println("上传成功");
                return "上传成功";
            } catch (Exception e) {

                speedMap.remove(uuid);
                System.out.println(e.toString());
            }
        }
        return "上传失败！";
    }
}
