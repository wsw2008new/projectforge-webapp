/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2012 Kai Reinhard (k.reinhard@micromata.com)
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

package org.projectforge.fibu.kost;

import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.xml.stream.XmlField;
import org.projectforge.xml.stream.XmlObject;

/**
 * Used in config.xml for the definition of the used business assessment schema. The business assessment is displayed in different
 * accounting areas, such as for DATEV accounting records.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "businessAssessment")
public class BusinessAssessmentConfig
{
  @XmlField(alias = "rows")
  private List<BusinessAssessmentRowConfig> rows;

  private final String heading = "business assessement";

  public BusinessAssessmentConfig()
  {
  }

  public List<BusinessAssessmentRowConfig> getRows()
  {
    return rows;
  }

  /**
   * @param id id or number of the row.
   * @return The found row or null if not found.
   */
  public BusinessAssessmentRowConfig getRow(final String id)
  {
    if (rows == null || id == null) {
      return null;
    }
    for (final BusinessAssessmentRowConfig row : rows) {
      if (id.equals(row.getId()) == true || id.equals(row.getNo()) == true) {
        return row;
      }
    }
    return null;
  }

  /**
   * @return the heading to be displayed first.
   */
  public String getHeading()
  {
    return heading;
  }

  @Override
  public String toString()
  {
    final ReflectionToStringBuilder builder = new ReflectionToStringBuilder(this);
    return builder.toString();
  }
}
