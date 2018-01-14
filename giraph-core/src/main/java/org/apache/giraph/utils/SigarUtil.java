package org.apache.giraph.utils;

import com.google.common.io.Resources;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.log4j.Logger;
import org.hyperic.sigar.Sigar;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by 11363 on 5/31/2017.
 */
public class SigarUtil {


    public static Sigar getSigar(Mapper<?, ?, ?, ?>.Context context) throws IOException{
        final Logger LOG = Logger.getLogger(SigarUtil.class);

        String userHome = System.getProperty("user.home");
        String sigarFolderName = userHome + "/sigar_lib";
        File sigarFolder = new File(sigarFolderName);

        if(!sigarFolder.exists()){
            sigarFolder.mkdir();

            Configuration conf = context.getConfiguration();
            FileSystem fileSystem = FileSystem.get(URI.create("hdfs://master:9000"), conf);
            Path path = new Path("/libraries/libsigar-amd64-linux.so");
            if(!fileSystem.exists(path)){
                LOG.info(path.toString() + " does not exist!");
                return null;
            }
            InputStream in = fileSystem.open(path);
            File file = new File(sigarFolderName + "/libsigar-amd64-linux.so");
            if(!file.exists()){
                file.createNewFile();
            }
            OutputStream out = new FileOutputStream(file.getCanonicalFile());
            IOUtils.copyBytes(in, out, 4096, true);

            /*
            //System.out.println(Resources.getResource("sigar_lib.zip"));
            ///usr/local/lib/hadoop-1.2.1/tmp/mapred/local/taskTracker/hadoop/jobcache/job_201801121423_0006/jars/sigar_lib.zip
            //File file = new File(Resources.getResource("sigar_lib.zip").getFile());
            //InputStream is = SigarUtil.class.getResourceAsStream("sigar_lib.zip");
            //System.out.println("SigarUtil.class.getCanonicalName()" + SigarUtil.class.getCanonicalName());
            ZipFile zip = new ZipFile(file);
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                System.out.println("decompress file :" + entry.getName());
                File outFile = new File(sigarFolderName + "/" + entry.getName());
                BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(entry));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile));

                byte[] buffer = new byte[1024];
                while (true) {
                    int len = bis.read(buffer);
                    if (len == -1)
                        break;
                    bos.write(buffer, 0, len);
                }
                bis.close();
                bos.close();
            }
            zis.close();
            */
        }


        String seperator = null;
        if (OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows)
            seperator = ";";
        else
            seperator = ":";
        LOG.info("SigarUtil: Before set java.library.path, it is " + System.getProperty("java.library.path"));
        String path = System.getProperty("java.library.path") + seperator + sigarFolder.getCanonicalPath();
        System.setProperty("java.library.path", path);
        LOG.info("SigarUtil: java.library.path added!");
        LOG.info("SigarUtil: After set java.library.path, it is " + System.getProperty("java.library.path"));
        //System.load(sigarFolder.getCanonicalPath() + "/libsigar-amd64-linux.so");
        return new Sigar();
    }
}
