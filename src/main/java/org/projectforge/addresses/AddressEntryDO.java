/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.addresses;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.core.DefaultBaseDO;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 *
 */
@Entity
@Indexed
@Table(name = "T_ADDRESSENTRY")
public class AddressEntryDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -1724220844452834692L;

  //private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressEntryDO.class);

  private Address2DO address;

  @Enumerated(EnumType.STRING)
  @Field(index = Index.TOKENIZED, store = Store.NO)
  private AddressType addressType;

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String city; // 255

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String country; // 255

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String state; // 255

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String street; // 255

  @Field(index = Index.TOKENIZED, store = Store.NO)
  private String zipCode; // 255

  /**
   * Not used as object due to performance reasons.
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "address_id", nullable = false)
  public Address2DO getAddress()
  {
    return address;
  }

  public void setAddress(final Address2DO address)
  {
    this.address = address;
  }

  @Transient
  public Integer getAddressId()
  {
    if (this.address == null)
      return null;
    return address.getId();
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 15, name = "address_type")
  public AddressType getAddressType()
  {
    return addressType;
  }

  /**
   * @return this for chaining.
   */
  public AddressEntryDO setAddressType(final AddressType addressType)
  {
    this.addressType = addressType;
    return this;
  }

  /**
   * @return the city
   */
  public String getCity()
  {
    return city;
  }

  /**
   * @param city the city to set
   * @return this for chaining.
   */
  public AddressEntryDO setCity(final String city)
  {
    this.city = city;
    return this;
  }

  /**
   * @return the country
   */
  public String getCountry()
  {
    return country;
  }

  /**
   * @param country the country to set
   * @return this for chaining.
   */
  public AddressEntryDO setCountry(final String country)
  {
    this.country = country;
    return this;
  }

  /**
   * @return the state
   */
  public String getState()
  {
    return state;
  }

  /**
   * @param state the state to set
   * @return this for chaining.
   */
  public AddressEntryDO setState(final String state)
  {
    this.state = state;
    return this;
  }

  /**
   * @return the street
   */
  public String getStreet()
  {
    return street;
  }

  /**
   * @param street the street to set
   * @return this for chaining.
   */
  public AddressEntryDO setStreet(final String street)
  {
    this.street = street;
    return this;
  }

  /**
   * @return the zipCode
   */
  public String getZipCode()
  {
    return zipCode;
  }

  /**
   * @param zipCode the zipCode to set
   * @return this for chaining.
   */
  public AddressEntryDO setZipCode(final String zipCode)
  {
    this.zipCode = zipCode;
    return this;
  }


}
