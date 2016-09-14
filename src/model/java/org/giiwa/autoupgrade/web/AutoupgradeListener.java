package org.giiwa.autoupgrade.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.web.IListener;
import org.giiwa.framework.web.Module;

public class AutoupgradeListener implements IListener {

	static Log log = LogFactory.getLog(AutoupgradeListener.class);

	@Override
	public void onStart(Configuration conf, Module m) {
		// TODO Auto-generated method stub
		log.info("autoupgrade is starting ...");

	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void uninstall(Configuration conf, Module m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void upgrade(Configuration conf, Module m) {
		// TODO Auto-generated method stub

	}

}
