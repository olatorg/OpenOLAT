/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.learningpath.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.course.editor.NodeEditController;

/**
 * 
 * Initial date: 27 Aug 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class TabbableLeaningPathNodeConfigController extends ActivateableTabbableDefaultController {

	private static final String PANE_TAB_LEARNIN_PATH = "pane.tab.learning.path";
	private final static String[] paneKeys = { PANE_TAB_LEARNIN_PATH };
	
	private final VelocityContainer configVC;
	private final Controller configCtrl;
	private TabbedPane tabPane;
	
	public TabbableLeaningPathNodeConfigController(UserRequest ureq, WindowControl wControl, Controller configCtrl) {
		super(ureq, wControl);
		this.configCtrl = configCtrl;

		listenTo(configCtrl);

		configVC = createVelocityContainer("config");
		configVC.put("config", configCtrl.getInitialComponent());
	}

	@Override
	public void addTabs(TabbedPane tabbedPane) {
		tabPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_LEARNIN_PATH), configVC);
	}

	@Override
	public String[] getPaneKeys() {
		return paneKeys;
	}

	@Override
	public TabbedPane getTabbedPane() {
		return tabPane;
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == configCtrl && event.equals(Event.DONE_EVENT)) {
			fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
		}
	}
	
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}

	@Override
	protected void doDispose() {
		//
	}

}