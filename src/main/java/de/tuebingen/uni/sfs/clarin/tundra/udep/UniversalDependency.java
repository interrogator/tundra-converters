package de.tuebingen.uni.sfs.clarin.tundra.udep;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexandr Chernov on 01/08/16.
 * This is a converter for the universal dependency treebanks taken from
 * http://universaldependencies.org/
 */
public class UniversalDependency {
    /**
     * Provides a list of files in a certain folder
     * @param sourceFolder the name of the folder to browse
     * @return list of files in the given folder
     */
    public static List<String> getFilesList(String sourceFolder) {
        File folder = new File(sourceFolder);
        File[] listOfFiles = folder.listFiles();
        List<String> folderList = new ArrayList<String>();;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                folderList.add(listOfFiles[i].getName());
            }
        }
        return folderList;
    }
}
