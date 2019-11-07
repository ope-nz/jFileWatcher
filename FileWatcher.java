package butt.droid;

import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BA.Hide;
import anywheresoftware.b4a.BA.ShortName;
import anywheresoftware.b4a.BA.Version;
import anywheresoftware.b4a.objects.collections.List;
import anywheresoftware.b4a.objects.collections.Map;
import anywheresoftware.b4a.keywords.Common;

import java.io.File;
import java.io.IOException;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;

import java.util.HashSet;

@ShortName("FileWatcher")
@Version(1.3F)

public class FileWatcher
{
  private List watchList = new List();  
  private String eventName;  
  private boolean watchingDesired = false;  
  private boolean watching = false;  
  private BA ba;
  
  public FileWatcher()
  {
    this.watchList.Initialize();
  }
  
  public FileWatcher Initialize(BA paramBA, String paramString)
  {
    this.ba = paramBA;
    this.eventName = paramString;
    
    if (this.watchList.getSize() > 0) this.watchList.Clear(); 
    
    return this;
  }
  
  public List GetWatchList()
  {
    List list = new List();
    list.Initialize();
    list.AddAll(this.watchList);
    return list;
  }
  
  public FileWatcher SetWatchList(List paramList)
  {
    if (this.watchingDesired)
    {
      this.watching = false;
    }
    
    this.watchList = paramList;

    if (this.watchingDesired)
    {      
      this.watching = true;
      StartWatching();
    } 
    return this;
  }
  
  public void StartWatching()
  {
    this.watchingDesired = true;
    this.watching = true;
    (new WatcherThread()).start();
  }
  
  public void StopWatching()
  {
    this.watchingDesired = false;
    this.watching = false;
  }
  
  @Hide
  @SuppressWarnings("unchecked")
  private class WatcherThread extends Thread
  {
    private WatcherThread() {}
    
    public void run()
    {
      try
      {
        WatchService watchService = FileSystems.getDefault().newWatchService();

        for (int i = 0; i < FileWatcher.this.watchList.getSize(); i++)
        {          
          String str1 = (String)FileWatcher.this.watchList.Get(i);
          Paths.get(str1, new String[0]).register(watchService, new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY });
        } 
        
        WatchKey watchKey = null;
        String str = FileWatcher.this.eventName.toLowerCase();

        while (FileWatcher.this.watching)
        {
          watchKey = watchService.take();
          if (!FileWatcher.this.watchingDesired) break;
          
          for (WatchEvent watchEvent : watchKey.pollEvents())
            {
              Path path = (Path)watchEvent.context();
              Path dir = (Path)watchKey.watchable();

              String filename = path.toString();
              String filedir = dir.toString();
       
              WatchEvent.Kind kind = watchEvent.kind();

              if (kind == StandardWatchEventKinds.ENTRY_CREATE)
              {    
                FileWatcher.this.ba.raiseEventFromDifferentThread(FileWatcher.this, null, 0, str + "_CreationDetected".toLowerCase(), false, new Object[] {filename,filedir}); continue;
              }
              
              if (kind == StandardWatchEventKinds.ENTRY_DELETE)
              {
                FileWatcher.this.ba.raiseEventFromDifferentThread(FileWatcher.this, null, 0, str + "_DeletionDetected".toLowerCase(), false, new Object[] {filename,filedir}); continue;
              }
              
              if (kind == StandardWatchEventKinds.ENTRY_MODIFY)
              {
                FileWatcher.this.ba.raiseEventFromDifferentThread(FileWatcher.this, null, 0, str + "_ModificationDetected".toLowerCase(), false, new Object[] {filename,filedir}); continue;
              }
              
              if (kind == StandardWatchEventKinds.OVERFLOW)
              {
                Common.Log("OVERFLOW");
                Common.Log(filename);
                Common.Log(filedir);
                FileWatcher.this.ba.raiseEventFromDifferentThread(FileWatcher.this, null, 0, str + "_Overflow".toLowerCase(), false, new Object[0]);
              }
          } 
          if (!watchKey.reset())
          {
            break;
          }
        } 
        
        if (!FileWatcher.this.watchingDesired)
        {
          FileWatcher.this.ba.raiseEventFromDifferentThread(null, null, 0, str + "_WatchingTerminated".toLowerCase(), false, new Object[0]);
        }
      }
      catch (Exception exception)
      {
        Common.Log("Error: " + exception.getMessage());
        exception.printStackTrace();
      } 
    }
  }  
}