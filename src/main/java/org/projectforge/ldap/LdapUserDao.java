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

package org.projectforge.ldap;

import java.util.List;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LdapUserDao extends LdapPersonDao
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LdapUserDao.class);

  public static final String DEACTIVATED_SUB_CONTEXT = "deactivated";

  private static final String DEACTIVATED_SUB_CONTEXT2 = "ou=" + DEACTIVATED_SUB_CONTEXT;

  private static final String DEACTIVATED_SUB_CONTEXT3 = DEACTIVATED_SUB_CONTEXT2 + ",";

  static final String DEACTIVATED_MAIL = "deactivated@devnull.com";

  public static final String RESTRICTED_USER_SUB_CONTEXT = "restricted";

  private static final String RESTRICTED_USER_SUB_CONTEXT2 = "ou=" + RESTRICTED_USER_SUB_CONTEXT;

  private static final String RESTRICTED_USER_SUB_CONTEXT3 = RESTRICTED_USER_SUB_CONTEXT2 + ",";

  private boolean useUidInDn = false;

  public static boolean isDeactivated(final LdapPerson user)
  {
    return user.isDeactivated()
        || user.getOrganizationalUnit() != null
        && LdapUtils.getOu(user.getOrganizationalUnit()).contains(DEACTIVATED_SUB_CONTEXT) == true;
  }

  public static boolean isRestrictedUser(final LdapPerson user)
  {
    return user.isRestrictedUser()
        || user.getOrganizationalUnit() != null
        && LdapUtils.getOu(user.getOrganizationalUnit()).contains(RESTRICTED_USER_SUB_CONTEXT) == true;
  }

  public LdapUserDao()
  {
    useUidInDn = true;
  }

  /**
   * @see org.projectforge.ldap.LdapPersonDao#getIdAttrId()
   */
  @Override
  public String getIdAttrId()
  {
    return "employeeNumber";
  }

  /**
   * @see org.projectforge.ldap.LdapPersonDao#getId(org.projectforge.ldap.LdapPerson)
   */
  @Override
  public String getId(final LdapPerson obj)
  {
    return obj.getEmployeeNumber();
  }

  /**
   * @see org.projectforge.ldap.LdapPersonDao#mapToObject(String, javax.naming.directory.Attributes)
   */
  @Override
  protected LdapPerson mapToObject(final String dn, final Attributes attributes) throws NamingException
  {
    final LdapPerson person = super.mapToObject(dn, attributes);
    if (dn != null) {
      if (dn.contains(DEACTIVATED_SUB_CONTEXT2) == true) {
        person.setDeactivated(true);
      }
      if (dn.contains(RESTRICTED_USER_SUB_CONTEXT2) == true) {
        person.setRestrictedUser(true);
      }

    }
    final Object userPassword = LdapUtils.getAttributeValue(attributes, "userPassword");
    if (userPassword != null) {
      person.setPasswordGiven(true);
    }
    return person;
  }

  public void deactivateUser(final LdapPerson user)
  {
    new LdapTemplate(ldapConnector) {
      @Override
      protected Object call() throws NameNotFoundException, Exception
      {
        deactivateUser(ctx, user);
        return null;
      }
    }.excecute();
  }

  public void deactivateUser(final DirContext ctx, final LdapPerson user) throws NamingException
  {
    log.info("Deactivate user: " + buildDn(null, user));
    final ModificationItem[] modificationItems;
    short i = 0;
    modificationItems = new ModificationItem[2];
    modificationItems[i++] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", null));
    modificationItems[i++] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("mail", DEACTIVATED_MAIL));
    buildDn(null, user);
    modify(ctx, user, modificationItems);
    final String ou = user.getOrganizationalUnit();
    if (ou.startsWith(DEACTIVATED_SUB_CONTEXT2) == false) {
      // Move user to the sub-context "deactivated".
      final String newOu = LdapUtils.getOu(DEACTIVATED_SUB_CONTEXT, ldapConfig.getUserBase());
      move(ctx, user, newOu);
      user.setOrganizationalUnit(newOu);
    }
  }

  /**
   * Moves the user only from the "deactivated" sub-context to the parent context. If the user isn't in the context name "deactivated"
   * nothing will be done.
   * @param user
   */
  public void reactivateUser(final LdapPerson user)
  {
    new LdapTemplate(ldapConnector) {
      @Override
      protected Object call() throws NameNotFoundException, Exception
      {
        reactivateUser(ctx, user);
        return null;
      }
    }.excecute();
  }

  public void reactivateUser(final DirContext ctx, final LdapPerson user) throws NamingException
  {
    log.info("Reactivate deactivated user: " + buildDn(null, user));
    final String ou = LdapUtils.getOu(user.getOrganizationalUnit());
    if (ou.startsWith(DEACTIVATED_SUB_CONTEXT2) == false) {
      log.info("Object isn't in a deactivated sub-context, nothing will be done: " + buildDn(null, user));
      return;
    }
    String newPath;
    if (ou.startsWith(DEACTIVATED_SUB_CONTEXT3) == true) {
      newPath = ou.substring(DEACTIVATED_SUB_CONTEXT3.length());
    } else {
      newPath = ou.substring(DEACTIVATED_SUB_CONTEXT2.length());
    }
    move(ctx, user, newPath);
    user.setOrganizationalUnit(newPath);
  }

  void updateActivatedStatus(final DirContext ctx, final LdapPerson user) throws NamingException
  {
    final String ou = LdapUtils.getOu(user.getOrganizationalUnit());
    if (user.isDeactivated() == true) {
      if (ou.startsWith(DEACTIVATED_SUB_CONTEXT2) == true) {
        // User is already stored in deactivated context. Nothing to be done.
        return;
      } else {
        deactivateUser(ctx, user);
      }
    } else {
      if (ou.startsWith(DEACTIVATED_SUB_CONTEXT2) == false) {
        // User isn't stored in deactivated context. Nothing to be done.
        return;
      } else {
        reactivateUser(ctx, user);
      }
    }
  }

  void updateRestrictedUserStatus(final DirContext ctx, final LdapPerson user) throws NamingException
  {
    final String ou = LdapUtils.getOu(user.getOrganizationalUnit());
    if (user.isDeactivated() == true) {
      // User is deactivated, thus the restricted-user-status is ignored.
      return;
    }
    if (user.isRestrictedUser() == true) {
      if (ou.startsWith(RESTRICTED_USER_SUB_CONTEXT2) == true) {
        // User is already stored in restricted context. Nothing to be done.
        return;
      } else {
        setUserAsRestrictedUser(ctx, user);
      }
    } else {
      if (ou.startsWith(RESTRICTED_USER_SUB_CONTEXT2) == false) {
        // User isn't stored in restricted context. Nothing to be done.
        return;
      } else {
        log.info("Move user from restricted sub context: " + buildDn(null, user));
        String newPath;
        if (ou.startsWith(RESTRICTED_USER_SUB_CONTEXT3) == true) {
          newPath = ou.substring(RESTRICTED_USER_SUB_CONTEXT3.length());
        } else {
          newPath = ou.substring(RESTRICTED_USER_SUB_CONTEXT2.length());
        }
        move(ctx, user, newPath);
        user.setOrganizationalUnit(newPath);
      }
    }
  }

  private void setUserAsRestrictedUser(final DirContext ctx, final LdapPerson user) throws NamingException
  {
    log.info("Move user to restricted sub context: " + buildDn(null, user));
    if (user.isDeactivated() == true) {
      log.info("User is deactivated, thus the restricted-user-status is ignored: " + buildDn(null, user));
      return;
    }
    final String ou = user.getOrganizationalUnit();
    if (ou.startsWith(RESTRICTED_USER_SUB_CONTEXT2) == false) {
      // Move user to the sub-context "restricted".
      final String newOu = LdapUtils.getOu(RESTRICTED_USER_SUB_CONTEXT, user.getOrganizationalUnit());
      move(ctx, user, newOu);
      user.setOrganizationalUnit(newOu);
    }
  }

  /**
   * Calls super method and {@link #deactivateUser(DirContext, LdapPerson)} if the given person is deactivated. If the given person is
   * deleted, nothing will be done.
   * @see org.projectforge.ldap.LdapDao#create(javax.naming.directory.DirContext, org.projectforge.ldap.LdapObject, java.lang.Object[])
   */
  @Override
  public void create(final DirContext ctx, final String ouBase, final LdapPerson user, final Object... args) throws NamingException
  {
    if (user.isDeleted() == true) {
      log.info("Given LDAP user is deleted, so the user will not be created in the LDAP system (nothing will be done).");
      return;
    }
    super.create(ctx, ouBase, user, args);
    if (user.isDeactivated() == true) {
      deactivateUser(ctx, user);
    } else if (user.isRestrictedUser() == true) {
      // Deactivated users shouldn't be moved to restricted ou sub context.
      setUserAsRestrictedUser(ctx, user);
    }
  }

  /**
   * @see org.projectforge.ldap.LdapDao#update(javax.naming.directory.DirContext, org.projectforge.ldap.LdapObject, java.lang.Object[])
   */
  @Override
  public void update(final DirContext ctx, final String ouBase, final LdapPerson user, final Object... objs) throws NamingException
  {
    if (user.isDeleted() == true) {
      log.info("Given LDAP user is deleted, so the user will be removed from the LDAP system.");
      delete(ctx, user);
      return;
    }
    super.update(ctx, ouBase, user, objs);
    updateActivatedStatus(ctx, user);
    updateRestrictedUserStatus(ctx, user);
  }

  public void changePassword(final LdapPerson user, final String oldPassword, final String newPassword)
  {
    log.info("Change password for " + getObjectClass() + ": " + buildDn(null, user));
    final ModificationItem[] modificationItems;
    // Replace the "unicdodePwd" attribute with a new value
    // Password must be both Unicode and a quoted string
    // try {
    // final String oldQuotedPassword = "\"" + oldPassword + "\"";
    // final byte[] oldUnicodePassword = oldQuotedPassword.getBytes("UTF-16LE");
    // final String newQuotedPassword = "\"" + newPassword + "\"";
    // final byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");
    if (oldPassword != null) {
      modificationItems = new ModificationItem[2];
      modificationItems[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("userPassword", oldPassword));
      modificationItems[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("userPassword", newPassword));
    } else {
      modificationItems = new ModificationItem[1];
      modificationItems[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("userPassword", newPassword));
    }
    // } catch (final UnsupportedEncodingException ex) {
    // log.error("While encoding passwords with UTF-16LE: " + ex.getMessage(), ex);
    // throw new RuntimeException(ex);
    // }
    // Perform the update
    modify(user, modificationItems);
  }

  public LdapPerson findByUsername(final Object username, final String... organizationalUnits)
  {
    return (LdapPerson) new LdapTemplate(ldapConnector) {
      @Override
      protected Object call() throws NameNotFoundException, Exception
      {
        NamingEnumeration< ? > results = null;
        final SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        final String searchBase = LdapUtils.getOu(organizationalUnits);
        results = ctx.search(searchBase, "(&(objectClass=" + getObjectClass() + ")(uid=" + username + "))", controls);
        if (results.hasMore() == false) {
          return null;
        }
        final SearchResult searchResult = (SearchResult) results.next();
        final String dn = searchResult.getName();
        final Attributes attributes = searchResult.getAttributes();
        if (results.hasMore() == true) {
          log.error("Oups, found entries with multiple id's: " + getObjectClass() + "." + username);
        }
        return mapToObject(dn, searchBase, attributes);
      }
    }.excecute();
  }

  public LdapPerson authenticate(final String username, final String userPassword, final String... organizationalUnits)
  {
    String dn;
    LdapPerson user = null;
    if (StringUtils.isNotBlank(ldapConfig.getManagerUser()) == true && StringUtils.isNotBlank(ldapConfig.getManagerPassword()) == true) {
      user = findByUsername(username, organizationalUnits);
      if (user == null || StringUtils.equals(username, user.getId()) == false) {
        log.info("User with id '" + username + "' not found.");
        return null;
      }
      dn = user.getDn() + "," + ldapConnector.getBase();
    } else {
      dn = "uid=" + username + "," + LdapUtils.getOu(LdapUtils.getOu(organizationalUnits)) + "," + ldapConnector.getBase();
    }
    try {
      ldapConnector.createContext(dn, userPassword);
      log.info("User '" + username + "' (" + dn + ") successfully authenticated.");
      user = new LdapPerson().setUid(username);
      return user;
    } catch (final Exception ex) {
      log.error("User '" + username + "' (" + dn + ") with invalid credentials.");
      return null;
    }
  }

  /**
   * @see org.projectforge.ldap.LdapDao#createAndAddModificationItems(java.util.List, java.lang.String, java.lang.String[])
   */
  @Override
  protected void createAndAddModificationItems(final List<ModificationItem> list, final String attrId, final String... attrValues)
  {
    if ("uid".equals(attrId) == true) {
      // Don't change uid because it's part of the dn.
      return;
    }
    super.createAndAddModificationItems(list, attrId, attrValues);
  }


  /**
   * @see org.projectforge.ldap.LdapPersonDao#addModificationItems(java.util.List, org.projectforge.ldap.LdapPerson)
   */
  @Override
  protected void addModificationItems(final List<ModificationItem> list, final LdapPerson person)
  {
    createAndAddModificationItems(list, "cn", person.getCommonName());
  }

  /**
   * @see org.projectforge.ldap.LdapDao#buildDnIdentifier(org.projectforge.ldap.LdapObject)
   */
  @Override
  protected String buildDnIdentifier(final LdapPerson obj)
  {
    if (useUidInDn == true) {
      return "uid=" + obj.getUid();
    } else {
      return "cn=" + LdapUtils.escapeCommonName(obj.getCommonName());
    }
  }
}
