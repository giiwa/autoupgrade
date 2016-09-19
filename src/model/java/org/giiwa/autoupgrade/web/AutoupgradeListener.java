/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.autoupgrade.web;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.autoupgrade.web.admin.autoupgrade;
import org.giiwa.core.base.Http;
import org.giiwa.core.base.Http.Response;
import org.giiwa.core.base.MD5;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.IListener;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;

// TODO: Auto-generated Javadoc
public class AutoupgradeListener implements IListener {

  static Log log = LogFactory.getLog(AutoupgradeListener.class);

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.giiwa.framework.web.IListener#onStart(org.apache.commons.configuration.
   * Configuration, org.giiwa.framework.web.Module)
   */
  @Override
  public void onStart(Configuration conf, Module m) {

    log.info("autoupgrade is starting ...");

    new AutoupdateTask().schedule(X.AMINUTE);

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.IListener#onStop()
   */
  @Override
  public void onStop() {

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.giiwa.framework.web.IListener#uninstall(org.apache.commons.
   * configuration.Configuration, org.giiwa.framework.web.Module)
   */
  @Override
  public void uninstall(Configuration conf, Module m) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.giiwa.framework.web.IListener#upgrade(org.apache.commons.configuration.
   * Configuration, org.giiwa.framework.web.Module)
   */
  @Override
  public void upgrade(Configuration conf, Module m) {
    // TODO Auto-generated method stub

  }

  private static class AutoupdateTask extends Task {

    long interval = X.AHOUR;

    @Override
    public void onExecute() {
      interval = X.AHOUR;

      String modules = Global.getString("autoupgrade." + Model.node() + ".modules", null);
      String url = Global.getString("autoupgrade.url", null);

      if (!X.isEmpty(url) && !X.isEmpty(modules)) {
        while (url.startsWith("/")) {
          url = url.substring(1);
        }
        while (url.endsWith("/")) {
          url = url.substring(0, url.length() - 1);
        }
        if (!url.startsWith("http://")) {
          url = "http://" + url;
        }
        if (!url.endsWith("/admin/module/query")) {
          url += "/admin/module/query";
        }

        String[] ss = modules.split("[,， ]");
        boolean restart = false;
        for (String s : ss) {
          if (X.isEmpty(s))
            continue;
          JSON jo = JSON.create();
          jo.put("name", s);
          Response r = Http.post(url, jo);
          log.info("remote module=" + s + ", resp=" + r.body);

          JSON j1 = JSON.fromObject(r.body);
          if (j1 != null && j1.getInt(X.STATE) == 200) {
            Module m = Module.load(s);
            if (m == null || !X.isSame(m.getVersion(), j1.getString("version"))
                || !X.isSame(m.getBuild(), j1.getString("build"))) {

              File f = _download(url, j1.getString("uri"), j1.getString("md5"));
              if (f != null) {

                OpLog.info(autoupgrade.class, "download", f.getName(), null, null);

                String name = j1.getString("uri");
                int i = name.lastIndexOf("/");
                name = name.substring(i + 1);
                if (_upgrade(name, f)) {
                  restart = true;
                }
              } else {
                // download error
                interval = X.AMINUTE;
              }
            } else {
              OpLog.info(autoupgrade.class, "check", "[" + s + "], same build, ignore, remote=" + r.body, null, null);
            }
          } else {
            OpLog.warn(autoupgrade.class, "check", "[" + s + "], got=" + r.body, null, url);
          }
        }

        if (restart) {
          OpLog.warn(autoupgrade.class, "restart", "autoupgrade shutdown the server", null, null);
          System.exit(0);
        }
      }
    }

    private boolean _upgrade(String name, File f) {
      try {
        String id = Repo.store(name, f);
        boolean restart = Module.install(Repo.load(id));
        OpLog.info(autoupgrade.class, "upgrade", "success, name=" + name, null, null);
        return restart;
      } catch (Exception e) {
        interval = X.AMINUTE;
        OpLog.warn(autoupgrade.class, "upgrade", "failed, name=" + name, null, null);
      }
      return false;
    }

    private File _download(String url, String repo, String md5) {
      int i = url.indexOf("/", 10);
      url = url.substring(0, i) + repo;
      i = repo.lastIndexOf("/");
      String name = repo.substring(i + 1);
      Temp t = Temp.create(name);
      File f = t.getFile();

      // System.out.println("url=" + url);
      int len = Http.download(url, f);
      if (len > 0) {
        String m1 = MD5.md5(f);
        if (X.isSame(md5, m1)) {
          return f;
        } else {
          OpLog.warn(autoupgrade.class, "download", "failed, url=" + url + ", md5=" + m1 + ", expected=" + md5, null,
              null);
        }
      } else {
        OpLog.warn(autoupgrade.class, "download", "failed, url=" + url + ", size=0", null, null);
      }
      return null;
    }

    @Override
    public void onFinish() {
      this.schedule(interval);
    }

  }

  /**
   * The main method.
   *
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {
    try {
      System.setProperty("home", "/Users/wujun/d/giiwa/");

      /**
       * initialize the configuration
       */
      Config.init("home", "giiwa");
      Temp.init(Config.getConfig());
      AutoupdateTask t = new AutoupdateTask();
      String url = "http://giiwa.org/admin/module/query";
      String repo = "/repo/erjfpyvz3qqjz/thirdlogin_1.0.1_1609070812.zip";
      String md5 = "d8d2c6d28a55edb71599d09d6e854481";
      File f = t._download(url, repo, md5);
      System.out.println(f.getAbsolutePath());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
