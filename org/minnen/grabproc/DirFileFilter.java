package org.minnen.grabproc;

import java.io.File;
import java.io.FileFilter;

/**
 * FileFilter that only accepts readable directories.
 * @author David Minnen
 */
public class DirFileFilter implements FileFilter
{
  @Override
  public boolean accept(File file)
  {
    return file.isDirectory() && file.canRead();
  }

}
