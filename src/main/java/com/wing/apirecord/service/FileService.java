package com.wing.apirecord.service;

import com.wing.apirecord.core.filter.ContentTypeFilter;
import com.wing.apirecord.core.filter.FilterChain;
import com.wing.apirecord.core.filter.MethodFilter;
import com.wing.apirecord.core.filter.UrlFilter;
import com.wing.apirecord.utils.ConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class FileService {

    public static String saveCodeRoot = ConfigUtil.getConfig("saveCodeRoot");
    public static String saveCodeBase = null;
    public static String saveCodeModule = ConfigUtil.getConfig("saveCodeModule");
    public static String saveCodeModuleData = null;
    public static String saveCodeModuleSchema = null;
    public static String saveCodeModuleTestsuites = null;


    public static void setSavePath(String savePath){
        if(savePath!=null)
        saveCodeRoot = savePath;
        saveCodeBase=saveCodeRoot+"/src/test/java/"+saveCodeModule;
        saveCodeModuleData=saveCodeBase+"/data";
        saveCodeModuleSchema=saveCodeBase+"/schema";
        saveCodeModuleTestsuites=saveCodeBase+"/testsuites";

        try {
            FileUtils.forceMkdir(new File(saveCodeModuleData));
            FileUtils.forceMkdir(new File(saveCodeModuleSchema));
            FileUtils.forceMkdir(new File(saveCodeModuleTestsuites));
        }catch (Exception e){
            log.error("创建路径失败",e);
        }
    }

    public void saveData(String fileName,String data) throws Exception {
        saveFile(saveCodeModuleData+"/"+fileName+".json",data);
    }

    public void saveSchema(String fileName,String data) throws Exception {
        saveFile(saveCodeModuleSchema+"/"+fileName+".json",data);
    }


    public void saveCodeModuleTestsuites(String fileName,String data) throws Exception {
        saveFile(saveCodeModuleTestsuites+"/"+fileName+".java",data);
    }

    public void saveFile(String savePath,String text) throws Exception{
        FileUtils.write(new File(savePath),text, Charset.forName("utf-8"));
    }


    public String readFile(String readPath) throws Exception{
        return FileUtils.readFileToString(new File(readPath), Charset.forName("utf-8"));
    }


    public String getApiDef() throws Exception{
        File file=new File(saveCodeBase + "/Api.java");
        if(file.exists()) {
            return  FileUtils.readFileToString(new File(saveCodeBase + "/Api.java"), Charset.forName("utf-8"));
        }else {
            return null;
        }
    }

    public void writeApiDef(String data) throws Exception{
        FileUtils.write(new File(saveCodeBase + "/Api.java"),data, Charset.forName("utf-8"));
    }


}
