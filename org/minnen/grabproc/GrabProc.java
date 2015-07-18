package org.minnen.grabproc;

import java.io.*;

import gnu.getopt.*;
import org.apache.commons.io.FileUtils;

/**
 * Utility to collect all basic images (jpegs, tiffs, pngs) from subdirs called "proc" into a new directory tree.
 * 
 * I store my photo collection by year and event (e.g. ".../photos/2014/0704 - fireworks"). All processed files go in to
 * a ./proc/ subdirectory after converting from raw and applying any chnages in lightroom or photoshop. This tool scans
 * my full photo directory and copies all of the processed images into a parallel directory tree. This new directory
 * makes it easier to backup my most important photos.
 * 
 * @author David Minnen
 */
public class GrabProc
{
  private static final String            DIRNAME_WITH_IMAGES = "proc";
  private static final ExtFilenameFilter filterImages        = new ExtFilenameFilter(new String[] { ".jpg", ".jpeg",
      ".png", ".tif", ".tiff"                               });
  private static final DirFileFilter     filterDirs          = new DirFileFilter();

  private static boolean                 bVerbose            = false;
  private static int                     nDirsSearched       = 0;
  private static int                     nProcDirs           = 0;
  private static int                     nImagesFound        = 0;
  private static int                     nImagesCopied       = 0;
  private static int                     nDupImages          = 0;

  /**
   * Copy all images in fsrc to parallel subdir in fdst.
   * 
   * @param fbase base directoy of search
   * @param fsrc subdirectory that holds photos
   * @param fdst base directory where photos are copied.
   * @return false on error.
   */
  private static boolean HandleProcDir(File fbase, File fsrc, File fdst)
  {
    try {
      ++nProcDirs;
      File[] files = fsrc.listFiles(filterImages);
      if (files.length < 1) {
        return true; // nothing to do
      }
      nImagesFound += files.length;

      // skip common base
      String[] a = fbase.getCanonicalPath().split("[\\\\/]");
      String[] b = fsrc.getCanonicalPath().split("[\\\\/]");
      int iDiff = 0;
      while (iDiff < a.length && iDiff < b.length && a[iDiff].equals(b[iDiff])) {
        ++iDiff;
      }

      if (bVerbose) {
        System.out.printf("Proc Dir: [%s] (%d images)\n", fsrc.getCanonicalPath(), files.length);
      }

      // build corresponding directories in destination dir
      File parent = fdst;
      int nSubs = b.length - 1; // last one is "proc"
      for (int i = iDiff; i < nSubs; i++) {
        File dir = new File(parent, b[i]);
        parent = dir;
        if (dir.exists()) {
          if (dir.isDirectory())
            continue;
          else {
            System.err.printf("Error: destination path exists but is not a directory (%s)\n", dir.getName());
            return false;
          }
        } else {
          if (!dir.mkdir()) {
            System.err.printf("Error: failed to create destination subdir (%s)\n", dir.getName());
            return false;
          }
        }
      }

      // now we can copy files
      for (File file : files) {
        // construct destination file
        File dst = new File(parent, file.getName());
        if (!dst.exists() || file.length() != dst.length() || file.lastModified() > dst.lastModified()) {
          FileUtils.copyFileToDirectory(file, parent, true);
          ++nImagesCopied;
        } else {
          ++nDupImages;
          // System.out.printf("Dup: [%s] <-> [%s]\n", file.getCanonicalPath(), dst.getCanonicalPath());
        }
      }

      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Search for all ./proc subdirectories under fdir and copy to parallel subdir under fdst.
   * 
   * @param fbase base directory of search (e.g. /home/david/photos).
   * @param fdir directory to search (e.g. /home/david/photos/2014).
   * @param fdst directory where photos will be copied (e.g. /home/david/photos-proc).
   * @return false if there is an error.
   */
  private static boolean SearchForProcImages(File fbase, File fdir, File fdst)
  {
    nDirsSearched++;
    File[] dirs = fdir.listFiles(filterDirs);
    for (File dir : dirs) {
      if (dir.getName().toLowerCase().equals(DIRNAME_WITH_IMAGES)) {
        if (!HandleProcDir(fbase, dir, fdst)) {
          return false;
        }
      } else {
        if (!SearchForProcImages(fbase, dir, fdst)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Search for all ./proc subdirectories under srcDir and copy to parallel subdir under dstDir.
   * 
   * @param srcDir directory under which photos are currently stored.
   * @param dstDir directory to which photos will be copied.
   * @return false on error.
   */
  private static boolean SearchForProcImages(File srcDir, File dstDir)
  {
    return SearchForProcImages(srcDir, srcDir, dstDir);
  }

  private static void usage()
  {
    System.out.println("USAGE java grabproc [options] <src-dir> <dst-dir>");
    System.out.println(" Options:");
    System.out.println("   -v                Use verbose output");
    System.out.println();
  }

  public static void main(String args[]) throws Exception
  {
    // Handle command line options.
    Getopt g = new Getopt("grabproc", args, "?v", null, true);
    int c;
    while ((c = g.getopt()) != -1) {
      switch (c) {
      case '?':
        usage();
        System.exit(0);
        break;
      case 'v':
        bVerbose = true;
        break;
      }
    }

    // Make sure we aren't missing any arguments.
    int i = g.getOptind();
    if (i > args.length - 2) {
      usage();
      System.exit(1);
    }

    String src = args[i++];
    String dst = args[i++];

    // Verify that source path is valid.
    File fsrc = new File(src);
    if (!fsrc.exists()) {
      System.err.println("Source path does not exist: " + src);
      System.exit(1);
    }
    if (!fsrc.isDirectory()) {
      System.err.println("Source path is not a directory: " + src);
      System.exit(1);
    }

    // Verify that destination path is valid.
    File fdst = new File(dst);
    if (!fdst.exists() && !fdst.mkdirs()) {
      System.err.printf("Destination path does not exist and unable to create it (%s)\n", dst);
      System.exit(1);
    }
    if (!fdst.isDirectory()) {
      System.err.println("Destination path is not a directory: " + dst);
      System.exit(1);
    }

    // Print source and destination directories.
    if (bVerbose) {
      System.out.printf("Source Path: [%s]\n", src);
      System.out.printf("Destination Path: [%s]\n", dst);
    }

    // Find proc/ subdirs and copy images.
    SearchForProcImages(fsrc, fdst);

    // Print some summary statistics.
    if (bVerbose) {
      System.out.printf("Dirs Searched: %d\n", nDirsSearched);
      System.out.printf("Proc Dirs: %d\n", nProcDirs);
      System.out.printf("Images Found: %d\n", nImagesFound);
      System.out.printf("Images Copied: %d\n", nImagesCopied);
      System.out.printf("Duplicate Images: %d\n", nDupImages);
    }

    System.exit(0);
  }
}
