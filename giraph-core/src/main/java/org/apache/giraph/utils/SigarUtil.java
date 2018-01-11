package org.apache.giraph.utils;

import com.google.common.io.Resources;
import org.apache.log4j.Logger;
import org.hyperic.sigar.Sigar;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by 11363 on 5/31/2017.
 */
public class SigarUtil {


    public static Sigar getSigar() throws IOException{
        final Logger LOG = Logger.getLogger(SigarUtil.class);
        String ResourceSigarFolderName = "sigar_lib";
        URL sigarFolderNameURL = Resources.getResource(ResourceSigarFolderName);
        File sigarFolder = new File(sigarFolderNameURL.getFile());

        if (!sigarFolder.exists()) {
            sigarFolder.mkdir();

            File file = new File(Resources.getResource("sigar_lib.zip").getFile());
            ZipFile sigarZip = new ZipFile(file);

            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                System.out.println("decompress file :" + entry.getName());
                File outFile = new File(sigarFolderNameURL.getFile() + entry.getName());
                BufferedInputStream bis = new BufferedInputStream(sigarZip.getInputStream(entry));
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
        if( (new File(sigarFolder.getCanonicalPath() + "/libsigar-amd64-linux.so")).exists())
            LOG.info(sigarFolder.getCanonicalPath() + "exists!");
        else
            LOG.info(sigarFolder.getCanonicalPath() + "does not exist!");
        return new Sigar();
    }
}
