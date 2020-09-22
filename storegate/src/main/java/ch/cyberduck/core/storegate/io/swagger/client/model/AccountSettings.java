/*
 * Storegate.Web
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: v4
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package ch.cyberduck.core.storegate.io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A accounts settings. Properties that are null/undefined/missing are not available
 */
@ApiModel(description = "A accounts settings. Properties that are null/undefined/missing are not available")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-09-18T14:15:21.736+02:00")



public class AccountSettings {
  @JsonProperty("startPagesAvailable")
  private List<String> startPagesAvailable = null;

  @JsonProperty("localesAvailable")
  private Map<String, String> localesAvailable = null;

  @JsonProperty("versionsAvailable")
  private List<Integer> versionsAvailable = null;

  @JsonProperty("twoFactor")
  private Boolean twoFactor = null;

  @JsonProperty("startPage")
  private String startPage = null;

  @JsonProperty("showHiddenFiles")
  private Boolean showHiddenFiles = null;

  @JsonProperty("locale")
  private String locale = null;

  @JsonProperty("officeOnline")
  private Boolean officeOnline = null;

  @JsonProperty("recycleBin")
  private Boolean recycleBin = null;

  @JsonProperty("versions")
  private Integer versions = null;

  @JsonProperty("loginNotification")
  private Boolean loginNotification = null;

  public AccountSettings startPagesAvailable(List<String> startPagesAvailable) {
    this.startPagesAvailable = startPagesAvailable;
    return this;
  }

  public AccountSettings addStartPagesAvailableItem(String startPagesAvailableItem) {
    if (this.startPagesAvailable == null) {
      this.startPagesAvailable = new ArrayList<>();
    }
    this.startPagesAvailable.add(startPagesAvailableItem);
    return this;
  }

   /**
   * Webpage startpages available
   * @return startPagesAvailable
  **/
  @ApiModelProperty(value = "Webpage startpages available")
  public List<String> getStartPagesAvailable() {
    return startPagesAvailable;
  }

  public void setStartPagesAvailable(List<String> startPagesAvailable) {
    this.startPagesAvailable = startPagesAvailable;
  }

  public AccountSettings localesAvailable(Map<String, String> localesAvailable) {
    this.localesAvailable = localesAvailable;
    return this;
  }

  public AccountSettings putLocalesAvailableItem(String key, String localesAvailableItem) {
    if (this.localesAvailable == null) {
      this.localesAvailable = new HashMap<>();
    }
    this.localesAvailable.put(key, localesAvailableItem);
    return this;
  }

   /**
   * Available locales
   * @return localesAvailable
  **/
  @ApiModelProperty(value = "Available locales")
  public Map<String, String> getLocalesAvailable() {
    return localesAvailable;
  }

  public void setLocalesAvailable(Map<String, String> localesAvailable) {
    this.localesAvailable = localesAvailable;
  }

  public AccountSettings versionsAvailable(List<Integer> versionsAvailable) {
    this.versionsAvailable = versionsAvailable;
    return this;
  }

  public AccountSettings addVersionsAvailableItem(Integer versionsAvailableItem) {
    if (this.versionsAvailable == null) {
      this.versionsAvailable = new ArrayList<>();
    }
    this.versionsAvailable.add(versionsAvailableItem);
    return this;
  }

   /**
   * Lists the available settings for versioning.
   * @return versionsAvailable
  **/
  @ApiModelProperty(value = "Lists the available settings for versioning.")
  public List<Integer> getVersionsAvailable() {
    return versionsAvailable;
  }

  public void setVersionsAvailable(List<Integer> versionsAvailable) {
    this.versionsAvailable = versionsAvailable;
  }

  public AccountSettings twoFactor(Boolean twoFactor) {
    this.twoFactor = twoFactor;
    return this;
  }

   /**
   * Show hidden files in the file list.
   * @return twoFactor
  **/
  @ApiModelProperty(value = "Show hidden files in the file list.")
  public Boolean isTwoFactor() {
    return twoFactor;
  }

  public void setTwoFactor(Boolean twoFactor) {
    this.twoFactor = twoFactor;
  }

  public AccountSettings startPage(String startPage) {
    this.startPage = startPage;
    return this;
  }

   /**
   * Webpage startpage
   * @return startPage
  **/
  @ApiModelProperty(value = "Webpage startpage")
  public String getStartPage() {
    return startPage;
  }

  public void setStartPage(String startPage) {
    this.startPage = startPage;
  }

