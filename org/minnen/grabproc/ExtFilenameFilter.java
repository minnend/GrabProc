package org.minnen.grabproc;

import java.io.*;

/**
 * Filename filter that matches files with given extensions (case-insensitive).
 * 
 * @author David Minnen
 */
public class ExtFilenameFilter implements FilenameFilter
{
  protected String[] exts;

  /**
   * Filter for a single extension.
   * 
   * @param ext extension to accept.
   */
  public ExtFilenameFilter(String ext)
  {
    exts = new String[1];
    exts[0] = ext.toLowerCase();
  }

  /**
   * Filter for any of the given extensions.
   * 
   * @param exts array of exentensions to accept.
   */
  public ExtFilenameFilter(String[] exts)
  {
    this.exts = new String[exts.length];
    for (int i = 0; i < exts.length; ++i) {
      this.exts[i] = exts[i].toLowerCase();
    }
  }

  @Override
  public boolean accept(File dir, String name)
  {
    name = name.toLowerCase();
    for (String s : exts) {
      if (name.endsWith(s))
        return true;
    }
    return false;
  }
}
