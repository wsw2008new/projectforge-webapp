/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.skillmatrix;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.projectforge.registry.Registry;
import org.projectforge.user.GroupDO;
import org.projectforge.user.PFUserContext;
import org.projectforge.user.UserDao;
import org.projectforge.user.UserGroupCache;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.GroupsComparator;
import org.projectforge.web.user.GroupsProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

/**
 * This is the edit formular page.
 * @author Werner Feder (werner.feder@t-online.de)
 */
@SuppressWarnings("deprecation")
public class TrainingEditForm extends AbstractEditForm<TrainingDO, TrainingEditPage>
{

  private static final long serialVersionUID = 359682752123823685L;

  private static final Logger log = Logger.getLogger(TrainingEditForm.class);

  @SpringBean(name = "trainingDao")
  private TrainingDao trainingDao;

  @SpringBean(name = "skillDao")
  private SkillDao skillDao;

  @SpringBean(name = "userDao")
  private UserDao userDao;

  private final FormComponent< ? >[] dependentFormComponents = new FormComponent[1];

  private TextField<String> valuesRating;

  private TextField<String> valuesCertificate;

  MultiChoiceListHelper<GroupDO> fullAccessGroupsListHelper, readonlyAccessGroupsListHelper;

  Collection<GroupDO> curUserGroups;


  private Model <String> labelFullModel;
  private Collection<Integer> curUserGroupIds;

  /**
   * @param parentPage
   * @param data
   */
  public TrainingEditForm(final TrainingEditPage parentPage, final TrainingDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  @Override
  public void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL50);
    { // Skill
      final FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "skill");
      final SkillTextSelectPanel parentSelectPanel = new SkillTextSelectPanel(fs.newChildId(), new PropertyModel<SkillDO>(data, "skill"), parentPage, "skillId");
      fs.add(parentSelectPanel);
      parentSelectPanel.setRequired(true);
      parentSelectPanel.init();
    }

    { // Title of training
      final FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "title");
      final RequiredMaxLengthTextField titleField = new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data,
          "title"));
      WicketUtils.setFocus(titleField);
      fs.add(titleField);
      dependentFormComponents[0] = titleField;
    }

    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Full access groups

      curUserGroupIds = new HashSet<Integer>();
      curUserGroupIds = userDao.getAssignedGroups(PFUserContext.getUser());
      final UserGroupCache userGroupCache = Registry.instance().getUserGroupCache();
      curUserGroups = new HashSet<GroupDO>();
      for (final Integer assignedGroup: curUserGroupIds) {
        curUserGroups.add(userGroupCache.getGroup(assignedGroup));
      }

      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.fullAccess"),
          getString("plugins.teamcal.access.groups"));
      final GroupsProvider groupsProvider = new GroupsProvider();
      final Collection<GroupDO> fullAccessGroups = new GroupsProvider().getSortedGroups(getData().getFullAccessGroupIds());
      fullAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator()).setFullList(
          groupsProvider.getSortedGroups());
      if (fullAccessGroups != null) {
        for (final GroupDO group : fullAccessGroups) {
          if (curUserGroups.contains(group) == false) {
            fullAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
          }
        }
      }
      final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<GroupDO>> (this.fullAccessGroupsListHelper, "assignedItems"), groupsProvider);
      groups.setRequired(true);
      fs.add(groups);

      final FieldsetPanel fs2 = gridBuilder.newFieldset("", getString("plugins.skillmatrix.skill.inherited")).setLabelFor(groups);
      fs2.setOutputMarkupId(true);
      labelFullModel = new Model<String>("");
      final Label labelFull = new Label (fs2.newChildId(), labelFullModel);
      labelFull.setOutputMarkupId(true);
      fs2.add(labelFull);
      fs2.getFieldset().setOutputMarkupId(true);
      labelFullModel.setObject(getGroupnames(curUserGroupIds.toArray(new Integer[0])));
    }

    {
      // Read-only access groups
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.teamcal.readonlyAccess"),
          getString("plugins.teamcal.access.groups"));
      final GroupsProvider groupsProvider = new GroupsProvider();
      final Collection<GroupDO> readOnlyAccessGroups = new GroupsProvider().getSortedGroups(getData().getReadOnlyAccessGroupIds());
      readonlyAccessGroupsListHelper = new MultiChoiceListHelper<GroupDO>().setComparator(new GroupsComparator()).setFullList(
          groupsProvider.getSortedGroups());
      if (readOnlyAccessGroups != null) {
        for (final GroupDO group : readOnlyAccessGroups) {
          readonlyAccessGroupsListHelper.addOriginalAssignedItem(group).assignItem(group);
        }
      }
      final Select2MultiChoice<GroupDO> groups = new Select2MultiChoice<GroupDO>(fs.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<GroupDO>>(this.readonlyAccessGroupsListHelper, "assignedItems"), groupsProvider);
      fs.add(groups);
    }

    gridBuilder.newGridPanel();
    { // Description
      final FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "description");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<String>(data, "description"))).setAutogrow();
    }

    { // startDate
      final FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "startDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "startDate"), DatePanelSettings.get().withTargetType(
          java.sql.Date.class)));
    }

    { // EndDate
      final FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "endDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<Date>(data, "endDate"), DatePanelSettings.get().withTargetType(
          java.sql.Date.class)));

    }

    { // Rating
      final FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "rating");
      valuesRating = new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "rating"));
      fs.addHelpIcon(getString("plugins.marketing.addressCampaign.values.format"));
      fs.add(valuesRating);
      fs.addAlertIcon(getString("plugins.skillmatrix.skilltraining.edit.warning.doNotChangeValues"));
      valuesRating.setRequired(false);
      valuesRating.add(new AbstractValidator<String>() {
        @Override
        protected void onValidate(final IValidatable<String> validatable)
        {
          if (TrainingDO.getValuesArray(validatable.getValue()) == null) {
            valuesRating.error(getString("plugins.skillmatrix.skilltraining.values.invalidFormat"));
          }
        }
      });
    }

    { // Certificate
      final FieldsetPanel fs = gridBuilder.newFieldset(TrainingDO.class, "certificate");
      valuesCertificate = new RequiredMaxLengthTextField(fs.getTextFieldId(), new PropertyModel<String>(data, "certificate"));
      fs.addHelpIcon(getString("plugins.marketing.addressCampaign.values.format"));
      fs.add(valuesCertificate);
      fs.addAlertIcon(getString("plugins.skillmatrix.skilltraining.edit.warning.doNotChangeValues"));
      valuesCertificate.setRequired(false);
      valuesCertificate.add(new AbstractValidator<String>() {
        @Override
        protected void onValidate(final IValidatable<String> validatable)
        {
          if (TrainingDO.getValuesArray(validatable.getValue()) == null) {
            valuesCertificate.error(getString("plugins.skillmatrix.skilltraining.values.invalidFormat"));
          }
        }
      });
    }

  }



  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  private String getGroupnames(final Integer[] ids) {
    String s ="";
    final UserGroupCache userGroupCache = Registry.instance().getUserGroupCache();
    for (final Integer id : ids) {
      s += userGroupCache.getGroupname(id) + ", ";
    }
    return s.substring(0, s.lastIndexOf(", "));
  }
}
