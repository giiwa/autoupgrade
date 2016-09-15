package org.giiwa.autoupgrade.web.admin;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Path;

public class autoupgrade extends Model {

  @Path(login = true, access = "access.config.admin")
  public void onGet() {
    int s = this.getInt("s");
    int n = this.getInt("n", 20, "number.per.page");

    W q = W.create("model", "admin.autoupgrade");

    Beans<OpLog> bs = OpLog.load(q, s, n);
    this.set(bs, s, n);

    this.show("/admin/autoupgrade.logs.html");
  }

  @Path(path = "setting", login = true, access = "access.config.admin")
  public void setting() {
    if (method.isPost()) {
      Global.setConfig("autoupgrade.url", this.getString("url"));
      Global.setConfig("autoupgrade." + Model.node() + ".modules", this.getString("modules"));

      this.set(X.MESSAGE, lang.get("save.success"));
    }
    this.set("modules", Global.getString("autoupgrade." + Model.node() + ".modules", ""));
    this.show("/admin/autoupgrade.setting.html");
  }

}
