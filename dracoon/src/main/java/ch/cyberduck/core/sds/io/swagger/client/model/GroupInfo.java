/*
 * DRACOON API
 * REST Web Services for DRACOON<br>built at: 2020-09-09 08:12:59<br><br>This page provides an overview of all available and documented DRACOON APIs, which are grouped by tags.<br>Each tag provides a collection of APIs that are intended for a specific area of the DRACOON.<br><br><a title='Developer Information' href='https://developer.dracoon.com'>Developer Information</a>&emsp;&emsp;<a title='Get SDKs on GitHub' href='https://github.com/dracoon'>Get SDKs on GitHub</a><br><br><a title='Terms of service' href='https://www.dracoon.com/terms/general-terms-and-conditions/'>Terms of service</a>
 *
 * OpenAPI spec version: 4.23.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package ch.cyberduck.core.sds.io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Group information
 */
@Schema(description = "Group information")
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2020-09-15T09:21:49.036118+02:00[Europe/Zurich]")
public class GroupInfo {
  @JsonProperty("id")
  private Long id = null;

  @JsonProperty("name")
  private String name = null;

  public GroupInfo id(Long id) {
    this.id = id;
    return this;
  }

   /**
   * Unique identifier for the group
   * @return id
  **/
  @Schema(required = true, description = "Unique identifier for the group")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public GroupInfo name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Group name
   * @return name
  **/
  @Schema(required = true, description = "Group name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupInfo groupInfo = (GroupInfo) o;
    return Objects.equals(this.id, groupInfo.id) &&
        Objects.equals(this.name, groupInfo.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GroupInfo {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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
