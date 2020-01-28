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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.TreeNodeFlexiCellRenderer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.TooledController;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CalloutSettings;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.helpers.Settings;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.util.Formatter;
import org.olat.core.util.nodes.INode;
import org.olat.course.assessment.IndentedNodeRenderer;
import org.olat.course.assessment.ui.tool.AssessmentStatusCellRenderer;
import org.olat.course.learningpath.manager.LearningPathCourseTreeModelBuilder;
import org.olat.course.learningpath.ui.LearningPathDataModel.LearningPathCols;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.AssessmentService;
import org.olat.modules.assessment.Overridable;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 16 Aug 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class LearningPathListController extends FormBasicController implements TooledController {

	private static final String CMD_END_DATE = "endDate";
	
	private final AtomicInteger counter = new AtomicInteger();
	private final TooledStackedPanel stackPanel;
	private FlexiTableElement tableEl;
	private LearningPathDataModel dataModel;
	private Link resetStatusLink;
	
	private CloseableCalloutWindowController ccwc;
	private Controller endDateEditCtrl;
	
	private final UserCourseEnvironment userCourseEnv;
	private final RepositoryEntry courseEntry;
	private final boolean canEdit;
	
	@Autowired
	private AssessmentService assessmentService;

	public LearningPathListController(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel,
			UserCourseEnvironment userCourseEnv) {
		super(ureq, wControl, LAYOUT_BAREBONE);
		this.userCourseEnv = userCourseEnv;
		this.stackPanel = stackPanel;
		this.courseEntry = userCourseEnv.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
		this.canEdit = getCanEdit();
		initForm(ureq);
	}
	
	private boolean getCanEdit() {
		IdentityEnvironment identityEnv = new IdentityEnvironment();
		identityEnv.setIdentity(getIdentity());
		UserCourseEnvironment myCourseEnv = new UserCourseEnvironmentImpl(identityEnv, userCourseEnv.getCourseEnvironment());
		return !myCourseEnv.isCourseReadOnly() && (myCourseEnv.isAdmin() || myCourseEnv.isCoach());
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		
		IndentedNodeRenderer intendedNodeRenderer = new IndentedNodeRenderer();
		intendedNodeRenderer.setIndentationEnabled(false);
		FlexiCellRenderer nodeRenderer = new TreeNodeFlexiCellRenderer(intendedNodeRenderer);
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.node, nodeRenderer));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.start));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.end));
		FlexiCellRenderer obligationRenderer = new ObligationCellRenderer(getLocale());
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.obligation, obligationRenderer));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.duration));
		DefaultFlexiColumnModel firstVisitColumnModel = new DefaultFlexiColumnModel(LearningPathCols.firstVisit);
		firstVisitColumnModel.setDefaultVisible(false);
		columnsModel.addFlexiColumnModel(firstVisitColumnModel);
		DefaultFlexiColumnModel lastVisitColumnModel = new DefaultFlexiColumnModel(LearningPathCols.lastVisit);
		lastVisitColumnModel.setDefaultVisible(false);
		columnsModel.addFlexiColumnModel(lastVisitColumnModel);
		FlexiCellRenderer statusRenderer = new AssessmentStatusCellRenderer(getTranslator());
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.status, statusRenderer));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.fullyAssessedDate));
		FlexiCellRenderer progressRenderer = new LearningPathProgressRenderer(getLocale());
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(LearningPathCols.progress, progressRenderer));

		dataModel = new LearningPathDataModel(columnsModel);
		tableEl = uifactory.addTableElement(getWindowControl(), "table", dataModel, 250, false, getTranslator(), formLayout);
		tableEl.setEmtpyTableMessageKey("table.empty");
		tableEl.setBordered(true);
		tableEl.setNumOfRowsEnabled(false);
		
		loadModel();
	}
	
	@Override
	public void initTools() {
		// Never enable this function in a productive environment. It may lead to corrupt data.
		if (Settings.isDebuging()) {
			resetStatusLink = LinkFactory.createToolLink("reset.all.status", translate("reset.all.status"), this);
			resetStatusLink.setIconLeftCSS("o_icon o_icon-lg o_icon_exclamation");
			stackPanel.addTool(resetStatusLink, Align.right);
		}
	}

	void loadModel() {
		userCourseEnv.getScoreAccounting().evaluateAll(true);
		LearningPathCourseTreeModelBuilder learningPathCourseTreeModelBuilder = new LearningPathCourseTreeModelBuilder(userCourseEnv);
		GenericTreeModel learningPathTreeModel = learningPathCourseTreeModelBuilder.build();
		List<LearningPathRow> rows = forgeRows(learningPathTreeModel);
		dataModel.setObjects(rows);
		tableEl.reset(true, true, true);
	}

	private List<LearningPathRow> forgeRows(GenericTreeModel learningPathTreeModel) {
		List<LearningPathRow> rows = new ArrayList<>();
		TreeNode rootNode = learningPathTreeModel.getRootNode();
		forgeRowAndChildren(rows, rootNode, null);
		return rows;
	}

	private void forgeRowAndChildren(List<LearningPathRow> rows, INode iNode, LearningPathRow parent) {
		if (iNode instanceof LearningPathTreeNode) {
			LearningPathTreeNode learningPathNode = (LearningPathTreeNode) iNode;
			forgeRowAndChildren(rows, learningPathNode, parent);
		}
	}

	private void forgeRowAndChildren(List<LearningPathRow> rows, LearningPathTreeNode learningPathNode,
			LearningPathRow parent) {
		LearningPathRow row = forgeRow(learningPathNode, parent);
		rows.add(row);

		int childCount = learningPathNode.getChildCount();
		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			INode child = learningPathNode.getChildAt(childIndex);
			forgeRowAndChildren(rows, child, row);
		}
	}

	private LearningPathRow forgeRow(LearningPathTreeNode treeNode, LearningPathRow parent) {
		LearningPathRow row = new LearningPathRow(treeNode);
		row.setParent(parent);
		forgeEndDate(row, treeNode);
		return row;
	}

	private void forgeEndDate(LearningPathRow row, LearningPathTreeNode treeNode) {
		Overridable<Date> endDate = treeNode.getEndDate();
		if (!canEdit && !endDate.isOverridden()) {
			// Show date as plain text
			return;
		}
		
		if (row.getEndDate().getCurrent() != null) {
			Date currentEndDate = endDate.getCurrent();
			StringBuilder sb = new StringBuilder();
			sb.append(Formatter.getInstance(getLocale()).formatDateAndTime(currentEndDate));
			if (treeNode.getEndDate().isOverridden()) {
				sb.append(" <i class='o_icon o_icon_info'> </i>");
			}
			FormLink endDateLink = uifactory.addFormLink("o_end_" + counter.getAndIncrement(), CMD_END_DATE,
					sb.toString(), null, null, Link.NONTRANSLATED);
			endDateLink.setUserObject(treeNode.getCourseNode());
			row.setEndDateFormItem(endDateLink);
		}
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source instanceof FormLink) {
			FormLink link = (FormLink) source;
			if (CMD_END_DATE.equals(link.getCmd())) {
				doEditEndDate(ureq, link);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == endDateEditCtrl) {
			if (event == FormEvent.DONE_EVENT) {
				loadModel();
			}
			ccwc.deactivate();
			cleanUp();
		} else if (source == ccwc) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}

	private void cleanUp() {
		removeAsListenerAndDispose(endDateEditCtrl);
		removeAsListenerAndDispose(ccwc);
		endDateEditCtrl = null;
		ccwc = null;
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == resetStatusLink) {
			doResetStatus();
		}
		super.event(ureq, source, event);
	}
	
	private void doEditEndDate(UserRequest ureq, FormLink link) {
		removeAsListenerAndDispose(ccwc);
		removeAsListenerAndDispose(endDateEditCtrl);
		
		CourseNode courseNode = (CourseNode)link.getUserObject();
		AssessmentEntry assessmentEntry = assessmentService.loadAssessmentEntry(
				userCourseEnv.getIdentityEnvironment().getIdentity(), courseEntry, courseNode.getIdent());
		
		endDateEditCtrl = new EndDateEditController(ureq, getWindowControl(), assessmentEntry, canEdit);
		listenTo(endDateEditCtrl);
		
		CalloutSettings settings = new CalloutSettings();
		ccwc = new CloseableCalloutWindowController(ureq, getWindowControl(), endDateEditCtrl.getInitialComponent(),
				link.getFormDispatchId(), "", true, "", settings);
		listenTo(ccwc);
		ccwc.activate();
	}

	private void doResetStatus() {
		List<AssessmentEntry> assessmentEntries = assessmentService.loadAssessmentEntriesByAssessedIdentity(getIdentity(), courseEntry);
		for (AssessmentEntry assessmentEntry : assessmentEntries) {
			assessmentEntry.setFullyAssessed(null);
			assessmentEntry.setAssessmentStatus(null);
			assessmentService.updateAssessmentEntry(assessmentEntry);
		}
		userCourseEnv.getScoreAccounting().evaluateAll(true);
		loadModel();
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void doDispose() {
		//
	}

}