  public AccountSettings showHiddenFiles(Boolean showHiddenFiles) {
    this.showHiddenFiles = showHiddenFiles;
    return this;
  }

   /**
   * Show hidden files in the file list.
   * @return showHiddenFiles
  **/
  @ApiModelProperty(value = "Show hidden files in the file list.")
  public Boolean isShowHiddenFiles() {
    return showHiddenFiles;
  }

  public void setShowHiddenFiles(Boolean showHiddenFiles) {
    this.showHiddenFiles = showHiddenFiles;
  }

  public AccountSettings locale(String locale) {
    this.locale = locale;
    return this;
  }

   /**
   * Locale of the webpage
   * @return locale
  **/
  @ApiModelProperty(value = "Locale of the webpage")
  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public AccountSettings officeOnline(Boolean officeOnline) {
    this.officeOnline = officeOnline;
    return this;
  }

   /**
   * Enable Office Online
   * @return officeOnline
  **/
  @ApiModelProperty(value = "Enable Office Online")
  public Boolean isOfficeOnline() {
    return officeOnline;
  }

  public void setOfficeOnline(Boolean officeOnline) {
    this.officeOnline = officeOnline;
  }

  public AccountSettings recycleBin(Boolean recycleBin) {
    this.recycleBin = recycleBin;
    return this;
  }

   /**
   * Indicates if the recycle bin is enabled.
   * @return recycleBin
  **/
  @ApiModelProperty(value = "Indicates if the recycle bin is enabled.")
  public Boolean isRecycleBin() {
    return recycleBin;
  }

  public void setRecycleBin(Boolean recycleBin) {
    this.recycleBin = recycleBin;
  }

  public AccountSettings versions(Integer versions) {
    this.versions = versions;
    return this;
  }

   /**
   * Number of versions keept in versioning.
   * @return versions
  **/
  @ApiModelProperty(value = "Number of versions keept in versioning.")
  public Integer getVersions() {
    return versions;
  }

  public void setVersions(Integer versions) {
    this.versions = versions;
  }

  public AccountSettings loginNotification(Boolean loginNotification) {
    this.loginNotification = loginNotification;
    return this;
  }

   /**
   * 
   * @return loginNotification
  **/
  @ApiModelProperty(value = "")
  public Boolean isLoginNotification() {
    return loginNotification;
  }

  public void setLoginNotification(Boolean loginNotification) {
    this.loginNotification = loginNotification;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountSettings accountSettings = (AccountSettings) o;
    return Objects.equals(this.startPagesAvailable, accountSettings.startPagesAvailable) &&
        Objects.equals(this.localesAvailable, accountSettings.localesAvailable) &&
        Objects.equals(this.versionsAvailable, accountSettings.versionsAvailable) &&
        Objects.equals(this.twoFactor, accountSettings.twoFactor) &&
        Objects.equals(this.startPage, accountSettings.startPage) &&
        Objects.equals(this.showHiddenFiles, accountSettings.showHiddenFiles) &&
        Objects.equals(this.locale, accountSettings.locale) &&
        Objects.equals(this.officeOnline, accountSettings.officeOnline) &&
        Objects.equals(this.recycleBin, accountSettings.recycleBin) &&
        Objects.equals(this.versions, accountSettings.versions) &&
        Objects.equals(this.loginNotification, accountSettings.loginNotification);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startPagesAvailable, localesAvailable, versionsAvailable, twoFactor, startPage, showHiddenFiles, locale, officeOnline, recycleBin, versions, loginNotification);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccountSettings {\n");
    
    sb.append("    startPagesAvailable: ").append(toIndentedString(startPagesAvailable)).append("\n");
    sb.append("    localesAvailable: ").append(toIndentedString(localesAvailable)).append("\n");
    sb.append("    versionsAvailable: ").append(toIndentedString(versionsAvailable)).append("\n");
    sb.append("    twoFactor: ").append(toIndentedString(twoFactor)).append("\n");
    sb.append("    startPage: ").append(toIndentedString(startPage)).append("\n");
    sb.append("    showHiddenFiles: ").append(toIndentedString(showHiddenFiles)).append("\n");
    sb.append("    locale: ").append(toIndentedString(locale)).append("\n");
    sb.append("    officeOnline: ").append(toIndentedString(officeOnline)).append("\n");
    sb.append("    recycleBin: ").append(toIndentedString(recycleBin)).append("\n");
    sb.append("    versions: ").append(toIndentedString(versions)).append("\n");
    sb.append("    loginNotification: ").append(toIndentedString(loginNotification)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

